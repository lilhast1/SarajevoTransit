import { useEffect, useState, useCallback } from 'react'
import { DataTable } from '../../components/admin/DataTable'
import { PanelCard } from '../../components/common/PanelCard'
import { ErrorAlert, SuccessAlert } from '../../components/common/Alerts'
import { VEHICLE_TYPE_META_BY_ID } from '../../constants/vehicleColors'
import { gatewayClient } from '../../services/gatewayClient'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)
const NOTIFICATION_TYPES = ['GENERAL', 'DELAY', 'DISRUPTION', 'ROUTE_CHANGE', 'TIMETABLE_CHANGE', 'UPCOMING_DEPARTURE']

const TYPE_BADGE = {
  GENERAL: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  DELAY: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  DISRUPTION: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  ROUTE_CHANGE: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
  TIMETABLE_CHANGE: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  UPCOMING_DEPARTURE: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
}

const EMPTY_BROADCAST = { lineId: '', title: '', content: '', type: 'GENERAL' }
const EMPTY_SINGLE = { userId: '', lineId: '', title: '', content: '', type: 'GENERAL' }

function trunc(str, n) {
  if (!str) return '—'
  return str.length > n ? `${str.slice(0, n)}…` : str
}

function VehicleTypePills({ value, onChange }) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-sm text-muted">Type:</span>
      {[{ id: '', label: 'All' }, ...VEHICLE_TYPES].map((vt) => (
        <button
          key={vt.id}
          type="button"
          onClick={() => onChange(vt.id)}
          className={`rounded-panel border px-3 py-1 text-xs font-medium transition ${
            value === vt.id
              ? 'border-accent bg-accent text-white'
              : 'border-border text-muted hover:bg-surface-alt'
          }`}
        >
          {vt.label}
        </button>
      ))}
    </div>
  )
}

export function AdminNotificationsPage() {
  // ── send form state ──────────────────────────────────────────────────────
  const [sendMode, setSendMode] = useState('broadcast')

  // broadcast form
  const [bVehicleTypeId, setBVehicleTypeId] = useState('')
  const [bLines, setBLines] = useState([])
  const [broadcastForm, setBroadcastForm] = useState(EMPTY_BROADCAST)
  const [sending, setSending] = useState(false)
  const [success, setSuccess] = useState(null)
  const [formError, setFormError] = useState(null)

  // single user form
  const [sVehicleTypeId, setSVehicleTypeId] = useState('')
  const [sLines, setSLines] = useState([])
  const [users, setUsers] = useState([])
  const [singleForm, setSingleForm] = useState(EMPTY_SINGLE)

  // ── history state ─────────────────────────────────────────────────────────
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(false)
  const [listError, setListError] = useState(null)
  const [sortDir, setSortDir] = useState('desc')
  const [nameSearch, setNameSearch] = useState('')

  // ── load lines for broadcast form ────────────────────────────────────────
  useEffect(() => {
    const q = bVehicleTypeId ? `?vehicleTypeId=${bVehicleTypeId}` : ''
    setBroadcastForm((f) => ({ ...f, lineId: '' }))
    gatewayClient.getLines(q).then(setBLines).catch(() => {})
  }, [bVehicleTypeId])

  // ── load lines for single form ───────────────────────────────────────────
  useEffect(() => {
    const q = sVehicleTypeId ? `?vehicleTypeId=${sVehicleTypeId}` : ''
    setSingleForm((f) => ({ ...f, lineId: '' }))
    gatewayClient.getLines(q).then(setSLines).catch(() => {})
  }, [sVehicleTypeId])

  // ── load users for single form ───────────────────────────────────────────
  useEffect(() => {
    if (sendMode !== 'single') return
    gatewayClient.getAllUsers('?page=0&size=100')
      .then((res) => setUsers(res.content ?? res))
      .catch(() => {})
  }, [sendMode])

  // ── load history ─────────────────────────────────────────────────────────
  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await gatewayClient.getAllNotifications(`?page=${page}&size=20&sort=sentAt,${sortDir}`)
      setData(res)
    } catch (err) {
      setListError(err.message)
    } finally {
      setLoading(false)
    }
  }, [page, sortDir])

  useEffect(() => { load() }, [load])

  // ── filtered rows ────────────────────────────────────────────────────────
  const visibleRows = (() => {
    let rows = data.content ?? []
    if (nameSearch.trim()) {
      const q = nameSearch.trim().toLowerCase()
      rows = rows.filter((r) => (r.userFullName ?? r.userEmail ?? '').toLowerCase().includes(q))
    }
    return rows
  })()

  // ── send broadcast ───────────────────────────────────────────────────────
  async function handleBroadcast(e) {
    e.preventDefault()
    setSending(true)
    setFormError(null)
    setSuccess(null)
    try {
      const res = await gatewayClient.broadcastNotification({
        lineId: Number(broadcastForm.lineId),
        title: broadcastForm.title,
        content: broadcastForm.content,
        type: broadcastForm.type,
      })
      setSuccess(`Broadcast sent to ${res.notificationsCreated ?? 'all'} subscribers: "${broadcastForm.title}"`)
      setBroadcastForm(EMPTY_BROADCAST)
      load()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSending(false)
    }
  }

  // ── send single notification ─────────────────────────────────────────────
  async function handleSingle(e) {
    e.preventDefault()
    setSending(true)
    setFormError(null)
    setSuccess(null)
    try {
      const selectedUser = users.find((u) => String(u.id) === String(singleForm.userId))
      const selectedLine = sLines.find((l) => String(l.id) === String(singleForm.lineId))
      await gatewayClient.createNotification({
        userId: Number(singleForm.userId),
        userFullName: selectedUser?.fullName ?? null,
        userEmail: selectedUser?.email ?? null,
        lineId: singleForm.lineId ? Number(singleForm.lineId) : null,
        lineCode: selectedLine?.code ?? null,
        lineName: selectedLine?.name ?? null,
        title: singleForm.title,
        content: singleForm.content,
        type: singleForm.type,
      })
      setSuccess(`Notification sent to ${selectedUser?.fullName ?? 'user'}: "${singleForm.title}"`)
      setSingleForm(EMPTY_SINGLE)
      load()
    } catch (err) {
      setFormError(err.message)
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
      setListError(err.message)
    }
  }

  const SELECT_CLS = 'mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink'
  const INPUT_CLS = 'mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink'

  const columns = [
    { key: 'sentAt', label: 'Date', render: (r) => r.sentAt ? new Date(r.sentAt).toLocaleString() : '—' },
    { key: 'title', label: 'Title' },
    { key: 'content', label: 'Message', render: (r) => trunc(r.content, 60) },
    {
      key: 'type', label: 'Type', render: (r) => (
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${TYPE_BADGE[r.type] ?? 'bg-gray-100 text-gray-600'}`}>
          {r.type}
        </span>
      )
    },
    {
      key: 'line', label: 'Line', render: (r) =>
        r.lineId ? `${r.lineCode ?? ''} – ${r.lineName ?? ''}`.trim().replace(/^–\s*/, '') : '—'
    },
    {
      key: 'recipient', label: 'Recipient', render: (r) =>
        r.userFullName ?? r.userEmail ?? `ID ${r.userId}`
    },
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
        <p className="mt-1 text-sm text-muted">Send notifications and view history.</p>
      </div>

      <PanelCard tone="soft">
        {/* mode tabs */}
        <div className="mb-4 flex gap-1 border-b border-border">
          {[{ key: 'broadcast', label: 'Broadcast to Line' }, { key: 'single', label: 'To Specific User' }].map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => { setSendMode(tab.key); setFormError(null); setSuccess(null) }}
              className={`px-4 py-2 text-sm font-medium transition border-b-2 -mb-px ${
                sendMode === tab.key
                  ? 'border-accent text-accent'
                  : 'border-transparent text-muted hover:text-ink'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {sendMode === 'broadcast' && (
          <form onSubmit={handleBroadcast} className="space-y-3">
            <VehicleTypePills value={bVehicleTypeId} onChange={setBVehicleTypeId} />
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block">
                <span className="text-xs text-muted">Line</span>
                <select
                  required
                  value={broadcastForm.lineId}
                  onChange={(e) => setBroadcastForm((f) => ({ ...f, lineId: e.target.value }))}
                  className={SELECT_CLS}
                >
                  <option value="">— Select line —</option>
                  {bLines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="text-xs text-muted">Type</span>
                <select
                  value={broadcastForm.type}
                  onChange={(e) => setBroadcastForm((f) => ({ ...f, type: e.target.value }))}
                  className={SELECT_CLS}
                >
                  {NOTIFICATION_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">Title</span>
                <input
                  required
                  value={broadcastForm.title}
                  onChange={(e) => setBroadcastForm((f) => ({ ...f, title: e.target.value }))}
                  className={INPUT_CLS}
                />
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">Message</span>
                <textarea
                  required
                  rows={3}
                  value={broadcastForm.content}
                  onChange={(e) => setBroadcastForm((f) => ({ ...f, content: e.target.value }))}
                  className={INPUT_CLS}
                />
              </label>
            </div>
            <button
              type="submit"
              disabled={sending}
              className="rounded-panel border border-accent bg-accent px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60"
            >
              {sending ? 'Sending…' : 'Send Broadcast'}
            </button>
          </form>
        )}

        {sendMode === 'single' && (
          <form onSubmit={handleSingle} className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">User</span>
                <select
                  required
                  value={singleForm.userId}
                  onChange={(e) => setSingleForm((f) => ({ ...f, userId: e.target.value }))}
                  className={SELECT_CLS}
                >
                  <option value="">— Select user —</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>{u.fullName} ({u.email})</option>
                  ))}
                </select>
              </label>
            </div>

            <VehicleTypePills value={sVehicleTypeId} onChange={setSVehicleTypeId} />

            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block">
                <span className="text-xs text-muted">Line (optional)</span>
                <select
                  value={singleForm.lineId}
                  onChange={(e) => setSingleForm((f) => ({ ...f, lineId: e.target.value }))}
                  className={SELECT_CLS}
                >
                  <option value="">— No specific line —</option>
                  {sLines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="text-xs text-muted">Type</span>
                <select
                  value={singleForm.type}
                  onChange={(e) => setSingleForm((f) => ({ ...f, type: e.target.value }))}
                  className={SELECT_CLS}
                >
                  {NOTIFICATION_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">Title</span>
                <input
                  required
                  value={singleForm.title}
                  onChange={(e) => setSingleForm((f) => ({ ...f, title: e.target.value }))}
                  className={INPUT_CLS}
                />
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">Message</span>
                <textarea
                  required
                  rows={3}
                  value={singleForm.content}
                  onChange={(e) => setSingleForm((f) => ({ ...f, content: e.target.value }))}
                  className={INPUT_CLS}
                />
              </label>
            </div>
            <button
              type="submit"
              disabled={sending}
              className="rounded-panel border border-accent bg-accent px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60"
            >
              {sending ? 'Sending…' : 'Send Notification'}
            </button>
          </form>
        )}

        <div className="mt-3 space-y-2">
          <ErrorAlert error={formError} onDismiss={() => setFormError(null)} />
          <SuccessAlert message={success} onDismiss={() => setSuccess(null)} />
        </div>
      </PanelCard>

      <div>
        <h3 className="mb-2 text-sm font-semibold text-ink">Notification History</h3>

        <div className="mb-3 flex flex-wrap items-center gap-3">
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
          <input
            type="search"
            placeholder="Search by recipient name…"
            value={nameSearch}
            onChange={(e) => setNameSearch(e.target.value)}
            className="w-72 rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted"
          />
        </div>

        <ErrorAlert error={listError} onDismiss={() => setListError(null)} />

        <DataTable
          columns={columns}
          rows={visibleRows}
          page={page}
          totalPages={data.totalPages ?? 0}
          onPageChange={(p) => setPage(p)}
          loading={loading}
        />
      </div>
    </div>
  )
}

export default AdminNotificationsPage
