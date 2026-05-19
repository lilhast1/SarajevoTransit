import {
  AlertCircle,
  ArrowLeft,
  Bell,
  CheckCircle,
  ChevronDown,
  ChevronUp,
  Clock,
  Loader2,
  MapPin,
  Wrench,
  XCircle,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { TransitMap } from '../components/map/TransitMap'
import { useAppContext } from '../context/AppContext'
import { gatewayClient } from '../services/gatewayClient'
import { VS_TYPE_MAP, STATUS_STYLES, STATUS_LABELS } from './VehiclesPage'

const VEHICLE_STATUSES = ['OPERATIONAL', 'IN_MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED']

const EMPTY_RECORD_FORM = {
  serviceStart: '',
  serviceEnd: '',
  description: '',
  partsChanged: '',
  cost: '',
}

function toIso(datetimeLocalValue) {
  if (!datetimeLocalValue) return null
  return datetimeLocalValue.length === 16 ? `${datetimeLocalValue}:00` : datetimeLocalValue
}

function formatDatetime(isoString) {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleString([], {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function defaultFrom() {
  const d = new Date(Date.now() - 24 * 60 * 60 * 1000)
  return d.toISOString().slice(0, 16)
}

function defaultTo() {
  return new Date().toISOString().slice(0, 16)
}

// ── Status Changer ─────────────────────────────────────────────────────────────

function StatusChanger({ currentStatus, vehicleId, onStatusChanged }) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [successMsg, setSuccessMsg] = useState(null)

  async function handleChange(newStatus) {
    if (newStatus === currentStatus || saving) return
    setSaving(true)
    setError(null)
    setSuccessMsg(null)
    try {
      await gatewayClient.updateVehicleStatus(vehicleId, newStatus)
      onStatusChanged(newStatus)
      setSuccessMsg(`Status updated to ${STATUS_LABELS[newStatus]}`)
      setTimeout(() => setSuccessMsg(null), 3000)
    } catch (err) {
      setError(err.message || 'Failed to update status')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="flex flex-col gap-3 rounded-panel border border-border p-4">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
        <CheckCircle size={15} />
        Change Status
      </h3>
      <div className="flex flex-wrap gap-2">
        {VEHICLE_STATUSES.map((s) => {
          const isActive = s === currentStatus
          return (
            <button
              key={s}
              type="button"
              disabled={saving || isActive}
              onClick={() => handleChange(s)}
              className={`rounded-panel border px-3 py-1.5 text-xs font-medium transition disabled:cursor-not-allowed ${
                isActive
                  ? `${STATUS_STYLES[s]} border-current`
                  : 'border-border text-muted hover:bg-surface-alt hover:text-ink disabled:opacity-50'
              }`}
            >
              {saving && !isActive ? <Loader2 size={11} className="animate-spin inline mr-1" /> : null}
              {STATUS_LABELS[s]}
            </button>
          )
        })}
      </div>
      {error && <ErrorAlert error={error} onDismiss={() => setError(null)} />}
      {successMsg && <SuccessAlert message={successMsg} onDismiss={() => setSuccessMsg(null)} />}
    </section>
  )
}

// ── GPS History ────────────────────────────────────────────────────────────────

function GpsHistorySection({ vehicleId }) {
  const [from, setFrom] = useState(defaultFrom)
  const [to, setTo] = useState(defaultTo)
  const [points, setPoints] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function loadHistory() {
    if (!from || !to) return
    setLoading(true)
    setError(null)
    try {
      const data = await gatewayClient.getVehicleLocationHistory(vehicleId, toIso(from), toIso(to))
      setPoints(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err.message || 'Failed to load history')
      setPoints(null)
    } finally {
      setLoading(false)
    }
  }

  const polyline = points ? points.map((p) => [p.latitude, p.longitude]) : []

  return (
    <section className="flex flex-col gap-3 rounded-panel border border-border p-4">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
        <MapPin size={15} />
        GPS History
      </h3>

      <div className="flex flex-wrap items-end gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-xs text-muted">From</span>
          <input
            type="datetime-local"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs text-muted">To</span>
          <input
            type="datetime-local"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
          />
        </label>
        <button
          type="button"
          onClick={loadHistory}
          disabled={loading}
          className="flex items-center gap-1.5 rounded-panel border border-accent bg-accent px-4 py-1.5 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-60"
        >
          {loading && <Loader2 size={13} className="animate-spin" />}
          Load History
        </button>
      </div>

      {error && <ErrorAlert error={error} onDismiss={() => setError(null)} />}

      {points !== null && (
        points.length === 0 ? (
          <p className="text-sm text-muted">No history data for this interval.</p>
        ) : (
          <>
            <p className="text-xs text-muted">
              {points.length} points · {formatDatetime(points.at(0)?.timestamp)} → {formatDatetime(points.at(-1)?.timestamp)}
            </p>
            <TransitMap
              polyline={polyline}
              className="h-64 rounded-panel"
            />
          </>
        )
      )}
    </section>
  )
}

// ── Service Records ────────────────────────────────────────────────────────────

function ServiceRecordsSection({ vehicleId, isAuthenticated }) {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_RECORD_FORM)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState(null)
  const [formSuccess, setFormSuccess] = useState(null)

  useEffect(() => {
    loadRecords()
  }, [vehicleId])

  async function loadRecords() {
    setLoading(true)
    setError(null)
    try {
      const data = await gatewayClient.getVehicleServiceRecords(vehicleId)
      setRecords(Array.isArray(data) ? data.slice().sort((a, b) => new Date(b.serviceStart) - new Date(a.serviceStart)) : [])
    } catch (err) {
      setError(err.message || 'Failed to load service records')
    } finally {
      setLoading(false)
    }
  }

  function setField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    setFormSuccess(null)
    try {
      const payload = {
        serviceStart: toIso(form.serviceStart),
        serviceEnd: form.serviceEnd ? toIso(form.serviceEnd) : null,
        description: form.description.trim() || null,
        partsChanged: form.partsChanged.trim() || null,
        cost: form.cost ? parseFloat(form.cost) : null,
      }
      await gatewayClient.addServiceRecord(vehicleId, payload)
      setFormSuccess('Service record added.')
      setForm(EMPTY_RECORD_FORM)
      setShowForm(false)
      await loadRecords()
    } catch (err) {
      setFormError(err.message || 'Failed to add service record')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="flex flex-col gap-3 rounded-panel border border-border p-4">
      <div className="flex items-center justify-between">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
          <Wrench size={15} />
          Service Records
        </h3>
        {isAuthenticated && (
          <button
            type="button"
            onClick={() => { setShowForm((prev) => !prev); setFormError(null); setFormSuccess(null) }}
            className="flex items-center gap-1 rounded-panel border border-border px-2.5 py-1 text-xs text-muted transition hover:bg-surface-alt hover:text-ink"
          >
            {showForm ? <ChevronUp size={13} /> : <ChevronDown size={13} />}
            {showForm ? 'Cancel' : '+ Add Record'}
          </button>
        )}
      </div>

      {formSuccess && <SuccessAlert message={formSuccess} onDismiss={() => setFormSuccess(null)} />}
      {formError && <ErrorAlert error={formError} onDismiss={() => setFormError(null)} />}

      {showForm && isAuthenticated && (
        <form onSubmit={handleSubmit} className="flex flex-col gap-3 rounded-panel border border-border bg-surface-alt p-3">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Service Start *</span>
              <input
                required
                type="datetime-local"
                value={form.serviceStart}
                onChange={(e) => setField('serviceStart', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Service End</span>
              <input
                type="datetime-local"
                value={form.serviceEnd}
                onChange={(e) => setField('serviceEnd', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
              />
            </label>
          </div>
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Description</span>
            <input
              type="text"
              value={form.description}
              onChange={(e) => setField('description', e.target.value)}
              placeholder="Engine overhaul, brake inspection…"
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
            />
          </label>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Parts Changed</span>
              <input
                type="text"
                value={form.partsChanged}
                onChange={(e) => setField('partsChanged', e.target.value)}
                placeholder="Brake pads, filters…"
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Cost (KM)</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.cost}
                onChange={(e) => setField('cost', e.target.value)}
                placeholder="0.00"
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
              />
            </label>
          </div>
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="flex items-center gap-1.5 rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-60"
            >
              {saving && <Loader2 size={13} className="animate-spin" />}
              {saving ? 'Saving…' : 'Save Record'}
            </button>
          </div>
        </form>
      )}

      {error && <ErrorAlert error={error} />}

      {loading ? (
        <div className="flex items-center gap-2 text-sm text-muted">
          <Loader2 size={14} className="animate-spin" />
          Loading records…
        </div>
      ) : records.length === 0 ? (
        <p className="text-sm text-muted">No service records yet.</p>
      ) : (
        <div className="flex flex-col gap-2">
          {records.map((r) => (
            <div key={r.id} className="rounded-panel border border-border px-3 py-2.5">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="flex items-center gap-2 text-xs text-muted">
                  <Clock size={12} />
                  <span>{formatDatetime(r.serviceStart)}</span>
                  <span>→</span>
                  <span>{r.serviceEnd ? formatDatetime(r.serviceEnd) : <em>ongoing</em>}</span>
                  {r.downtimeInHours > 0 && (
                    <span className="rounded bg-surface-alt px-1.5 py-0.5 font-medium text-ink">
                      {r.downtimeInHours}h downtime
                    </span>
                  )}
                </div>
                {r.cost != null && (
                  <span className="text-xs font-semibold text-ink">
                    {Number(r.cost).toLocaleString(undefined, { minimumFractionDigits: 2 })} KM
                  </span>
                )}
              </div>
              {r.description && <p className="mt-1 text-sm text-ink">{r.description}</p>}
              {r.partsChanged && (
                <p className="mt-0.5 text-xs text-muted">Parts: {r.partsChanged}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

// ── VehicleDetailPage ──────────────────────────────────────────────────────────

// ── Pending Requests (admin-only) ──────────────────────────────────────────────

const REQUEST_STATUS_STYLES = {
  PENDING:  'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  APPROVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  REJECTED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

function PendingRequestsSection({ vehicleId, session, onStatusChanged }) {
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [resolveNotes, setResolveNotes] = useState({})
  const [resolvingId, setResolvingId] = useState(null)
  const [resolveError, setResolveError] = useState(null)

  useEffect(() => { loadRequests() }, [vehicleId])

  async function loadRequests() {
    setLoading(true)
    setError(null)
    try {
      const data = await gatewayClient.getVehicleStatusRequests(vehicleId, 'PENDING')
      setRequests(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err.message || 'Failed to load pending requests')
    } finally {
      setLoading(false)
    }
  }

  async function handleResolve(requestId, requestedStatus, resolution) {
    setResolvingId(requestId)
    setResolveError(null)
    try {
      await gatewayClient.resolveStatusRequest(requestId, {
        resolution,
        resolvedByUserId: session?.userId,
        resolutionNote: resolveNotes[requestId] || null,
      })
      if (resolution === 'APPROVED') {
        onStatusChanged(requestedStatus)
      }
      await loadRequests()
    } catch (err) {
      setResolveError(err.message || 'Failed to resolve request')
    } finally {
      setResolvingId(null)
    }
  }

  return (
    <section className="flex flex-col gap-3 rounded-panel border border-amber-200 bg-amber-50/30 p-4 dark:border-amber-900/40 dark:bg-amber-950/10">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-ink">
        <Bell size={15} />
        Pending Status Requests
        {requests.length > 0 && (
          <span className="rounded-full bg-amber-500 px-1.5 py-0.5 text-[10px] font-bold text-white">
            {requests.length}
          </span>
        )}
      </h3>

      {resolveError && <ErrorAlert error={resolveError} onDismiss={() => setResolveError(null)} />}

      {loading ? (
        <div className="flex items-center gap-2 text-sm text-muted">
          <Loader2 size={13} className="animate-spin" />
          Loading…
        </div>
      ) : requests.length === 0 ? (
        <p className="text-sm text-muted">No pending requests for this vehicle.</p>
      ) : (
        requests.map((req) => (
          <div key={req.id} className="rounded-panel border border-border bg-surface px-3 py-3">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <div className="flex items-center gap-2">
                  <span className={`rounded px-1.5 py-0.5 text-[10px] font-semibold ${REQUEST_STATUS_STYLES.PENDING}`}>
                    PENDING
                  </span>
                  <span className="text-xs font-medium text-ink">
                    → {STATUS_LABELS[req.requestedStatus] || req.requestedStatus}
                  </span>
                </div>
                <p className="mt-0.5 text-xs text-muted">
                  Driver ID {req.requestedByUserId} · {formatDatetime(req.requestedAt)}
                </p>
                {req.notes && <p className="mt-1 text-xs italic text-muted">{req.notes}</p>}
              </div>
            </div>

            <div className="mt-3 flex flex-wrap items-center gap-2">
              <input
                type="text"
                placeholder="Resolution note (optional)"
                value={resolveNotes[req.id] || ''}
                onChange={(e) => setResolveNotes((prev) => ({ ...prev, [req.id]: e.target.value }))}
                className="min-w-0 flex-1 rounded-panel border border-border bg-surface px-3 py-1 text-xs text-ink placeholder:text-muted focus:border-accent focus:outline-none"
              />
              <button
                type="button"
                disabled={resolvingId === req.id}
                onClick={() => handleResolve(req.id, req.requestedStatus, 'APPROVED')}
                className="flex items-center gap-1 rounded-panel bg-emerald-600 px-3 py-1 text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-60"
              >
                <CheckCircle size={12} />
                Approve
              </button>
              <button
                type="button"
                disabled={resolvingId === req.id}
                onClick={() => handleResolve(req.id, req.requestedStatus, 'REJECTED')}
                className="flex items-center gap-1 rounded-panel border border-red-300 bg-red-50 px-3 py-1 text-xs font-medium text-red-700 transition hover:bg-red-100 disabled:opacity-60 dark:border-red-800 dark:bg-red-950/30 dark:text-red-400"
              >
                <XCircle size={12} />
                Reject
              </button>
            </div>
          </div>
        ))
      )}
    </section>
  )
}

export function VehicleDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated, session } = useAppContext()

  const [vehicle, setVehicle] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    gatewayClient
      .getVehicleById(id)
      .then((data) => { if (active) { setVehicle(data); setLoading(false) } })
      .catch((err) => { if (active) { setError(err.message || 'Vehicle not found'); setLoading(false) } })
    return () => { active = false }
  }, [id])

  function handleStatusChanged(newStatus) {
    setVehicle((prev) => prev ? { ...prev, status: newStatus } : prev)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20 gap-2 text-muted">
        <Loader2 size={18} className="animate-spin" />
        <span className="text-sm">Loading vehicle…</span>
      </div>
    )
  }

  if (error || !vehicle) {
    return (
      <div className="flex flex-col gap-3 py-8">
        <button
          type="button"
          onClick={() => navigate('/vehicles')}
          className="flex items-center gap-1.5 self-start text-sm text-muted transition hover:text-ink"
        >
          <ArrowLeft size={15} />
          Back to Fleet
        </button>
        <ErrorAlert error={error || 'Vehicle not found'} />
      </div>
    )
  }

  const typeInfo = VS_TYPE_MAP[vehicle.type] || VS_TYPE_MAP.BUS
  const statusStyle = STATUS_STYLES[vehicle.status] || STATUS_STYLES.OPERATIONAL
  const statusLabel = STATUS_LABELS[vehicle.status] || vehicle.status

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4">
      {/* Back */}
      <button
        type="button"
        onClick={() => navigate('/vehicles')}
        className="flex items-center gap-1.5 self-start text-sm text-muted transition hover:text-ink"
      >
        <ArrowLeft size={15} />
        Back to Fleet
      </button>

      {/* Header card */}
      <div className="rounded-panel border border-border p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-ink">{vehicle.registrationNumber}</h2>
            <div className="mt-1 flex flex-wrap items-center gap-2">
              <span className="flex items-center gap-1.5 text-sm text-muted">
                <span
                  className="inline-block h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: typeInfo.color }}
                />
                {typeInfo.label}
              </span>
              <span className={`rounded px-2 py-0.5 text-xs font-semibold ${statusStyle}`}>
                {statusLabel}
              </span>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-right text-xs text-muted sm:grid-cols-3">
            {vehicle.internalId && (
              <div>
                <div className="font-medium text-ink">{vehicle.internalId}</div>
                <div>Internal ID</div>
              </div>
            )}
            {vehicle.capacity && (
              <div>
                <div className="font-medium text-ink">{vehicle.capacity}</div>
                <div>Capacity</div>
              </div>
            )}
            {vehicle.manufactureDate && (
              <div>
                <div className="font-medium text-ink">{vehicle.manufactureDate}</div>
                <div>Manufactured</div>
              </div>
            )}
            {vehicle.lastLat != null && (
              <div className="col-span-2 sm:col-span-3">
                <div className="font-medium text-ink">
                  {vehicle.lastLat.toFixed(5)}, {vehicle.lastLon.toFixed(5)}
                </div>
                <div>Last GPS{vehicle.lastGpsUpdate ? ` · ${formatDatetime(vehicle.lastGpsUpdate)}` : ''}</div>
              </div>
            )}
          </div>
        </div>

        {!isAuthenticated && (
          <p className="mt-3 rounded-panel border border-border bg-surface-alt px-3 py-2 text-xs text-muted">
            <AlertCircle size={12} className="mr-1 inline" />
            Sign in to change status and add service records.
          </p>
        )}
      </div>

      {/* Status changer — auth-protected */}
      {isAuthenticated && (
        <StatusChanger
          currentStatus={vehicle.status}
          vehicleId={vehicle.id}
          onStatusChanged={handleStatusChanged}
        />
      )}

      {/* GPS History */}
      <GpsHistorySection vehicleId={vehicle.id} />

      {/* Service Records */}
      <ServiceRecordsSection vehicleId={vehicle.id} isAuthenticated={isAuthenticated} />

      {/* Pending Status Requests — admin only */}
      {isAuthenticated && session?.role === 'ADMIN' && (
        <PendingRequestsSection
          vehicleId={vehicle.id}
          session={session}
          onStatusChanged={handleStatusChanged}
        />
      )}
    </div>
  )
}
