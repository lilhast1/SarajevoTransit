import { useEffect, useState, useCallback } from 'react'
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
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const query = new URLSearchParams({ page, size: 20, sort: 'createdAt,desc' })
      if (statusFilter !== 'ALL') query.set('status', statusFilter)
      const res = await gatewayClient.getReports(`?${query}`)
      setData(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [statusFilter, page])

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
    if (!window.confirm('Delete this report?')) return
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
    { key: 'description', label: 'Description', render: (r) => trunc(r.description, 80) },
    { key: 'reporterUserId', label: 'Reporter ID' },
    { key: 'lineId', label: 'Line' },
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
