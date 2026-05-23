import { useEffect, useState, useCallback } from 'react'
import { Eye, EyeOff } from 'lucide-react'
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
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  const [lines, setLines] = useState([])
  const [lineId, setLineId] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    const q = vehicleTypeId ? `?vehicleTypeId=${vehicleTypeId}` : ''
    setLineId('')
    gatewayClient.getLines(q).then(setLines).catch(() => {})
  }, [vehicleTypeId])

  const load = useCallback(async () => {
    if (!lineId) return
    setLoading(true)
    setError(null)
    try {
      const query = new URLSearchParams({ lineId, includeHidden: true, page, size: 20 })
      const res = await gatewayClient.getReviews(`?${query}`)
      setData(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [lineId, page])

  useEffect(() => { load() }, [load])

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
    if (!window.confirm('Delete this review?')) return
    try {
      await gatewayClient.deleteReview(id)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const columns = [
    { key: 'createdAt', label: 'Date', render: (r) => r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—' },
    { key: 'reviewerUserId', label: 'Reviewer ID' },
    { key: 'rating', label: 'Rating', render: (r) => '⭐'.repeat(r.rating ?? 0) },
    { key: 'comment', label: 'Comment', render: (r) => trunc(r.comment, 80) },
    {
      key: 'moderationStatus', label: 'Status', render: (r) => (
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
      key: 'actions', label: 'Actions', render: (r) => (
        <div className="flex items-center gap-2">
          <button
            type="button"
            title={r.moderationStatus === 'VISIBLE' ? 'Hide' : 'Show'}
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
            Delete
          </button>
        </div>
      )
    },
  ]

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold text-ink">Review Moderation</h2>
        <p className="mt-1 text-sm text-muted">Show or hide user reviews per line.</p>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm text-muted">Type:</span>
        {[{ id: '', label: 'All' }, ...VEHICLE_TYPES].map((vt) => (
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

      <div className="flex items-center gap-3">
        <label className="text-sm text-muted">Line:</label>
        <select
          value={lineId}
          onChange={(e) => { setLineId(e.target.value); setPage(0) }}
          className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
        >
          <option value="">— Select a line —</option>
          {lines.map((l) => (
            <option key={l.id} value={l.id}>{l.code} – {l.name}</option>
          ))}
        </select>
      </div>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      {lineId ? (
        <DataTable
          columns={columns}
          rows={data.content ?? []}
          page={page}
          totalPages={data.totalPages ?? 0}
          onPageChange={setPage}
          loading={loading}
        />
      ) : (
        <p className="text-sm text-muted">Select a line to load its reviews.</p>
      )}
    </div>
  )
}

export default AdminReviewsPage
