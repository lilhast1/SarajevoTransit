import { useEffect, useState, useCallback } from 'react'
import { Image } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { DataTable } from '../../components/admin/DataTable'
import { ErrorAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'

const STATUSES = ['ALL', 'RECEIVED', 'IN_PROGRESS', 'RESOLVED']

const STATUS_BADGE = {
  RECEIVED: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  IN_PROGRESS: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  RESOLVED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
}

function trunc(str, n) {
  if (!str) return '—'
  return str.length > n ? `${str.slice(0, n)}…` : str
}


export function AdminReportsPage() {
  const { t } = useTranslation('admin-reports')
  const navigate = useNavigate()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sortDir, setSortDir] = useState('desc')
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [userNames, setUserNames] = useState({})
  const [lineNames, setLineNames] = useState({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const query = new URLSearchParams({ page, size: 20, sort: `createdAt,${sortDir}` })
      if (statusFilter !== 'ALL') query.set('status', statusFilter)
      const res = await gatewayClient.getReports(`?${query}`)
      setData(res)
      const rows = res.content ?? []

      const ids = [...new Set(rows.map((r) => r.reporterUserId).filter(Boolean))]
      const entries = await Promise.all(
        ids.map((id) => gatewayClient.getUserById(id).then((u) => [id, u.fullName ?? `#${id}`]).catch(() => [id, `#${id}`]))
      )
      setUserNames(Object.fromEntries(entries))

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
  }, [statusFilter, sortDir, page])

  useEffect(() => { load() }, [load])

  async function handleStatusChange(id, status) {
    try {
      await gatewayClient.updateReportStatus(id, status)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleDelete(id) {
    if (!window.confirm(t('delete_confirm'))) return
    try {
      await gatewayClient.deleteReport(id)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const columns = [
    { key: 'createdAt', label: 'Date', render: (r) => r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—' },
    { key: 'category', label: 'Category' },
    { key: 'description', label: 'Description', render: (r) => trunc(r.description, 60) },
    { key: 'reporterUserId', label: 'Reporter', render: (r) => userNames[r.reporterUserId] ?? (r.reporterUserId ? `#${r.reporterUserId}` : '—') },
    { key: 'lineId', label: 'Line', render: (r) => r.lineId ? (lineNames[r.lineId] ?? `#${r.lineId}`) : '—' },
    {
      key: 'photos', label: 'Photos', render: (r) => (
        r.photoUrls?.length > 0 ? (
          <span className="flex items-center gap-1 text-xs text-muted">
            <Image size={12} />
            {r.photoUrls.length}
          </span>
        ) : '—'
      )
    },
    {
      key: 'status', label: 'Status', render: (r) => (
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[r.status] ?? ''}`}>
          {r.status}
        </span>
      )
    },
    {
      key: 'actions', label: 'Actions', render: (r) => (
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => navigate(`/admin/reports/${r.id}`)}
            className="rounded border border-border px-2 py-0.5 text-xs text-muted hover:bg-surface-alt hover:text-ink"
          >
            Details
          </button>
          <select
            value={r.status}
            onChange={(e) => handleStatusChange(r.id, e.target.value)}
            className="rounded border border-border bg-surface px-1 py-0.5 text-xs text-ink"
          >
            {['RECEIVED', 'IN_PROGRESS', 'RESOLVED'].map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
          <button
            type="button"
            onClick={() => handleDelete(r.id)}
            className="rounded border border-red-300 px-2 py-0.5 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-950"
          >
            Delete
          </button>
        </div>
      )
    },
  ]

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold text-ink">Problem Reports</h2>
        <p className="mt-1 text-sm text-muted">Review and manage user-submitted problem reports.</p>
      </div>

      <div className="flex flex-wrap items-center gap-4">
        <div className="flex gap-2">
          {STATUSES.map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => { setStatusFilter(s); setPage(0) }}
              className={`rounded-panel border px-3 py-1 text-xs font-medium transition ${
                statusFilter === s
                  ? 'border-accent bg-accent text-white'
                  : 'border-border text-muted hover:bg-surface-alt'
              }`}
            >
              {s}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-1">
          <span className="text-sm text-muted">Sort:</span>
          {[{ value: 'desc', label: 'Latest first' }, { value: 'asc', label: 'Earliest first' }].map((opt) => (
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

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      <DataTable
        columns={columns}
        rows={data.content ?? []}
        page={page}
        totalPages={data.totalPages ?? 0}
        onPageChange={setPage}
        loading={loading}
      />

    </div>
  )
}

export default AdminReportsPage
