import { AlertCircle, BarChart2, Calendar, Car, CheckCircle, Clock, RefreshCw, Wrench, XCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { useAppContext } from '../context/AppContext'
import { gatewayClient } from '../services/gatewayClient'
import { VS_TYPE_MAP, STATUS_LABELS, STATUS_I18N } from './VehiclesPage'

const ALL_STATUSES = ['OPERATIONAL', 'IN_MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED']

const REQUEST_STATUS_STYLES = {
  PENDING:  'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  APPROVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  REJECTED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

const REQUEST_STATUS_ICONS = {
  PENDING:  Clock,
  APPROVED: CheckCircle,
  REJECTED: XCircle,
}

function formatDatetime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString([], {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function RequestRow({ req, vehicles }) {
  const { t: td } = useTranslation('driver')
  const { t: tv } = useTranslation('vehicles')
  const vehicle = vehicles.find((v) => v.id === req.vehicleId)
  const typeInfo = vehicle ? (VS_TYPE_MAP[vehicle.type] || VS_TYPE_MAP.BUS) : null
  const StatusIcon = REQUEST_STATUS_ICONS[req.requestStatus] || Clock

  return (
    <div className="rounded-panel border border-border px-3 py-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <div className="flex items-center gap-2">
            {typeInfo && (
              <span
                className="inline-block h-2 w-2 rounded-full"
                style={{ backgroundColor: typeInfo.color }}
              />
            )}
            <span className="text-sm font-medium text-ink">
              {req.vehicleRegistrationNumber || `Vehicle #${req.vehicleId}`}
            </span>
          </div>
          <p className="mt-0.5 text-xs text-muted">
            {td('requested_label')} <strong className="text-ink">{tv(STATUS_I18N[req.requestedStatus] || 'status_operational')}</strong>
            {' · '}{formatDatetime(req.requestedAt)}
          </p>
          {req.notes && <p className="mt-1 text-xs text-muted italic">{req.notes}</p>}
        </div>
        <span className={`flex items-center gap-1 rounded px-2 py-0.5 text-xs font-semibold ${REQUEST_STATUS_STYLES[req.requestStatus] || ''}`}>
          <StatusIcon size={11} />
          {req.requestStatus}
        </span>
      </div>
      {(req.resolutionNote || req.resolvedAt) && (
        <div className="mt-2 border-t border-border pt-2 text-xs text-muted">
          {req.resolutionNote && <p>{td('note_label')} {req.resolutionNote}</p>}
          {req.resolvedAt && <p>{td('resolved_label')} {formatDatetime(req.resolvedAt)}</p>}
        </div>
      )}
    </div>
  )
}

export function DriverPage() {
  const { t } = useTranslation('driver')
  const { t: tv } = useTranslation('vehicles')
  const { session, isAuthenticated } = useAppContext()

  const [vehicles, setVehicles] = useState([])
  const [vehiclesLoading, setVehiclesLoading] = useState(true)
  const [vehiclesError, setVehiclesError] = useState(null)

  const [selectedVehicleId, setSelectedVehicleId] = useState('')
  const [requestedStatus, setRequestedStatus] = useState('IN_MAINTENANCE')
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const [submitSuccess, setSubmitSuccess] = useState(null)

  const [myRequests, setMyRequests] = useState([])
  const [requestsLoading, setRequestsLoading] = useState(false)
  const [requestsError, setRequestsError] = useState(null)

  const isDriver = isAuthenticated && session?.role === 'DRIVER'

  useEffect(() => {
    if (!isDriver) return
    loadVehicles()
    loadMyRequests()
  }, [isDriver])

  async function loadVehicles() {
    setVehiclesLoading(true)
    setVehiclesError(null)
    try {
      const response = await gatewayClient.getVehicles('?size=200&sort=registrationNumber,asc')
      const content = Array.isArray(response?.content) ? response.content : Array.isArray(response) ? response : []
      setVehicles(content)
      if (content.length > 0) setSelectedVehicleId(String(content[0].id))
    } catch (err) {
      setVehicles([])
      setSelectedVehicleId('')
      setVehiclesError(err.message || t('vehicles_load_failed'))
    } finally {
      setVehiclesLoading(false)
    }
  }

  async function loadMyRequests() {
    if (!session?.userId) return
    setRequestsLoading(true)
    setRequestsError(null)
    try {
      const data = await gatewayClient.getMyStatusRequests(session.userId)
      setMyRequests(Array.isArray(data) ? data : [])
    } catch (err) {
      setRequestsError(err.message || 'Failed to load requests')
    } finally {
      setRequestsLoading(false)
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!selectedVehicleId) return
    setSubmitting(true)
    setSubmitError(null)
    setSubmitSuccess(null)
    try {
      await gatewayClient.createStatusRequest(Number(selectedVehicleId), {
        requestedStatus,
        requestedByUserId: session.userId,
        notes: notes.trim() || null,
      })
      setSubmitSuccess(t('request_submitted'))
      setNotes('')
      await loadMyRequests()
    } catch (err) {
      setSubmitError(err.message || 'Failed to submit request')
    } finally {
      setSubmitting(false)
    }
  }

  if (!isAuthenticated) {
    return (
      <div className="flex items-center gap-2 rounded-panel border border-border px-4 py-8 text-muted">
        <AlertCircle size={16} aria-hidden="true" />
        <span className="text-sm">{t('sign_in_required')}</span>
      </div>
    )
  }

  if (!isDriver) {
    return (
      <div className="flex items-center gap-2 rounded-panel border border-red-200 bg-red-50 px-4 py-8 text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">
        <AlertCircle size={16} aria-hidden="true" />
        <span className="text-sm">{t('access_denied')}</span>
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h2 className="flex items-center gap-2 text-lg font-semibold text-ink">
          <Car size={18} aria-hidden="true" />
          {t('title')}
        </h2>
        <p className="text-xs text-muted">
          {session?.email} · {t('role')}
        </p>
      </div>

      <section className="rounded-panel border border-border p-4">
        <h3 className="mb-3 text-sm font-semibold text-ink">{t('request_status')}</h3>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          {vehiclesError && <ErrorAlert error={vehiclesError} onDismiss={() => setVehiclesError(null)} />}

          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">{t('vehicle_label')}</span>
            <select
              required
              value={selectedVehicleId}
              onChange={(e) => setSelectedVehicleId(e.target.value)}
              disabled={vehiclesLoading || vehicles.length === 0}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none disabled:opacity-60"
            >
              {vehiclesLoading && <option value="">{t('loading_vehicles')}</option>}
              {!vehiclesLoading && vehicles.length === 0 && <option value="">{t('no_vehicles')}</option>}
              {vehicles.map((v) => {
                const vt = VS_TYPE_MAP[v.type] || VS_TYPE_MAP.BUS
                return (
                  <option key={v.id} value={v.id}>
                    {v.registrationNumber} ({vt.label}{v.internalId ? ` · #${v.internalId}` : ''})
                  </option>
                )
              })}
            </select>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">{t('requested_status')}</span>
            <select
              required
              value={requestedStatus}
              onChange={(e) => setRequestedStatus(e.target.value)}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
            >
              {ALL_STATUSES.map((s) => (
                <option key={s} value={s}>{tv(STATUS_I18N[s] || 'status_operational')}</option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">{t('reason_notes')}</span>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              placeholder={t('reason_placeholder')}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
            />
          </label>

          {submitError && <ErrorAlert error={submitError} onDismiss={() => setSubmitError(null)} />}
          {submitSuccess && <SuccessAlert message={submitSuccess} onDismiss={() => setSubmitSuccess(null)} />}

          <div className="flex justify-end">
            <button
              type="submit"
              disabled={submitting || !selectedVehicleId}
              className="rounded-panel bg-accent px-5 py-1.5 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-60"
            >
              {submitting ? t('submitting') : t('submit_request')}
            </button>
          </div>
        </form>
      </section>

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-ink">{t('my_requests')}</h3>
          <button
            type="button"
            onClick={loadMyRequests}
            disabled={requestsLoading}
            className="rounded-panel border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink"
            aria-label={t('refresh_requests')}
          >
            <RefreshCw size={13} className={requestsLoading ? 'animate-spin' : ''} aria-hidden="true" />
          </button>
        </div>

        {requestsError && <ErrorAlert error={requestsError} />}

        {!requestsLoading && myRequests.length === 0 && (
          <p className="text-sm text-muted">{t('no_requests')}</p>
        )}

        {myRequests.map((req) => (
          <RequestRow key={req.id} req={req} vehicles={vehicles} />
        ))}
      </section>

      <DriverAvailabilitySection session={session} />
      <DriverStatisticsSection session={session} />
      <DriverServiceRequestSection vehicles={vehicles} session={session} />
    </div>
  )
}

// ── Driver availability calendar ───────────────────────────────────────────────

function DriverAvailabilitySection({ session }) {
  const [driverProfile, setDriverProfile] = useState(null)
  const [availability, setAvailability] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(null)

  const today = new Date()
  const [viewYear, setViewYear] = useState(today.getFullYear())
  const [viewMonth, setViewMonth] = useState(today.getMonth())

  useEffect(() => {
    if (!session?.userId) return
    gatewayClient.getDriverByUser(session.userId)
      .then((profile) => {
        setDriverProfile(profile)
        return loadAvailability(profile.id)
      })
      .catch(() => setLoading(false))
  }, [session?.userId])

  async function loadAvailability(driverId) {
    const from = new Date(viewYear, viewMonth, 1).toISOString().split('T')[0]
    const to = new Date(viewYear, viewMonth + 2, 0).toISOString().split('T')[0]
    try {
      const data = await gatewayClient.getDriverAvailability(driverId, from, to)
      setAvailability(data || [])
    } catch {
      setAvailability([])
    } finally {
      setLoading(false)
    }
  }

  async function toggleDay(dateStr) {
    if (!driverProfile) return
    const existing = availability.find((a) => a.availableDate === dateStr)
    const newAvail = existing ? !existing.available : true
    setSaving(dateStr)
    try {
      const updated = await gatewayClient.setDriverAvailability(driverProfile.id, { date: dateStr, available: newAvail })
      setAvailability((prev) => {
        const filtered = prev.filter((a) => a.availableDate !== dateStr)
        return [...filtered, updated]
      })
    } catch (err) {
      alert('Failed to update availability: ' + err.message)
    } finally {
      setSaving(null)
    }
  }

  if (!driverProfile && !loading) return null

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
  const firstDay = new Date(viewYear, viewMonth, 1).getDay()
  const monthName = new Date(viewYear, viewMonth, 1).toLocaleString('default', { month: 'long', year: 'numeric' })

  function getAvailForDay(day) {
    const dateStr = `${viewYear}-${String(viewMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    return availability.find((a) => a.availableDate === dateStr)
  }

  return (
    <section className="rounded-panel border border-border p-4 flex flex-col gap-3">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
        <Calendar size={15} />My Availability
      </h3>
      {loading ? <p className="text-xs text-muted">Loading…</p> : (
        <>
          <div className="flex items-center gap-2">
            <button type="button" onClick={() => { const d = new Date(viewYear, viewMonth - 1, 1); setViewYear(d.getFullYear()); setViewMonth(d.getMonth()) }}
              className="rounded-panel border border-border px-2 py-1 text-xs text-muted hover:bg-surface-alt">‹</button>
            <span className="text-sm font-medium text-ink flex-1 text-center">{monthName}</span>
            <button type="button" onClick={() => { const d = new Date(viewYear, viewMonth + 1, 1); setViewYear(d.getFullYear()); setViewMonth(d.getMonth()) }}
              className="rounded-panel border border-border px-2 py-1 text-xs text-muted hover:bg-surface-alt">›</button>
          </div>
          <div className="grid grid-cols-7 gap-1 text-center">
            {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((d, i) => (
              <div key={i} className="text-[10px] font-medium text-muted pb-1">{d}</div>
            ))}
            {Array.from({ length: firstDay }).map((_, i) => <div key={`e${i}`} />)}
            {Array.from({ length: daysInMonth }, (_, i) => i + 1).map((day) => {
              const dateStr = `${viewYear}-${String(viewMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
              const avail = getAvailForDay(day)
              const isAvailable = avail ? avail.available : false
              const isPast = new Date(dateStr) < new Date(today.toISOString().split('T')[0])
              return (
                <button key={day} type="button" disabled={isPast || saving === dateStr}
                  onClick={() => toggleDay(dateStr)}
                  className={`rounded p-1.5 text-xs transition ${
                    saving === dateStr ? 'opacity-50 cursor-wait' :
                    isPast ? 'opacity-30 cursor-default' :
                    isAvailable ? 'bg-emerald-100 text-emerald-700 hover:bg-emerald-200 dark:bg-emerald-900/30 dark:text-emerald-400' :
                    'bg-surface border border-border text-muted hover:bg-surface-alt'
                  }`}>
                  {day}
                </button>
              )
            })}
          </div>
          <p className="text-[10px] text-muted">Click a day to toggle availability. Green = available.</p>
        </>
      )}
    </section>
  )
}

// ── Driver statistics ───────────────────────────────────────────────────────────

function DriverStatisticsSection({ session }) {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!session?.userId) return
    gatewayClient.getDriverByUser(session.userId)
      .then((profile) => gatewayClient.getDriverStatistics(profile.id))
      .then((data) => { setStats(data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [session?.userId])

  if (!stats && !loading) return null

  return (
    <section className="rounded-panel border border-border p-4 flex flex-col gap-3">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
        <BarChart2 size={15} />My Statistics
      </h3>
      {loading ? <p className="text-xs text-muted">Loading…</p> : stats ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="rounded-panel border border-border p-3 text-center">
            <p className="text-2xl font-bold text-ink">{stats.totalAssignments}</p>
            <p className="text-xs text-muted mt-0.5">Total Assignments</p>
          </div>
          <div className="rounded-panel border border-border p-3 text-center">
            <p className="text-2xl font-bold text-ink">{stats.totalDaysActive}</p>
            <p className="text-xs text-muted mt-0.5">Active Days</p>
          </div>
          <div className="rounded-panel border border-border p-3 text-center">
            <p className="text-2xl font-bold text-ink">{stats.lineCodesServed?.length || 0}</p>
            <p className="text-xs text-muted mt-0.5">Lines Served</p>
          </div>
          <div className="rounded-panel border border-border p-3 text-center">
            <p className="text-2xl font-bold text-ink">{stats.vehicleIdsUsed?.length || 0}</p>
            <p className="text-xs text-muted mt-0.5">Vehicles Driven</p>
          </div>
        </div>
      ) : <p className="text-xs text-muted">No statistics available.</p>}
    </section>
  )
}

// ── Driver service request (maintenance request for a vehicle) ─────────────────

function DriverServiceRequestSection({ vehicles, session }) {
  const [form, setForm] = useState({ vehicleId: '', description: '' })
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState(null)
  const [error, setError] = useState(null)
  const [driverProfile, setDriverProfile] = useState(null)

  useEffect(() => {
    if (!session?.userId) return
    gatewayClient.getDriverByUser(session.userId)
      .then(setDriverProfile)
      .catch(() => {})
  }, [session?.userId])

  async function handleSubmit(e) {
    e.preventDefault()
    if (!driverProfile) return
    setSubmitting(true)
    setError(null)
    setSuccess(null)
    try {
      await gatewayClient.createDriverServiceRequest(driverProfile.id, {
        vehicleId: parseInt(form.vehicleId, 10),
        description: form.description.trim(),
      })
      setSuccess('Service request sent to admin.')
      setForm({ vehicleId: '', description: '' })
    } catch (err) {
      setError(err.message || 'Failed to send request')
    } finally {
      setSubmitting(false)
    }
  }

  if (!driverProfile) return null

  return (
    <section className="rounded-panel border border-border p-4 flex flex-col gap-3">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
        <Wrench size={15} />Request Vehicle Service
      </h3>
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-xs font-medium text-muted">Vehicle</span>
          <select required value={form.vehicleId} onChange={(e) => setForm((p) => ({ ...p, vehicleId: e.target.value }))}
            className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none">
            <option value="">Select vehicle…</option>
            {vehicles.map((v) => (
              <option key={v.id} value={v.id}>{v.registrationNumber} ({VS_TYPE_MAP[v.type]?.label || v.type})</option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs font-medium text-muted">Description of issue</span>
          <textarea required rows={3} value={form.description}
            onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
            placeholder="Describe what needs servicing…"
            className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none" />
        </label>
        {error && <div className="text-xs text-red-600">{error}</div>}
        {success && <div className="text-xs text-emerald-600">{success}</div>}
        <div className="flex justify-end">
          <button type="submit" disabled={submitting}
            className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50">
            {submitting ? 'Sending…' : 'Send Request'}
          </button>
        </div>
      </form>
    </section>
  )
}
