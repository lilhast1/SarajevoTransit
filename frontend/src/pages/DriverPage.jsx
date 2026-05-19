import { AlertCircle, Car, CheckCircle, Clock, RefreshCw, XCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { useAppContext } from '../context/AppContext'
import { gatewayClient } from '../services/gatewayClient'
import { VS_TYPE_MAP, STATUS_LABELS } from './VehiclesPage'

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
            Requested: <strong className="text-ink">{STATUS_LABELS[req.requestedStatus] || req.requestedStatus}</strong>
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
          {req.resolutionNote && <p>Note: {req.resolutionNote}</p>}
          {req.resolvedAt && <p>Resolved: {formatDatetime(req.resolvedAt)}</p>}
        </div>
      )}
    </div>
  )
}

export function DriverPage() {
  const { session, isAuthenticated } = useAppContext()

  const [vehicles, setVehicles] = useState([])
  const [vehiclesLoading, setVehiclesLoading] = useState(true)

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
    try {
      const response = await gatewayClient.getVehicles('?size=200&sort=registrationNumber,asc')
      const content = Array.isArray(response?.content) ? response.content : Array.isArray(response) ? response : []
      setVehicles(content)
      if (content.length > 0) setSelectedVehicleId(String(content[0].id))
    } catch {
      // silent — form won't have options
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
      setSubmitSuccess('Request submitted. The admin will review it shortly.')
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
        <AlertCircle size={16} />
        <span className="text-sm">Sign in to access the Driver Portal.</span>
      </div>
    )
  }

  if (!isDriver) {
    return (
      <div className="flex items-center gap-2 rounded-panel border border-red-200 bg-red-50 px-4 py-8 text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">
        <AlertCircle size={16} />
        <span className="text-sm">Access denied. This page is for drivers only.</span>
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h2 className="flex items-center gap-2 text-lg font-semibold text-ink">
          <Car size={18} />
          Driver Portal
        </h2>
        <p className="text-xs text-muted">
          {session?.email} · Driver
        </p>
      </div>

      {/* Submit request */}
      <section className="rounded-panel border border-border p-4">
        <h3 className="mb-3 text-sm font-semibold text-ink">Request Status Change</h3>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Vehicle</span>
            <select
              required
              value={selectedVehicleId}
              onChange={(e) => setSelectedVehicleId(e.target.value)}
              disabled={vehiclesLoading || vehicles.length === 0}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none disabled:opacity-60"
            >
              {vehiclesLoading && <option value="">Loading vehicles…</option>}
              {!vehiclesLoading && vehicles.length === 0 && <option value="">No vehicles available</option>}
              {vehicles.map((v) => {
                const t = VS_TYPE_MAP[v.type] || VS_TYPE_MAP.BUS
                return (
                  <option key={v.id} value={v.id}>
                    {v.registrationNumber} ({t.label}{v.internalId ? ` · #${v.internalId}` : ''})
                  </option>
                )
              })}
            </select>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Requested Status</span>
            <select
              required
              value={requestedStatus}
              onChange={(e) => setRequestedStatus(e.target.value)}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
            >
              {ALL_STATUSES.map((s) => (
                <option key={s} value={s}>{STATUS_LABELS[s] || s}</option>
              ))}
            </select>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Reason / Notes</span>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              placeholder="Describe the issue or reason for this request…"
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
              {submitting ? 'Submitting…' : 'Submit Request'}
            </button>
          </div>
        </form>
      </section>

      {/* My requests */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-ink">My Requests</h3>
          <button
            type="button"
            onClick={loadMyRequests}
            disabled={requestsLoading}
            className="rounded-panel border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink"
            aria-label="Refresh requests"
          >
            <RefreshCw size={13} className={requestsLoading ? 'animate-spin' : ''} />
          </button>
        </div>

        {requestsError && <ErrorAlert error={requestsError} />}

        {!requestsLoading && myRequests.length === 0 && (
          <p className="text-sm text-muted">No requests submitted yet.</p>
        )}

        {myRequests.map((req) => (
          <RequestRow key={req.id} req={req} vehicles={vehicles} />
        ))}
      </section>
    </div>
  )
}
