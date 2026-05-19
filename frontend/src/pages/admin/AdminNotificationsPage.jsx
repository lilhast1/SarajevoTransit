import { useEffect, useState, useCallback } from 'react'
import { DataTable } from '../../components/admin/DataTable'
import { PanelCard } from '../../components/common/PanelCard'
import { ErrorAlert, SuccessAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'

const NOTIFICATION_TYPES = ['GENERAL', 'DELAY', 'DISRUPTION', 'ROUTE_CHANGE', 'TIMETABLE_CHANGE', 'UPCOMING_DEPARTURE']

const TYPE_BADGE = {
  GENERAL: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  DELAY: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  DISRUPTION: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  ROUTE_CHANGE: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
  TIMETABLE_CHANGE: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  UPCOMING_DEPARTURE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
}

const EMPTY_FORM = { lineId: '', title: '', content: '', type: 'GENERAL' }

function trunc(str, n) {
  if (!str) return '—'
  return str.length > n ? `${str.slice(0, n)}…` : str
}

export function AdminNotificationsPage() {
  const [lines, setLines] = useState([])
  const [form, setForm] = useState(EMPTY_FORM)
  const [sending, setSending] = useState(false)
  const [success, setSuccess] = useState(null)
  const [error, setError] = useState(null)

  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    gatewayClient.getLines('').then(setLines).catch(() => {})
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await gatewayClient.getAllNotifications(`?page=${page}&size=20&sort=sentAt,desc`)
      setData(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => { load() }, [load])

  async function handleBroadcast(e) {
    e.preventDefault()
    setSending(true)
    setError(null)
    setSuccess(null)
    try {
      await gatewayClient.broadcastNotification({
        lineId: Number(form.lineId),
        title: form.title,
        content: form.content,
        type: form.type,
      })
      setSuccess(`Broadcast sent: "${form.title}"`)
      setForm(EMPTY_FORM)
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSending(false)
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Delete this notification?')) return
    try {
      await gatewayClient.deleteNotification(id)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const columns = [
    { key: 'sentAt', label: 'Date', render: (r) => r.sentAt ? new Date(r.sentAt).toLocaleString() : '—' },
    { key: 'title', label: 'Title' },
    { key: 'content', label: 'Message', render: (r) => trunc(r.content, 80) },
    {
      key: 'type', label: 'Type', render: (r) => (
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${TYPE_BADGE[r.type] ?? 'bg-gray-100 text-gray-600'}`}>
          {r.type}
        </span>
      )
    },
    { key: 'userId', label: 'User ID', render: (r) => r.userId ?? 'Broadcast' },
    {
      key: 'actions', label: 'Actions', render: (r) => (
        <button
          type="button"
          onClick={() => handleDelete(r.id)}
          className="rounded border border-red-300 px-2 py-0.5 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-950"
        >
          Delete
        </button>
      )
    },
  ]

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-semibold text-ink">Notifications</h2>
        <p className="mt-1 text-sm text-muted">Send broadcast notifications to all users and view history.</p>
      </div>

      <PanelCard tone="soft">
        <h3 className="mb-3 text-sm font-semibold text-ink">Broadcast Notification</h3>
        <form onSubmit={handleBroadcast} className="grid gap-3 sm:grid-cols-2">
          <label className="block">
            <span className="text-xs text-muted">Line</span>
            <select
              required
              value={form.lineId}
              onChange={(e) => setForm((f) => ({ ...f, lineId: e.target.value }))}
              className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
            >
              <option value="">— Select line —</option>
              {lines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
            </select>
          </label>
          <label className="block">
            <span className="text-xs text-muted">Type</span>
            <select
              value={form.type}
              onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
              className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
            >
              {NOTIFICATION_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </label>
          <label className="block sm:col-span-2">
            <span className="text-xs text-muted">Title</span>
            <input
              required
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
            />
          </label>
          <label className="block sm:col-span-2">
            <span className="text-xs text-muted">Message</span>
            <textarea
              required
              rows={3}
              value={form.content}
              onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
              className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
            />
          </label>
          <div className="sm:col-span-2">
            <button
              type="submit"
              disabled={sending}
              className="rounded-panel border border-accent bg-accent px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60"
            >
              {sending ? 'Sending…' : 'Send Broadcast'}
            </button>
          </div>
        </form>
        <div className="mt-3 space-y-2">
          <ErrorAlert error={error} onDismiss={() => setError(null)} />
          <SuccessAlert message={success} onDismiss={() => setSuccess(null)} />
        </div>
      </PanelCard>

      <div>
        <h3 className="mb-2 text-sm font-semibold text-ink">Notification History</h3>
        <DataTable
          columns={columns}
          rows={data.content ?? []}
          page={page}
          totalPages={data.totalPages ?? 0}
          onPageChange={setPage}
          loading={loading}
        />
      </div>
    </div>
  )
}

export default AdminNotificationsPage
