import { useEffect, useState, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { DataTable } from '../../components/admin/DataTable'
import { AdminPagePanel, SELECT_CLS } from '../../components/common/AdminPagePanel'
import { StatusBadge } from '../../components/common/StatusBadge'
import { ErrorAlert, SuccessAlert } from '../../components/common/Alerts'
import { VEHICLE_TYPE_META_BY_ID } from '../../constants/vehicleColors'
import { gatewayClient } from '../../services/gatewayClient'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)
const NOTIFICATION_TYPES = ['GENERAL', 'DELAY', 'DISRUPTION', 'ROUTE_CHANGE', 'TIMETABLE_CHANGE', 'UPCOMING_DEPARTURE']

const EMPTY_BROADCAST = { lineId: '', title: '', content: '', type: 'GENERAL' }
const EMPTY_SINGLE = { userId: '', lineId: '', title: '', content: '', type: 'GENERAL' }

function trunc(str, n) {
  if (!str) return '—'
  return str.length > n ? `${str.slice(0, n)}…` : str
}

function VehicleTypePills({ value, onChange, options }) {
  return (
    <div className="flex flex-wrap items-center gap-1">
      {options.map((vt) => (
        <button
          key={vt.id}
          type="button"
          onClick={() => onChange(vt.id)}
          className={`rounded-panel border px-2.5 py-1 text-xs font-medium transition ${
            value === vt.id
              ? 'border-accent bg-accent text-white shadow-sm'
              : 'border-border text-muted hover:border-accent-subtle hover:text-ink'
          }`}
        >
          {vt.label}
        </button>
      ))}
    </div>
  )
}

export function AdminNotificationsPage() {
  const { t } = useTranslation('admin-notifications')
  const [sendMode, setSendMode] = useState('broadcast')

  const [bVehicleTypeId, setBVehicleTypeId] = useState('')
  const [bLines, setBLines] = useState([])
  const [broadcastForm, setBroadcastForm] = useState(EMPTY_BROADCAST)
  const [sending, setSending] = useState(false)
  const [success, setSuccess] = useState(null)
  const [formError, setFormError] = useState(null)

  const [sVehicleTypeId, setSVehicleTypeId] = useState('')
  const [sLines, setSLines] = useState([])
  const [users, setUsers] = useState([])
  const [singleForm, setSingleForm] = useState(EMPTY_SINGLE)

  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(false)
  const [listError, setListError] = useState(null)
  const [sortDir, setSortDir] = useState('desc')
  const [nameSearch, setNameSearch] = useState('')
  const [broadcastLinesError, setBroadcastLinesError] = useState(null)
  const [singleLinesError, setSingleLinesError] = useState(null)
  const [usersError, setUsersError] = useState(null)

  useEffect(() => {
    let active = true
    const q = bVehicleTypeId ? `?vehicleTypeId=${bVehicleTypeId}` : ''
    setBroadcastForm((f) => ({ ...f, lineId: '' }))

    const loadBroadcastLines = async () => {
      try {
        setBroadcastLinesError(null)
        const response = await gatewayClient.getLines(q)
        if (active) setBLines(response)
      } catch (err) {
        if (active) { setBLines([]); setBroadcastLinesError(err.message || t('lines_load_failed')) }
      }
    }

    loadBroadcastLines()

    return () => { active = false }
  }, [bVehicleTypeId, t])

  useEffect(() => {
    let active = true
    const q = sVehicleTypeId ? `?vehicleTypeId=${sVehicleTypeId}` : ''
    setSingleForm((f) => ({ ...f, lineId: '' }))

    const loadSingleLines = async () => {
      try {
        setSingleLinesError(null)
        const response = await gatewayClient.getLines(q)
        if (active) setSLines(response)
      } catch (err) {
        if (active) { setSLines([]); setSingleLinesError(err.message || t('lines_load_failed')) }
      }
    }

    loadSingleLines()

    return () => { active = false }
  }, [sVehicleTypeId, t])

  useEffect(() => {
    if (sendMode !== 'single') return

    let active = true
    const loadUsers = async () => {
      try {
        setUsersError(null)
        const res = await gatewayClient.getAllUsers('?page=0&size=100')
        if (active) setUsers(res.content ?? res)
      } catch (err) {
        if (active) { setUsers([]); setUsersError(err.message || t('users_load_failed')) }
      }
    }

    loadUsers()

    return () => { active = false }
  }, [sendMode, t])

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

  const visibleRows = (() => {
    let rows = data.content ?? []
    if (nameSearch.trim()) {
      const q = nameSearch.trim().toLowerCase()
      rows = rows.filter((r) => (r.userFullName ?? r.userEmail ?? '').toLowerCase().includes(q))
    }
    return rows
  })()

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
    if (!window.confirm(t('delete_confirm'))) return
    try {
      await gatewayClient.deleteNotification(id)
      load()
    } catch (err) {
      setListError(err.message)
    }
  }

  const INPUT_CLS = `${SELECT_CLS} w-full`

  const columns = [
    { key: 'sentAt', label: t('col_date'), render: (r) => r.sentAt ? new Date(r.sentAt).toLocaleString() : '—' },
    { key: 'title', label: t('col_title') },
    { key: 'content', label: t('col_message'), render: (r) => trunc(r.content, 60) },
    {
      key: 'type', label: t('col_type'), render: (r) => <StatusBadge status={r.type} />
    },
    {
      key: 'line', label: t('col_line'), render: (r) =>
        r.lineId ? `${r.lineCode ?? ''} – ${r.lineName ?? ''}`.trim().replace(/^–\s*/, '') : '—'
    },
    {
      key: 'recipient', label: t('col_recipient'), render: (r) =>
        r.userFullName ?? r.userEmail ?? `ID ${r.userId}`
    },
    {
      key: 'actions', label: t('col_actions'), render: (r) => (
        <button type="button" onClick={() => handleDelete(r.id)}
          className="rounded-panel border border-danger-soft px-2.5 py-1 text-xs font-medium text-danger transition hover:bg-danger-soft/20">
          {t('delete')}
        </button>
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

      <div className="rounded-panel border border-border bg-surface-soft p-4">
        <div className="mb-4 flex gap-1 border-b border-border">
          {[{ key: 'broadcast', label: t('tab_broadcast') }, { key: 'single', label: t('tab_user') }].map((tab) => (
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
            <VehicleTypePills value={bVehicleTypeId} onChange={setBVehicleTypeId} options={typePillOptions} />
            {broadcastLinesError && <ErrorAlert error={broadcastLinesError} onDismiss={() => setBroadcastLinesError(null)} />}
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block">
                <span className="text-xs text-muted">{t('line_label')}</span>
                <select required value={broadcastForm.lineId}
                  onChange={(e) => setBroadcastForm((f) => ({ ...f, lineId: e.target.value }))}
                  className={INPUT_CLS}>
                  <option value="">{t('select_line')}</option>
                  {bLines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="text-xs text-muted">{t('type_label')}</span>
                <select value={broadcastForm.type}
                  onChange={(e) => setBroadcastForm((f) => ({ ...f, type: e.target.value }))}
                  className={INPUT_CLS}>
                  {NOTIFICATION_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">{t('title_label')}</span>
                <input required value={broadcastForm.title}
                  onChange={(e) => setBroadcastForm((f) => ({ ...f, title: e.target.value }))}
                  className={INPUT_CLS} />
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">{t('message_label')}</span>
                <textarea required rows={3} value={broadcastForm.content}
                  onChange={(e) => setBroadcastForm((f) => ({ ...f, content: e.target.value }))}
                  className={INPUT_CLS} />
              </label>
            </div>
            <button type="submit" disabled={sending}
              className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white shadow-sm transition hover:bg-accent-strong disabled:opacity-60">
              {sending ? t('sending') : t('send_broadcast')}
            </button>
          </form>
        )}

        {sendMode === 'single' && (
          <form onSubmit={handleSingle} className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-2">
              {usersError && <ErrorAlert error={usersError} onDismiss={() => setUsersError(null)} />}
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">{t('user_label')}</span>
                <select required value={singleForm.userId}
                  onChange={(e) => setSingleForm((f) => ({ ...f, userId: e.target.value }))}
                  className={INPUT_CLS}>
                  <option value="">{t('select_user')}</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>{u.fullName} ({u.email})</option>
                  ))}
                </select>
              </label>
            </div>

            <VehicleTypePills value={sVehicleTypeId} onChange={setSVehicleTypeId} options={typePillOptions} />
            {singleLinesError && <ErrorAlert error={singleLinesError} onDismiss={() => setSingleLinesError(null)} />}

            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block">
                <span className="text-xs text-muted">{t('line_optional')}</span>
                <select value={singleForm.lineId}
                  onChange={(e) => setSingleForm((f) => ({ ...f, lineId: e.target.value }))}
                  className={INPUT_CLS}>
                  <option value="">{t('no_line')}</option>
                  {sLines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
                </select>
              </label>
              <label className="block">
                <span className="text-xs text-muted">{t('type_label')}</span>
                <select value={singleForm.type}
                  onChange={(e) => setSingleForm((f) => ({ ...f, type: e.target.value }))}
                  className={INPUT_CLS}>
                  {NOTIFICATION_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">{t('title_label')}</span>
                <input required value={singleForm.title}
                  onChange={(e) => setSingleForm((f) => ({ ...f, title: e.target.value }))}
                  className={INPUT_CLS} />
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs text-muted">{t('message_label')}</span>
                <textarea required rows={3} value={singleForm.content}
                  onChange={(e) => setSingleForm((f) => ({ ...f, content: e.target.value }))}
                  className={INPUT_CLS} />
              </label>
            </div>
            <button type="submit" disabled={sending}
              className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white shadow-sm transition hover:bg-accent-strong disabled:opacity-60">
              {sending ? t('sending') : t('send_user')}
            </button>
          </form>
        )}

        <div className="mt-3 space-y-2">
          <ErrorAlert error={formError} onDismiss={() => setFormError(null)} />
          <SuccessAlert message={success} onDismiss={() => setSuccess(null)} />
        </div>
      </div>

      <div>
        <h3 className="mb-3 text-base font-semibold text-ink">{t('history_title')}</h3>

        <div className="mb-3 flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-1">
            <span className="text-sm text-muted">{t('sort')}</span>
            {[{ value: 'desc', label: t('latest_first') }, { value: 'asc', label: t('earliest_first') }].map((opt) => (
              <button key={opt.value} type="button" onClick={() => { setSortDir(opt.value); setPage(0) }}
                className={`rounded-panel border px-2.5 py-1 text-xs font-medium transition ${
                  sortDir === opt.value
                    ? 'border-accent bg-accent text-white shadow-sm'
                    : 'border-border text-muted hover:border-accent-subtle hover:text-ink'
                }`}>
                {opt.label}
              </button>
            ))}
          </div>
          <input type="search" placeholder={t('search_placeholder')}
            value={nameSearch} onChange={(e) => setNameSearch(e.target.value)}
            className={SELECT_CLS} />
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
    </AdminPagePanel>
  )
}

export default AdminNotificationsPage
