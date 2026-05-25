import { AlertCircle, Car, CheckCircle, Clock, RefreshCw, XCircle } from 'lucide-react'
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
    </div>
  )
}
