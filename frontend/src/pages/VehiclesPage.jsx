import { AlertCircle, Plus, RefreshCw, Truck, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TransitMap } from '../components/map/TransitMap'
import { useAppContext } from '../context/AppContext'
import { gatewayClient } from '../services/gatewayClient'

const POLL_INTERVAL_MS = 5000

export const VS_TYPE_MAP = {
  BUS:     { typeId: 2, key: 'bus',        label: 'Bus',        color: '#3b82f6' },
  TRAM:    { typeId: 4, key: 'tram',       label: 'Tram',       color: '#e63946' },
  TROLLEY: { typeId: 3, key: 'trolleybus', label: 'Trolleybus', color: '#10b981' },
  MINIBUS: { typeId: 1, key: 'minibus',    label: 'Minibus',    color: '#f97316' },
}

export const STATUS_STYLES = {
  OPERATIONAL:    'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  IN_MAINTENANCE: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  OUT_OF_SERVICE: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  RETIRED:        'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400',
}

export const STATUS_LABELS = {
  OPERATIONAL:    'Operational',
  IN_MAINTENANCE: 'Maintenance',
  OUT_OF_SERVICE: 'Out of service',
  RETIRED:        'Retired',
}

const TYPE_FILTERS = ['all', 'bus', 'tram', 'trolleybus', 'minibus']

const VEHICLE_TYPES = ['BUS', 'TRAM', 'TROLLEY', 'MINIBUS']
const VEHICLE_STATUSES = ['OPERATIONAL', 'IN_MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED']

const EMPTY_ADD_FORM = {
  registrationNumber: '',
  internalId: '',
  type: 'BUS',
  capacity: '',
  status: 'OPERATIONAL',
  manufactureDate: '',
}

export function normalizeFleetVehicle(v) {
  if (v.lastLat == null || v.lastLon == null) return null
  const t = VS_TYPE_MAP[v.type] || VS_TYPE_MAP.BUS
  return {
    id: v.id,
    lineCode: v.internalId || `#${v.id}`,
    name: `${t.label} ${v.registrationNumber}`,
    latitude: v.lastLat,
    longitude: v.lastLon,
    typeId: t.typeId,
    type: t.key,
    typeLabel: t.label,
    color: t.color,
  }
}

function PollingStatusBadge({ status, lastUpdatedAt, theme }) {
  const [now, setNow] = useState(Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 250)
    return () => window.clearInterval(timer)
  }, [])

  const elapsedMs = lastUpdatedAt ? Math.max(0, now - lastUpdatedAt) : 0
  const progress = Math.min(1, elapsedMs / POLL_INTERVAL_MS)
  const progressDeg = Math.round(progress * 360)
  const fillColor = theme === 'dark' ? '#cbd5e1' : '#334155'

  let text = 'Idle'
  if (status === 'loading') text = 'Updating'
  else if (status === 'ok') text = `Updated ${Math.floor(elapsedMs / 1000)}s ago`
  else if (status === 'error') text = 'Retrying'

  const fillDeg = status === 'error' ? 360 : progressDeg
  const ringFill = `conic-gradient(${fillColor} 0deg ${fillDeg}deg, transparent ${fillDeg}deg 360deg)`

  return (
    <span
      className="inline-flex h-5 w-5 rounded-full"
      style={{
        background: ringFill,
        WebkitMask: 'radial-gradient(farthest-side, transparent calc(100% - 3px), #000 0)',
        mask: 'radial-gradient(farthest-side, transparent calc(100% - 3px), #000 0)',
      }}
      title={text}
      aria-label={text}
      role="status"
    />
  )
}

function VehicleRow({ vehicle, onClick }) {
  const typeInfo = VS_TYPE_MAP[vehicle.type] || VS_TYPE_MAP.BUS
  const statusStyle = STATUS_STYLES[vehicle.status] || STATUS_STYLES.OPERATIONAL
  const statusLabel = STATUS_LABELS[vehicle.status] || vehicle.status
  const hasGps = vehicle.lastLat != null && vehicle.lastLon != null

  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full rounded-panel border border-border px-3 py-2.5 text-left transition hover:bg-surface-alt hover:border-accent/50"
    >
      <div className="flex items-center gap-2">
        <span
          className="inline-flex h-2.5 w-2.5 shrink-0 rounded-full"
          style={{ backgroundColor: typeInfo.color }}
        />
        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-ink">
            {vehicle.registrationNumber}
          </span>
          <span className="block text-xs text-muted">
            {typeInfo.label}{vehicle.internalId ? ` · #${vehicle.internalId}` : ''}{vehicle.capacity ? ` · ${vehicle.capacity} cap` : ''}
          </span>
        </span>
        <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${statusStyle}`}>
          {statusLabel}
        </span>
      </div>
      {!hasGps && (
        <p className="mt-1 text-[10px] text-muted italic">No GPS fix</p>
      )}
    </button>
  )
}

function AddVehicleModal({ onClose, onCreated }) {
  const [form, setForm] = useState(EMPTY_ADD_FORM)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  function setField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const payload = {
        registrationNumber: form.registrationNumber.trim(),
        internalId: form.internalId.trim() || undefined,
        type: form.type,
        capacity: parseInt(form.capacity, 10),
        status: form.status,
        manufactureDate: form.manufactureDate || undefined,
      }
      const vehicle = await gatewayClient.addVehicle(payload)
      onCreated(vehicle)
    } catch (err) {
      setError(err.message || 'Failed to add vehicle')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <button
        type="button"
        aria-label="Close"
        className="fixed inset-0 z-[1100] bg-black/40"
        onClick={onClose}
      />
      <div className="fixed left-1/2 top-1/2 z-[1200] w-[min(480px,92vw)] -translate-x-1/2 -translate-y-1/2 rounded-panel border border-border bg-surface p-5 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-base font-semibold text-ink">Add Vehicle</h3>
          <button
            type="button"
            onClick={onClose}
            className="rounded-panel border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink"
            aria-label="Close"
          >
            <X size={15} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Registration No. *</span>
              <input
                required
                type="text"
                value={form.registrationNumber}
                onChange={(e) => setField('registrationNumber', e.target.value)}
                placeholder="A12-E-345"
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Internal ID</span>
              <input
                type="text"
                value={form.internalId}
                onChange={(e) => setField('internalId', e.target.value)}
                placeholder="401"
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
              />
            </label>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Type *</span>
              <select
                required
                value={form.type}
                onChange={(e) => setField('type', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
              >
                {VEHICLE_TYPES.map((t) => (
                  <option key={t} value={t}>{VS_TYPE_MAP[t]?.label || t}</option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Capacity *</span>
              <input
                required
                type="number"
                min="1"
                value={form.capacity}
                onChange={(e) => setField('capacity', e.target.value)}
                placeholder="80"
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
              />
            </label>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Status *</span>
              <select
                required
                value={form.status}
                onChange={(e) => setField('status', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
              >
                {VEHICLE_STATUSES.map((s) => (
                  <option key={s} value={s}>{STATUS_LABELS[s] || s}</option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Manufacture Date</span>
              <input
                type="date"
                value={form.manufactureDate}
                onChange={(e) => setField('manufactureDate', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
              />
            </label>
          </div>

          {error && (
            <div className="flex items-center gap-2 rounded-panel border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">
              <AlertCircle size={13} className="shrink-0" />
              {error}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="rounded-panel border border-border px-4 py-1.5 text-sm text-muted transition hover:bg-surface-alt hover:text-ink disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
            >
              {saving ? 'Adding…' : 'Add Vehicle'}
            </button>
          </div>
        </form>
      </div>
    </>
  )
}

export function VehiclesPage() {
  const { theme, isAuthenticated } = useAppContext()
  const navigate = useNavigate()
  const [vehicles, setVehicles] = useState([])
  const [pollStatus, setPollStatus] = useState('idle')
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null)
  const [error, setError] = useState(null)
  const [typeFilter, setTypeFilter] = useState('all')
  const [showAddModal, setShowAddModal] = useState(false)
  const pollRef = useRef(null)
  const retryCountRef = useRef(0)

  const fetchVehicles = async () => {
    if (document.visibilityState === 'hidden') return
    setPollStatus('loading')
    try {
      const response = await gatewayClient.getVehicles('?size=200&sort=id,asc')
      const content = Array.isArray(response?.content) ? response.content : Array.isArray(response) ? response : []
      setVehicles(content)
      setPollStatus('ok')
      setLastUpdatedAt(Date.now())
      setError(null)
      retryCountRef.current = 0
    } catch (err) {
      retryCountRef.current += 1
      setPollStatus('error')
      setError(err.message || 'Failed to load vehicles')
    }
  }

  useEffect(() => {
    fetchVehicles()
    pollRef.current = window.setInterval(fetchVehicles, POLL_INTERVAL_MS)
    return () => window.clearInterval(pollRef.current)
  }, [])

  const filteredVehicles = useMemo(() => {
    if (typeFilter === 'all') return vehicles
    const mapped = { bus: 'BUS', tram: 'TRAM', trolleybus: 'TROLLEY', minibus: 'MINIBUS' }
    const enumVal = mapped[typeFilter]
    return vehicles.filter((v) => v.type === enumVal)
  }, [vehicles, typeFilter])

  const mapVehicles = useMemo(
    () => filteredVehicles.map(normalizeFleetVehicle).filter(Boolean),
    [filteredVehicles],
  )

  const operationalCount = vehicles.filter((v) => v.status === 'OPERATIONAL').length

  function handleVehicleCreated(vehicle) {
    setShowAddModal(false)
    navigate(`/vehicles/${vehicle.id}`)
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-ink">
            <Truck size={18} />
            Fleet
          </h2>
          <p className="text-xs text-muted">
            {vehicles.length} vehicle{vehicles.length !== 1 ? 's' : ''} · {operationalCount} operational
          </p>
        </div>
        <div className="flex items-center gap-2">
          {isAuthenticated && (
            <button
              type="button"
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-1.5 rounded-panel border border-accent bg-accent px-3 py-1.5 text-xs font-medium text-white transition hover:opacity-90"
            >
              <Plus size={13} />
              Add Vehicle
            </button>
          )}
          <PollingStatusBadge status={pollStatus} lastUpdatedAt={lastUpdatedAt} theme={theme} />
          <button
            type="button"
            onClick={fetchVehicles}
            className="rounded-panel border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink"
            aria-label="Refresh"
          >
            <RefreshCw size={14} />
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-1.5">
        {TYPE_FILTERS.map((f) => {
          const typeInfo = f !== 'all' ? Object.values(VS_TYPE_MAP).find((t) => t.key === f) : null
          return (
            <button
              key={f}
              type="button"
              onClick={() => setTypeFilter(f)}
              className={`flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition ${
                typeFilter === f
                  ? 'border-accent bg-accent text-white'
                  : 'border-border text-muted hover:bg-surface-alt hover:text-ink'
              }`}
            >
              {typeInfo && (
                <span
                  className="inline-block h-2 w-2 rounded-full"
                  style={{ backgroundColor: typeInfo.color }}
                />
              )}
              {f === 'all' ? 'All types' : typeInfo?.label || f}
            </button>
          )
        })}
      </div>

      {error && pollStatus !== 'ok' && (
        <div className="flex items-center gap-2 rounded-panel border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">
          <AlertCircle size={14} className="shrink-0" />
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[340px_1fr]">
        <div className="flex flex-col gap-1.5 lg:max-h-[calc(100vh-220px)] lg:overflow-y-auto">
          {filteredVehicles.length === 0 && pollStatus !== 'loading' && (
            <p className="py-8 text-center text-sm text-muted">
              {typeFilter === 'all' ? 'No vehicles in fleet yet.' : `No ${typeFilter} vehicles.`}
            </p>
          )}
          {filteredVehicles.map((v) => (
            <VehicleRow
              key={v.id}
              vehicle={v}
              onClick={() => navigate(`/vehicles/${v.id}`)}
            />
          ))}
        </div>

        <TransitMap
          vehicles={mapVehicles}
          className="h-[480px] rounded-panel lg:h-[calc(100vh-220px)]"
        />
      </div>

      {showAddModal && (
        <AddVehicleModal
          onClose={() => setShowAddModal(false)}
          onCreated={handleVehicleCreated}
        />
      )}
    </div>
  )
}
