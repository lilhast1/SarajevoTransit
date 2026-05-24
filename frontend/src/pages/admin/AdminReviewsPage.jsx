import { useEffect, useState, useCallback } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { DataTable } from '../../components/admin/DataTable'
import { ErrorAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'
import { VEHICLE_TYPE_META_BY_ID } from '../../constants/vehicleColors'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)

function trunc(str, n) {
  if (!str) return '—'
  return str.length > n ? `${str.slice(0, n)}…` : str
}

export function AdminReviewsPage() {
  const { t } = useTranslation('admin-reviews')
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  const [lines, setLines] = useState([])
  const [lineId, setLineId] = useState('')
  const [page, setPage] = useState(0)
  const [sortDir, setSortDir] = useState('desc')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [reviewerNames, setReviewerNames] = useState({})
  const [lineNames, setLineNames] = useState({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    const q = vehicleTypeId ? `?vehicleTypeId=${vehicleTypeId}` : ''
    setLineId('')
    gatewayClient.getLines(q).then(setLines).catch(() => {})
  }, [vehicleTypeId])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = { includeHidden: true, page, size: 20, sort: `createdAt,${sortDir}` }
      if (lineId) params.lineId = lineId
      const query = new URLSearchParams(params)
      const res = await gatewayClient.getReviews(`?${query}`)
      setData(res)

      const rows = res.content ?? []

      // batch-fetch reviewer names
      const userIds = [...new Set(rows.map((r) => r.reviewerUserId).filter(Boolean))]
      const userEntries = await Promise.all(
        userIds.map((id) =>
          gatewayClient.getUserById(id)
            .then((u) => [id, u.fullName ?? u.email ?? `#${id}`])
            .catch(() => [id, `#${id}`])
        )
      )
      setReviewerNames((prev) => ({ ...prev, ...Object.fromEntries(userEntries) }))

      // batch-fetch line names
      const lids = [...new Set(rows.map((r) => r.lineId).filter(Boolean))]
      const lineEntries = await Promise.all(
        lids.map((id) =>
          gatewayClient.getLineById(id)
            .then((l) => [id, l.code ? `${l.code} – ${l.name}` : l.name])
            .catch(() => [id, `#${id}`])
        )
      )
      setLineNames((prev) => ({ ...prev, ...Object.fromEntries(lineEntries) }))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [lineId, page, sortDir])

  useEffect(() => { load() }, [load])

  const visibleRows = (() => {
    const rows = data.content ?? []
    if (!dateFrom && !dateTo) return rows
    const from = dateFrom ? new Date(dateFrom) : null
    const to = dateTo ? new Date(`${dateTo}T23:59:59`) : null
    return rows.filter((r) => {
      if (!r.createdAt) return true
      const d = new Date(r.createdAt)
      if (from && d < from) return false
      if (to && d > to) return false
      return true
    })
  })()

  async function handleToggleVisibility(review) {
    const next = review.moderationStatus === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE'
    try {
      await gatewayClient.updateReviewModeration(review.id, next)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleDelete(id) {
    if (!window.confirm(t('delete_confirm'))) return
    try {
      await gatewayClient.deleteReview(id)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const columns = [
    { key: 'createdAt', label: t('col_date'), render: (r) => r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—' },
    { key: 'reviewer', label: t('col_reviewer'), render: (r) => reviewerNames[r.reviewerUserId] ?? (r.reviewerUserId ? `#${r.reviewerUserId}` : '—') },
    { key: 'line', label: t('col_line'), render: (r) => lineNames[r.lineId] ?? (r.lineId ? `#${r.lineId}` : '—') },
    { key: 'rating', label: t('col_rating'), render: (r) => '⭐'.repeat(r.rating ?? 0) },
    { key: 'comment', label: t('col_comment'), render: (r) => trunc(r.reviewText, 80) },
    {
      key: 'moderationStatus', label: t('col_status'), render: (r) => (
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${
          r.moderationStatus === 'VISIBLE'
            ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
            : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
        }`}>
          {r.moderationStatus ?? 'VISIBLE'}
        </span>
      )
    },
    {
      key: 'actions', label: t('col_actions'), render: (r) => (
        <div className="flex items-center gap-2">
          <button
            type="button"
            title={r.moderationStatus === 'VISIBLE' ? t('hide') : t('show')}
            onClick={() => handleToggleVisibility(r)}
            className="rounded border border-border p-1 text-muted hover:bg-surface-alt"
          >
            {r.moderationStatus === 'VISIBLE' ? <EyeOff size={14} /> : <Eye size={14} />}
          </button>
          <button
            type="button"
            onClick={() => handleDelete(r.id)}
            className="rounded border border-red-300 px-2 py-0.5 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-950"
          >
            {t('delete')}
          </button>
        </div>
      )
    },
  ]

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold text-ink">{t('title')}</h2>
        <p className="mt-1 text-sm text-muted">{t('subtitle')}</p>
      </div>

      {/* vehicle type filter */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm text-muted">{t('type_label')}</span>
        {[{ id: '', label: t('all') }, ...VEHICLE_TYPES].map((vt) => (
          <button
            key={vt.id}
            type="button"
            onClick={() => setVehicleTypeId(vt.id)}
            className={`rounded-panel border px-3 py-1 text-xs font-medium transition ${
              vehicleTypeId === vt.id
                ? 'border-accent bg-accent text-white'
                : 'border-border text-muted hover:bg-surface-alt'
            }`}
          >
            {vt.label}
          </button>
        ))}
      </div>

      {/* line + sort row */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <label className="text-sm text-muted">{t('line_label')}</label>
          <select
            value={lineId}
            onChange={(e) => { setLineId(e.target.value); setPage(0) }}
            className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
          >
            <option value="">{t('all_lines')}</option>
            {lines.map((l) => (
              <option key={l.id} value={l.id}>{l.code} – {l.name}</option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-1">
          <span className="text-sm text-muted">{t('sort')}</span>
          {[{ value: 'desc', label: t('latest_first') }, { value: 'asc', label: t('earliest_first') }].map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => { setSortDir(opt.value); setPage(0) }}
              className={`rounded-panel border px-3 py-1 text-xs font-medium transition ${
                sortDir === opt.value
                  ? 'border-accent bg-accent text-white'
                  : 'border-border text-muted hover:bg-surface-alt'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* date range filter */}
      <div className="flex flex-wrap items-center gap-3">
        <span className="text-sm text-muted">{t('date_range')}</span>
        <label className="flex items-center gap-1.5 text-sm text-muted">
          {t('from')}
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
            className="rounded-panel border border-border bg-surface px-2 py-1 text-sm text-ink dark:[color-scheme:dark]"
          />
        </label>
        <label className="flex items-center gap-1.5 text-sm text-muted">
          {t('to')}
          <input
            type="date"
            value={dateTo}
            onChange={(e) => setDateTo(e.target.value)}
            className="rounded-panel border border-border bg-surface px-2 py-1 text-sm text-ink dark:[color-scheme:dark]"
          />
        </label>
        {(dateFrom || dateTo) && (
          <button
            type="button"
            onClick={() => { setDateFrom(''); setDateTo('') }}
            className="text-xs text-muted underline hover:text-ink"
          >
            {t('clear')}
          </button>
        )}
      </div>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      <DataTable
        columns={columns}
        rows={visibleRows}
        page={page}
        totalPages={data.totalPages ?? 0}
        onPageChange={setPage}
        loading={loading}
      />
    </div>
  )
}

export default AdminReviewsPage
