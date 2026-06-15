import { useEffect, useState, useCallback } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { DataTable } from '../../components/admin/DataTable'
import { AdminPagePanel } from '../../components/common/AdminPagePanel'
import { StatusBadge } from '../../components/common/StatusBadge'
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
  const [lineFilterError, setLineFilterError] = useState(null)

  useEffect(() => {
    let active = true
    const q = vehicleTypeId ? `?vehicleTypeId=${vehicleTypeId}` : ''
    setLineId('')

    const loadLines = async () => {
      try {
        setLineFilterError(null)
        const response = await gatewayClient.getLines(q)
        if (active) setLines(response)
      } catch (err) {
        if (active) {
          setLines([])
          setLineFilterError(err.message || t('lines_load_failed'))
        }
      }
    }

    loadLines()

    return () => {
      active = false
    }
  }, [vehicleTypeId, t])

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

      const userIds = [...new Set(rows.map((r) => r.reviewerUserId).filter(Boolean))]
      const userEntries = await Promise.all(
        userIds.map((id) =>
          gatewayClient.getUserById(id)
            .then((u) => [id, u.fullName ?? u.email ?? `#${id}`])
            .catch(() => [id, `#${id}`])
        )
      )
      setReviewerNames((prev) => ({ ...prev, ...Object.fromEntries(userEntries) }))

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
      key: 'moderationStatus', label: t('col_status'), render: (r) => <StatusBadge status={r.moderationStatus} />
    },
    {
      key: 'actions', label: t('col_actions'), render: (r) => (
        <div className="flex items-center gap-2">
          <button
            type="button"
            title={r.moderationStatus === 'VISIBLE' ? t('hide') : t('show')}
            onClick={() => handleToggleVisibility(r)}
            className="rounded-panel border border-border p-1.5 text-muted transition hover:bg-surface-alt"
          >
            {r.moderationStatus === 'VISIBLE' ? <EyeOff size={14} /> : <Eye size={14} />}
          </button>
          <button
            type="button"
            onClick={() => handleDelete(r.id)}
            className="rounded-panel border border-danger-soft px-2.5 py-1 text-xs font-medium text-danger transition hover:bg-danger-soft/20"
          >
            {t('delete')}
          </button>
        </div>
      )
    },
  ]

  const typePillOptions = [{ id: '', label: t('all') }, ...VEHICLE_TYPES]

  return (
    <AdminPagePanel>
      <AdminPagePanel.Header
        title={t('title')}
        subtitle={t('subtitle')}
      />

      <AdminPagePanel.Toolbar>
        <AdminPagePanel.ToolbarGroup label={t('type_label')}>
          <div className="flex flex-wrap gap-1">
            {typePillOptions.map((vt) => (
              <button
                key={vt.id}
                type="button"
                onClick={() => setVehicleTypeId(vt.id)}
                className={`rounded-panel border px-2.5 py-1 text-xs font-medium transition ${
                  vehicleTypeId === vt.id
                    ? 'border-accent bg-accent text-white shadow-sm'
                    : 'border-border text-muted hover:border-accent-subtle hover:text-ink'
                }`}
              >
                {vt.label}
              </button>
            ))}
          </div>
        </AdminPagePanel.ToolbarGroup>
      </AdminPagePanel.Toolbar>

      <AdminPagePanel.Toolbar>
        <AdminPagePanel.ToolbarGroup label={t('line_label')}>
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
        </AdminPagePanel.ToolbarGroup>

        <AdminPagePanel.ToolbarDivider />

        <AdminPagePanel.ToolbarGroup label={t('sort')}>
          {[{ value: 'desc', label: t('latest_first') }, { value: 'asc', label: t('earliest_first') }].map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => { setSortDir(opt.value); setPage(0) }}
              className={`rounded-panel border px-2.5 py-1 text-xs font-medium transition ${
                sortDir === opt.value
                  ? 'border-accent bg-accent text-white shadow-sm'
                  : 'border-border text-muted hover:border-accent-subtle hover:text-ink'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </AdminPagePanel.ToolbarGroup>
      </AdminPagePanel.Toolbar>

      <AdminPagePanel.Toolbar>
        <AdminPagePanel.ToolbarGroup label={t('date_range')}>
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
        </AdminPagePanel.ToolbarGroup>
      </AdminPagePanel.Toolbar>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />
      <ErrorAlert error={lineFilterError} onDismiss={() => setLineFilterError(null)} />

      <DataTable
        columns={columns}
        rows={visibleRows}
        page={page}
        totalPages={data.totalPages ?? 0}
        onPageChange={setPage}
        loading={loading}
      />
    </AdminPagePanel>
  )
}

export default AdminReviewsPage
