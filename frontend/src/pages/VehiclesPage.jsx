import { AlertCircle, RefreshCw, Truck } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { TransitMap } from '../components/map/TransitMap'
import { useAppContext } from '../context/AppContext'
import { gatewayClient } from '../services/gatewayClient'

const POLL_INTERVAL_MS = 5000

const VS_TYPE_MAP = {
  BUS:     { typeId: 2, key: 'bus',        label: 'Bus',        color: '#3b82f6' },
  TRAM:    { typeId: 4, key: 'tram',       label: 'Tram',       color: '#e63946' },
  TROLLEY: { typeId: 3, key: 'trolleybus', label: 'Trolleybus', color: '#10b981' },
  MINIBUS: { typeId: 1, key: 'minibus',    label: 'Minibus',    color: '#f97316' },
}

const STATUS_STYLES = {
  OPERATIONAL:    'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  IN_MAINTENANCE: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  OUT_OF_SERVICE: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  RETIRED:        'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400',
}

const STATUS_LABELS = {
  OPERATIONAL:    'Operational',
  IN_MAINTENANCE: 'Maintenance',
  OUT_OF_SERVICE: 'Out of service',
  RETIRED:        'Retired',
}

const TYPE_FILTERS = ['all', 'bus', 'tram', 'trolleybus', 'minibus']

function normalizeFleetVehicle(v) {
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

function VehicleRow({ vehicle, isSelected, onClick }) {
  const typeInfo = VS_TYPE_MAP[vehicle.type] || VS_TYPE_MAP.BUS
  const statusStyle = STATUS_STYLES[vehicle.status] || STATUS_STYLES.OPERATIONAL
  const statusLabel = STATUS_LABELS[vehicle.status] || vehicle.status
  const hasGps = vehicle.lastLat != null && vehicle.lastLon != null

  return (
    <button
      type="button"
      onClick={onClick}
      className={`w-full rounded-panel border px-3 py-2.5 text-left transition ${
        isSelected
          ? 'border-accent bg-surface-alt'
          : 'border-border hover:bg-surface-alt hover:border-border'
      }`}
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

export function VehiclesPage() {
  const { theme } = useAppContext()
  const [vehicles, setVehicles] = useState([])
  const [pollStatus, setPollStatus] = useState('idle')
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null)
  const [error, setError] = useState(null)
  const [typeFilter, setTypeFilter] = useState('all')
  const [selectedId, setSelectedId] = useState(null)
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
    const key = typeFilter.toUpperCase()
    const mapped = { bus: 'BUS', tram: 'TRAM', trolleybus: 'TROLLEY', minibus: 'MINIBUS' }
    const enumVal = mapped[typeFilter] || key
    return vehicles.filter((v) => v.type === enumVal)
  }, [vehicles, typeFilter])

  const mapVehicles = useMemo(
    () => filteredVehicles.map(normalizeFleetVehicle).filter(Boolean),
    [filteredVehicles],
  )

  const selectedVehicle = useMemo(
    () => (selectedId != null ? vehicles.find((v) => v.id === selectedId) : null),
    [vehicles, selectedId],
  )

  const focusPositions = useMemo(() => {
    if (!selectedVehicle || selectedVehicle.lastLat == null) return []
    return [[selectedVehicle.lastLat, selectedVehicle.lastLon]]
  }, [selectedVehicle])

  const operationalCount = vehicles.filter((v) => v.status === 'OPERATIONAL').length

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold text-ink flex items-center gap-2">
            <Truck size={18} />
            Fleet
          </h2>
          <p className="text-xs text-muted">
            {vehicles.length} vehicle{vehicles.length !== 1 ? 's' : ''} · {operationalCount} operational
          </p>
        </div>
        <div className="flex items-center gap-2">
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
              isSelected={selectedId === v.id}
              onClick={() => setSelectedId((prev) => (prev === v.id ? null : v.id))}
            />
          ))}
        </div>

        <TransitMap
          vehicles={mapVehicles}
          focusPositions={focusPositions}
          focusKey={selectedId}
          className="h-[480px] rounded-panel lg:h-[calc(100vh-220px)]"
        />
      </div>
    </div>
  )
}
