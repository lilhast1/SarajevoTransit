import { AlertCircle, RefreshCw, Truck } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { TransitMap } from '../components/map/TransitMap'
import { useAppContext } from '../context/AppContext'
import { gatewayClient } from '../services/gatewayClient'
import { transitApi } from '../services/transitApi'

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

export function normalizeFleetVehicle(v, jpPositions = []) {
  const t = VS_TYPE_MAP[v.type] || VS_TYPE_MAP.BUS

  let lat = v.lastLat
  let lon = v.lastLon
  let isMapped = false

  if (jpPositions.length > 0) {
    // 1. Exact 1:1 match by internalId → jp.id (GRAS fleet number)
    let jpMatch = v.internalId
      ? jpPositions.find((p) => String(p.id).trim() === String(v.internalId).trim())
      : null
    // 2. Fallback: first vehicle on same line
    if (!jpMatch && v.assignedLineCode) {
      jpMatch = jpPositions.find(
        (p) => String(p.lineCode).trim() === String(v.assignedLineCode).trim()
      )
    }
    if (jpMatch) {
      lat = jpMatch.latitude
      lon = jpMatch.longitude
      isMapped = true
    }
  }

  if (lat == null || lon == null) return null

  return {
    id: v.id,
    lineCode: v.assignedLineCode || v.internalId || `#${v.id}`,
    name: `${t.label} ${v.registrationNumber}`,
    latitude: lat,
    longitude: lon,
    typeId: t.typeId,
    type: t.key,
    typeLabel: t.label,
    color: t.color,
    isMapped,
  }
}

function PollingStatusBadge({ status, lastUpdatedAt, theme }) {
  const { t } = useTranslation('vehicles')
  const [now, setNow] = useState(Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 250)
    return () => window.clearInterval(timer)
  }, [])

  const elapsedMs = lastUpdatedAt ? Math.max(0, now - lastUpdatedAt) : 0
  const progress = Math.min(1, elapsedMs / POLL_INTERVAL_MS)
  const progressDeg = Math.round(progress * 360)
  const fillColor = theme === 'dark' ? '#cbd5e1' : '#334155'

  let text = t('status_idle')
  if (status === 'loading') text = t('status_updating')
  else if (status === 'ok') text = t('status_updated', { seconds: Math.floor(elapsedMs / 1000) })
  else if (status === 'error') text = t('status_retrying')

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

export const STATUS_I18N = {
  OPERATIONAL: 'status_operational',
  IN_MAINTENANCE: 'status_maintenance',
  OUT_OF_SERVICE: 'status_out_of_service',
  RETIRED: 'status_retired',
}

function VehicleRow({ vehicle, onClick, isSelected }) {
  const { t } = useTranslation('vehicles')
  const typeInfo = VS_TYPE_MAP[vehicle.type] || VS_TYPE_MAP.BUS
  const statusStyle = STATUS_STYLES[vehicle.status] || STATUS_STYLES.OPERATIONAL
  const statusLabel = t(STATUS_I18N[vehicle.status] || 'status_operational')
  const hasGps = vehicle.lastLat != null && vehicle.lastLon != null

  return (
    <button
      type="button"
      onClick={onClick}
      className={`w-full rounded-panel border px-3 py-2.5 text-left transition hover:bg-surface-alt hover:border-accent/50 ${
        isSelected ? 'border-accent bg-accent/5' : 'border-border'
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
            {typeInfo.label}{vehicle.internalId ? ` · #${vehicle.internalId}` : ''}{vehicle.capacity ? ` · ${vehicle.capacity} cap` : ''}{vehicle.assignedLineCode ? ` · Line ${vehicle.assignedLineCode}` : ''}
          </span>
        </span>
        <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${statusStyle}`}>
          {statusLabel}
        </span>
      </div>
      {!hasGps && (
        <p className="mt-1 text-[10px] text-muted italic">{t('no_gps')}</p>
      )}
    </button>
  )
}

export function VehiclesPage() {
  const { t } = useTranslation('vehicles')
  const { theme } = useAppContext()
  const [vehicles, setVehicles] = useState([])
  const [jpPositions, setJpPositions] = useState([])
  const [pollStatus, setPollStatus] = useState('idle')
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null)
  const [error, setError] = useState(null)
  const [typeFilter, setTypeFilter] = useState('all')
  const [selectedVehicleId, setSelectedVehicleId] = useState(null)
  const [showUnmapped, setShowUnmapped] = useState(false)
  const pollRef = useRef(null)
  const retryCountRef = useRef(0)

  const fetchVehicles = async () => {
    if (document.visibilityState === 'hidden') return
    setPollStatus('loading')
    try {
      const [response, livePositions] = await Promise.allSettled([
        gatewayClient.getVehicles('?size=500&sort=id,asc'),
        transitApi.getVehiclePositions([1, 2, 3, 4]),
      ])
      const content = response.status === 'fulfilled'
        ? (Array.isArray(response.value?.content) ? response.value.content : Array.isArray(response.value) ? response.value : [])
        : []
      setVehicles(content)
      if (livePositions.status === 'fulfilled') setJpPositions(livePositions.value || [])
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

  const selectedVehicle = useMemo(
    () => (selectedVehicleId ? vehicles.find((v) => v.id === selectedVehicleId) : null),
    [vehicles, selectedVehicleId],
  )

  const mapVehicles = useMemo(() => {
    if (selectedVehicle) {
      const typeKey = VS_TYPE_MAP[selectedVehicle.type]?.key

      // 1. Exact 1:1 match by internalId → jp.id (set via the Line modal picker)
      if (selectedVehicle.internalId) {
        const exact = jpPositions.find(
          (p) => String(p.id).trim() === String(selectedVehicle.internalId).trim()
        )
        if (exact) return [exact]
      }
      // 2. Internal GPS position
      if (selectedVehicle.lastLat != null && selectedVehicle.lastLon != null) {
        return [{
          id: selectedVehicle.id,
          lineCode: selectedVehicle.assignedLineCode || `#${selectedVehicle.id}`,
          name: `${VS_TYPE_MAP[selectedVehicle.type]?.label || ''} ${selectedVehicle.registrationNumber}`,
          latitude: selectedVehicle.lastLat,
          longitude: selectedVehicle.lastLon,
          type: typeKey || 'bus',
          typeLabel: VS_TYPE_MAP[selectedVehicle.type]?.label || '',
          color: VS_TYPE_MAP[selectedVehicle.type]?.color || '#334155',
        }]
      }
      // 3. Vehicle assigned to a line → show only JP vehicles on that line
      if (selectedVehicle.assignedLineCode) {
        const norm = (c) => String(c ?? '').trim().toLowerCase().replace(/^0+/, '')
        return jpPositions.filter(
          (p) => norm(p.lineCode) === norm(selectedVehicle.assignedLineCode)
        )
      }
      // 4. Not assigned to any line → show all live vehicles of same type
      if (typeKey) return jpPositions.filter((p) => p.type === typeKey)
      return jpPositions
    }

    const base = typeFilter === 'all' ? jpPositions : jpPositions.filter((p) => p.type === typeFilter)

    if (showUnmapped) {
      const mappedJpIds = new Set(jpPositions.map((p) => String(p.id)))
      const unmapped = vehicles.filter(
        (v) => v.lastLat != null && (!v.internalId || !mappedJpIds.has(String(v.internalId)))
      )
      const unmappedPins = unmapped.map((v) => ({
        id: `unmapped-${v.id}`,
        lineCode: v.assignedLineCode || `#${v.id}`,
        name: `${VS_TYPE_MAP[v.type]?.label || ''} ${v.registrationNumber}`,
        latitude: v.lastLat,
        longitude: v.lastLon,
        type: VS_TYPE_MAP[v.type]?.key || 'bus',
        typeLabel: VS_TYPE_MAP[v.type]?.label || '',
        color: '#94a3b8',
        isUnmapped: true,
      }))
      return [...base, ...unmappedPins]
    }

    return base
  }, [selectedVehicle, jpPositions, typeFilter, showUnmapped, vehicles])

  const focusPositions = useMemo(
    () => mapVehicles.map((p) => [p.latitude, p.longitude]),
    [mapVehicles],
  )

  const operationalCount = vehicles.filter((v) => v.status === 'OPERATIONAL').length

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-ink">
            <Truck size={18} aria-hidden="true" />
            {t('title')}
          </h2>
          <p className="text-xs text-muted">
            {t('fleet_count', { total: vehicles.length, operational: operationalCount })}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <PollingStatusBadge status={pollStatus} lastUpdatedAt={lastUpdatedAt} theme={theme} />
          <button
            type="button"
            onClick={fetchVehicles}
            className="rounded-panel border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink"
            aria-label={t('refresh')}
          >
            <RefreshCw size={14} aria-hidden="true" />
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
              {f === 'all' ? t('all_types') : t(`type_${f}`) || typeInfo?.label || f}
            </button>
          )
        })}
        <button
          type="button"
          onClick={() => setShowUnmapped((p) => !p)}
          className={`flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition ${
            showUnmapped
              ? 'border-slate-400 bg-slate-500 text-white'
              : 'border-border text-muted hover:bg-surface-alt hover:text-ink'
          }`}
        >
          <span className="inline-block h-2 w-2 rounded-full bg-slate-400" />
          Unmapped fleet
        </button>
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
              {typeFilter === 'all' ? t('empty') : t('empty_type', { type: typeFilter })}
            </p>
          )}
          {filteredVehicles.map((v) => (
            <VehicleRow
              key={v.id}
              vehicle={v}
              isSelected={v.id === selectedVehicleId}
              onClick={() => setSelectedVehicleId((prev) => prev === v.id ? null : v.id)}
            />
          ))}
        </div>

        <TransitMap
          vehicles={mapVehicles}
          focusPositions={focusPositions}
          focusKey={selectedVehicleId}
          className="h-[480px] rounded-panel lg:h-[calc(100vh-220px)]"
        />
      </div>
    </div>
  )
}
