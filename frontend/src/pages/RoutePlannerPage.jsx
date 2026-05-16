import { ArrowLeft, Loader2, MapPin, Route, Search } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { TransitMap } from '../components/map/TransitMap'
import { sarajevoCenter } from '../data/mockTransitData'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'
import { VEHICLE_TYPE_META_BY_ID, getColorForLegMode, withAlpha } from '../constants/vehicleColors'
import { formatDepartureFromTimestamp, formatDurationFromSeconds } from '../utils/formatters'

const POLL_INTERVAL_MS = 5000
const STATIONARY_TIMEOUT_MS = 60 * 1000
const MOVEMENT_THRESHOLD_METERS = 3

const VEHICLE_TYPE_META = VEHICLE_TYPE_META_BY_ID

function getCoordsFromStop(stop) {
  if (!stop) return null
  return { lat: stop.latitude, lon: stop.longitude }
}

function toRadians(value) {
  return (value * Math.PI) / 180
}

function distanceMeters(fromLat, fromLon, toLat, toLon) {
  const earthRadius = 6371000
  const lat1 = toRadians(fromLat)
  const lat2 = toRadians(toLat)
  const deltaLat = toRadians(toLat - fromLat)
  const deltaLon = toRadians(toLon - fromLon)
  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return earthRadius * c
}

function headingFromPositions(previous, next) {
  const deltaLat = next.latitude - previous.latitude
  const deltaLon = next.longitude - previous.longitude
  const headingDegrees = (Math.atan2(deltaLon, deltaLat) * 180) / Math.PI
  return (headingDegrees + 360) % 360
}

function PollingStatusBadge({ status, lastUpdatedAt, theme }) {
  const [now, setNow] = useState(Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNow(Date.now())
    }, 250)
    return () => window.clearInterval(timer)
  }, [])

  const elapsedMs = lastUpdatedAt ? Math.max(0, now - lastUpdatedAt) : 0
  const progress = Math.min(1, elapsedMs / POLL_INTERVAL_MS)
  const progressDeg = Math.round(progress * 360)

  const fillColor = theme === 'dark' ? '#cbd5e1' : '#334155'

  let text = 'Idle'

  if (status === 'loading') {
    text = 'Updating'
  } else if (status === 'ok') {
    text = `Updated ${Math.floor(elapsedMs / 1000)}s`
  } else if (status === 'error') {
    text = 'Retrying'
  }

  const fillDeg = status === 'error' ? 360 : progressDeg
  const ringFill = `conic-gradient(${fillColor} 0deg ${fillDeg}deg, transparent ${fillDeg}deg 360deg)`

  return (
    <span
      className="pointer-events-auto inline-flex h-5 w-5 rounded-full"
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

export function RoutePlannerPage() {
  const { addTripHistoryItem, theme } = useAppContext()
  const [stops, setStops] = useState([])
  const [fromQuery, setFromQuery] = useState('')
  const [toQuery, setToQuery] = useState('')
  const [fromStop, setFromStop] = useState(null)
  const [toStop, setToStop] = useState(null)
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [routeMode, setRouteMode] = useState(false)
  const [selectedItineraryIndex, setSelectedItineraryIndex] = useState(0)
  const [selectedDetailIndex, setSelectedDetailIndex] = useState(null)
  const [selectedLegIndex, setSelectedLegIndex] = useState(null)
  const [pickingMode, setPickingMode] = useState('none')
  const [activeVehicleTypes, setActiveVehicleTypes] = useState([1, 2, 3, 4])
  const [vehicles, setVehicles] = useState([])
  const [pollingStatus, setPollingStatus] = useState('idle')
  const [lastVehiclesUpdatedAt, setLastVehiclesUpdatedAt] = useState(null)

  const pollTimerRef = useRef(null)
  const vehicleMovementRef = useRef({})

  useEffect(() => {
    transitApi.getStops().then(setStops)
  }, [])

  const fromMatches = useMemo(
    () => stops.filter((stop) => stop.name.toLowerCase().includes(fromQuery.toLowerCase())).slice(0, 6),
    [fromQuery, stops],
  )
  const toMatches = useMemo(
    () => stops.filter((stop) => stop.name.toLowerCase().includes(toQuery.toLowerCase())).slice(0, 6),
    [stops, toQuery],
  )

  const primary = results[selectedItineraryIndex]
  const displayItinerary = selectedDetailIndex !== null ? results[selectedDetailIndex] : primary

  const coloredPolylines = useMemo(() => {
    if (!displayItinerary?.legs) return []
    return displayItinerary.legs.map((leg, legIndex) => {
      const color = getColorForLegMode(leg.mode)
      const dimmed = selectedLegIndex !== null && legIndex !== selectedLegIndex
      return {
        positions: leg.path || [],
        color: dimmed ? '#94a3b8' : color,
        opacity: dimmed ? 0.32 : 0.92,
        weight: dimmed ? 4 : 7,
        casingOpacity: dimmed ? 0.2 : 0.78,
        legIndex,
        label: `${leg.mode}: ${leg.fromName} -> ${leg.toName} (${Math.round(leg.distanceMeters / 100) / 10} km)`,
      }
    })
  }, [displayItinerary, selectedLegIndex])

  const legStops = useMemo(() => {
    if (!displayItinerary?.legs) return []
    const stopsSet = new Set()
    const collected = []

    displayItinerary.legs.forEach((leg, index) => {
      const legColor = getColorForLegMode(leg.mode)
      const dimmed = selectedLegIndex !== null && index !== selectedLegIndex
      if (!stopsSet.has(leg.fromName)) {
        stopsSet.add(leg.fromName)
        const firstPoint = leg.path?.[0]
        collected.push({
          id: `stop-from-${index}`,
          name: leg.fromName,
          latitude: firstPoint ? firstPoint[0] : sarajevoCenter[0],
          longitude: firstPoint ? firstPoint[1] : sarajevoCenter[1],
          color: dimmed ? '#94a3b8' : legColor,
        })
      }
      if (!stopsSet.has(leg.toName)) {
        stopsSet.add(leg.toName)
        const lastPoint = leg.path?.[leg.path.length - 1]
        collected.push({
          id: `stop-to-${index}`,
          name: leg.toName,
          latitude: lastPoint ? lastPoint[0] : sarajevoCenter[0],
          longitude: lastPoint ? lastPoint[1] : sarajevoCenter[1],
          color: dimmed ? '#94a3b8' : legColor,
        })
      }
    })
    return collected
  }, [displayItinerary, selectedLegIndex])

  const focusPositions = useMemo(() => {
    if (!displayItinerary?.legs?.length) return []
    if (selectedLegIndex !== null) {
      return displayItinerary.legs[selectedLegIndex]?.path || []
    }
    return displayItinerary.legs.flatMap((leg) => leg.path || [])
  }, [displayItinerary, selectedLegIndex])

  const focusKey = useMemo(() => {
    if (!displayItinerary?.legs?.length) return 'none'
    const routeKey = selectedDetailIndex !== null ? `detail-${selectedDetailIndex}` : `itinerary-${selectedItineraryIndex}`
    const legKey = selectedLegIndex !== null ? `leg-${selectedLegIndex}` : 'full'
    return `${routeKey}-${legKey}`
  }, [displayItinerary, selectedDetailIndex, selectedItineraryIndex, selectedLegIndex])

  const planningMode = routeMode || loading

  const fetchVehicles = async () => {
    if (activeVehicleTypes.length === 0) {
      setVehicles([])
      setPollingStatus('idle')
      return
    }

    setPollingStatus('loading')
    try {
      const next = await transitApi.getVehiclePositions(activeVehicleTypes)
      const now = Date.now()
      const previousById = vehicleMovementRef.current
      const nextById = {}

      const enriched = next.map((vehicle) => {
        const previous = previousById[vehicle.id]
        const isFirstFetch = !previous
        const typeMeta = VEHICLE_TYPE_META[vehicle.typeId]

        let headingDeg = previous?.headingDeg ?? 0
        let lastMovedAt = previous?.lastMovedAt ?? now
        let hasMoved = previous?.hasMoved ?? false

        if (previous) {
          const movedDistance = distanceMeters(
            previous.latitude,
            previous.longitude,
            vehicle.latitude,
            vehicle.longitude,
          )

          if (movedDistance >= MOVEMENT_THRESHOLD_METERS) {
            headingDeg = headingFromPositions(previous, vehicle)
            lastMovedAt = now
            hasMoved = true
          }
        }

        const isStationary = isFirstFetch || !hasMoved || now - lastMovedAt >= STATIONARY_TIMEOUT_MS
        const normalized = {
          ...vehicle,
          color: typeMeta?.color || vehicle.color || '#6366f1',
          typeLabel: typeMeta?.label || vehicle.typeLabel || 'Transit',
          headingDeg,
          isFirstFetch,
          isMoving: !isStationary,
        }

        nextById[vehicle.id] = {
          latitude: vehicle.latitude,
          longitude: vehicle.longitude,
          headingDeg,
          lastMovedAt,
          hasMoved,
          seenAt: now,
        }

        return normalized
      })

      vehicleMovementRef.current = nextById
      setVehicles(enriched)
      setLastVehiclesUpdatedAt(Date.now())
      setPollingStatus('ok')
    } catch {
      setPollingStatus('error')
    }
  }

  useEffect(() => {
    fetchVehicles()
    if (pollTimerRef.current) window.clearInterval(pollTimerRef.current)

    pollTimerRef.current = window.setInterval(() => {
      if (document.visibilityState !== 'visible') return
      fetchVehicles()
    }, POLL_INTERVAL_MS)

    return () => {
      if (pollTimerRef.current) window.clearInterval(pollTimerRef.current)
    }
  }, [activeVehicleTypes])

  const onPlanRoute = async () => {
    if (!fromStop || !toStop) return

    const fromCoords = getCoordsFromStop(fromStop)
    const toCoords = getCoordsFromStop(toStop)
    if (!fromCoords || !toCoords) return

    setRouteMode(true)
    setLoading(true)
    try {
      const response = await transitApi.getOptimalRoute({
        fromLat: fromCoords.lat,
        fromLon: fromCoords.lon,
        toLat: toCoords.lat,
        toLon: toCoords.lon,
        numItineraries: 5,
      })
      setResults(response.itineraries || [])
      setSelectedItineraryIndex(0)
      setSelectedDetailIndex(null)
      setSelectedLegIndex(null)

      if (response.itineraries?.[0]) {
        addTripHistoryItem({
          id: Date.now(),
          fromStop: fromStop.name,
          toStop: toStop.name,
          durationMinutes: Math.round((response.itineraries[0].durationSeconds || 0) / 60),
          lineCode: response.itineraries[0].legs.find((leg) => leg.mode !== 'WALK')?.mode || 'Mixed',
          traveledAt: new Date().toISOString(),
        })
      }
    } finally {
      setLoading(false)
    }
  }

  const exitRouteMode = () => {
    setRouteMode(false)
    setLoading(false)
    setResults([])
    setSelectedItineraryIndex(0)
    setSelectedDetailIndex(null)
    setSelectedLegIndex(null)
  }

  const handleMapClick = (latlng) => {
    if (pickingMode === 'from') {
      const name = `Map point (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`
      setFromStop({
        id: 'custom-from',
        name,
        latitude: latlng.lat,
        longitude: latlng.lng,
      })
      setFromQuery(name)
      setPickingMode('none')
    } else if (pickingMode === 'to') {
      const name = `Map point (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`
      setToStop({
        id: 'custom-to',
        name,
        latitude: latlng.lat,
        longitude: latlng.lng,
      })
      setToQuery(name)
      setPickingMode('none')
    }
  }

  const toggleVehicleType = (typeId) => {
    setActiveVehicleTypes((current) =>
      current.includes(typeId) ? current.filter((id) => id !== typeId) : [...current, typeId].sort(),
    )
  }

  return (
    <div className="relative">
      <div className={`grid gap-4 transition-all duration-300 ${planningMode ? 'lg:grid-cols-[minmax(260px,25%)_1fr]' : 'grid-cols-1'}`}>
        {planningMode ? (
          <aside className="order-2 lg:order-1 rounded-panel border border-border bg-surface p-4 lg:max-h-[calc(100vh-8rem)] lg:overflow-auto">
            <button
              type="button"
              onClick={exitRouteMode}
              className="mb-3 inline-flex items-center gap-2 rounded-panel border border-border bg-surface-soft px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
            >
              <ArrowLeft size={14} />
              Back to live map
            </button>

            {loading ? (
              <div className="flex h-48 items-center justify-center gap-2 text-sm text-muted">
                <Loader2 size={16} className="animate-spin" />
                Finding routes...
              </div>
            ) : selectedDetailIndex !== null && results[selectedDetailIndex] ? (
              <div className="space-y-3">
                <button
                  type="button"
                  onClick={() => {
                    setSelectedDetailIndex(null)
                    setSelectedLegIndex(null)
                  }}
                  className="inline-flex items-center gap-2 rounded-panel border border-border bg-surface-soft px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
                >
                  <ArrowLeft size={14} />
                  Back to routes
                </button>

                <h3 className="text-base font-semibold text-ink">Route details</h3>
                <div className="grid gap-2">
                  {results[selectedDetailIndex].legs?.map((leg, idx) => (
                    <button
                      key={`${leg.mode}-${idx}`}
                      type="button"
                      onClick={() => setSelectedLegIndex(idx)}
                      className="w-full rounded-panel border p-3 text-left text-sm transition"
                      style={{
                        borderColor:
                          selectedLegIndex === idx
                            ? getColorForLegMode(leg.mode)
                            : withAlpha(getColorForLegMode(leg.mode), 0.45),
                        backgroundColor:
                          selectedLegIndex === idx
                            ? withAlpha(getColorForLegMode(leg.mode), theme === 'dark' ? 0.24 : 0.16)
                            : withAlpha(getColorForLegMode(leg.mode), theme === 'dark' ? 0.18 : 0.1),
                      }}
                    >
                      <p className="font-semibold" style={{ color: getColorForLegMode(leg.mode) }}>{leg.mode}</p>
                      <p className="mt-1 text-muted">
                        {leg.fromName} {'->'} {leg.toName}
                      </p>
                      <p className="mt-1 text-xs text-muted">Distance: {Math.round(leg.distanceMeters)} m</p>
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="space-y-3">
                <h3 className="text-base font-semibold text-ink">Found routes</h3>
                {results.length === 0 ? (
                  <p className="text-sm text-muted">No routes found yet.</p>
                ) : (
                  results.map((itinerary, index) => (
                    (() => {
                      const primaryLeg = itinerary.legs?.find((leg) => String(leg.mode || '').toUpperCase() !== 'WALK') || itinerary.legs?.[0]
                      const cardColor = getColorForLegMode(primaryLeg?.mode)
                      return (
                    <button
                      key={index}
                      type="button"
                      onClick={() => {
                        setSelectedItineraryIndex(index)
                        setSelectedDetailIndex(index)
                        setSelectedLegIndex(null)
                      }}
                      className={`w-full rounded-panel border p-3 text-left transition ${
                        selectedItineraryIndex === index
                          ? 'border-accent bg-accent/5 ring-1 ring-accent'
                          : 'border-border bg-surface-soft hover:bg-surface-alt'
                      }`}
                      style={{
                        borderColor:
                          selectedItineraryIndex === index
                            ? undefined
                            : withAlpha(cardColor, theme === 'dark' ? 0.42 : 0.38),
                        backgroundColor:
                          selectedItineraryIndex === index
                            ? undefined
                            : withAlpha(cardColor, theme === 'dark' ? 0.14 : 0.08),
                      }}
                    >
                      <div className="flex flex-wrap gap-3 text-sm">
                        <span className="font-semibold text-ink">{formatDurationFromSeconds(itinerary.durationSeconds)}</span>
                        <span className="text-muted">Transfers: {itinerary.transfers}</span>
                      </div>
                      <p className="mt-1 text-xs text-muted">
                        Departure: {formatDepartureFromTimestamp(itinerary.legs?.[0]?.startTime)}
                      </p>
                      <ol className="mt-2 space-y-1">
                        {itinerary.legs?.map((leg, legIndex) => (
                          <li key={`${index}-${legIndex}`} className="flex items-center gap-2 text-xs text-ink">
                            <span
                              className="h-2 w-2 rounded-full"
                              style={{ backgroundColor: getColorForLegMode(leg.mode) }}
                            />
                            <span className="font-semibold" style={{ color: getColorForLegMode(leg.mode) }}>
                              {leg.mode}
                            </span>
                            <span className="text-muted">{leg.fromName} {'->'} {leg.toName}</span>
                          </li>
                        ))}
                      </ol>
                    </button>
                      )
                    })()
                  ))
                )}
              </div>
            )}
          </aside>
        ) : null}

        <section className={`order-1 relative overflow-visible rounded-panel border border-border bg-surface ${planningMode ? 'lg:order-2' : ''}`}>
          {!planningMode ? (
            <>
              <div className="pointer-events-none absolute right-3 top-3 z-[999]">
                <PollingStatusBadge status={pollingStatus} lastUpdatedAt={lastVehiclesUpdatedAt} theme={theme} />
              </div>
              <div className="pointer-events-auto absolute bottom-3 left-3 z-[999] flex flex-wrap items-center gap-2 rounded-full border border-border bg-surface/90 px-2 py-1 backdrop-blur">
                {Object.entries(VEHICLE_TYPE_META).map(([rawId, meta]) => {
                  const typeId = Number(rawId)
                  const active = activeVehicleTypes.includes(typeId)
                  return (
                    <button
                      key={typeId}
                      type="button"
                      onClick={() => toggleVehicleType(typeId)}
                      className={`rounded-full border px-2 py-1 text-[11px] font-semibold transition ${
                        active
                          ? 'border-transparent text-white'
                          : 'border-border bg-surface text-muted hover:bg-surface-alt hover:text-ink'
                      }`}
                      style={active ? { backgroundColor: meta.color } : undefined}
                    >
                      {meta.label}
                    </button>
                  )
                })}
              </div>
            </>
          ) : null}

          <TransitMap
            className={`h-[calc(100vh-9rem)] min-h-[520px] ${planningMode ? 'lg:h-[calc(100vh-8rem)]' : ''}`}
            stops={legStops}
            vehicles={planningMode ? [] : vehicles}
            polylines={coloredPolylines}
            focusPositions={focusPositions}
            focusKey={focusKey}
            onLegPathClick={(legIndex) => setSelectedLegIndex(legIndex)}
            startPin={fromStop ? [fromStop.latitude, fromStop.longitude] : null}
            endPin={toStop ? [toStop.latitude, toStop.longitude] : null}
            onMapClick={handleMapClick}
          />

          <div className="absolute left-1/2 top-0 z-[1000] w-[min(680px,94vw)] max-h-[72vh] -translate-x-1/2 -translate-y-1/2 overflow-visible rounded-[18px] border border-border bg-surface/95 p-3 shadow-xl backdrop-blur sm:p-4 md:max-h-none">
            <div className="mb-3 flex items-center gap-2 text-ink">
              <Route size={18} />
              <h2 className="text-base font-semibold sm:text-lg">Plan your trip</h2>
            </div>

            <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
              <div className="relative">
                <label htmlFor="planner-from" className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">
                  From
                </label>
                <div className="mt-1 flex gap-2">
                  <input
                    id="planner-from"
                    value={fromQuery}
                    onChange={(event) => {
                      setFromQuery(event.target.value)
                      setFromStop(null)
                    }}
                    placeholder="Start stop"
                    className="w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setPickingMode(pickingMode === 'from' ? 'none' : 'from')}
                    title="Pick from map"
                    className={`flex items-center justify-center rounded-panel border px-3 transition ${
                      pickingMode === 'from'
                        ? 'border-accent bg-accent text-white'
                        : 'border-border bg-surface text-muted hover:bg-surface-alt hover:text-ink'
                    }`}
                  >
                    <MapPin size={16} />
                  </button>
                </div>
                {fromQuery && !fromStop ? (
                  <div className="absolute left-0 right-0 top-full z-20 mt-2 grid max-h-48 gap-1 overflow-auto rounded-panel border border-border bg-surface p-2 shadow-lg">
                    {fromMatches.map((stop) => (
                      <button
                        key={stop.id}
                        type="button"
                        onClick={() => {
                          setFromStop(stop)
                          setFromQuery(stop.name)
                        }}
                        className="rounded-panel px-2 py-1 text-left text-sm text-ink transition hover:bg-surface-alt"
                      >
                        {stop.name}
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>

              <div className="relative">
                <label htmlFor="planner-to" className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">
                  To
                </label>
                <div className="mt-1 flex gap-2">
                  <input
                    id="planner-to"
                    value={toQuery}
                    onChange={(event) => {
                      setToQuery(event.target.value)
                      setToStop(null)
                    }}
                    placeholder="Destination stop"
                    className="w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setPickingMode(pickingMode === 'to' ? 'none' : 'to')}
                    title="Pick from map"
                    className={`flex items-center justify-center rounded-panel border px-3 transition ${
                      pickingMode === 'to'
                        ? 'border-accent bg-accent text-white'
                        : 'border-border bg-surface text-muted hover:bg-surface-alt hover:text-ink'
                    }`}
                  >
                    <MapPin size={16} />
                  </button>
                </div>
                {toQuery && !toStop ? (
                  <div className="absolute left-0 right-0 top-full z-20 mt-2 grid max-h-48 gap-1 overflow-auto rounded-panel border border-border bg-surface p-2 shadow-lg">
                    {toMatches.map((stop) => (
                      <button
                        key={stop.id}
                        type="button"
                        onClick={() => {
                          setToStop(stop)
                          setToQuery(stop.name)
                        }}
                        className="rounded-panel px-2 py-1 text-left text-sm text-ink transition hover:bg-surface-alt"
                      >
                        {stop.name}
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>

              <button
                type="button"
                onClick={onPlanRoute}
                disabled={!fromStop || !toStop || loading}
                className="mt-2 inline-flex h-fit items-center justify-center gap-2 rounded-panel border border-accent bg-accent px-4 py-2 text-sm font-semibold text-white transition md:mt-6 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <Search size={15} />
                {loading ? 'Planning...' : 'Find route'}
              </button>
            </div>

          </div>
        </section>
      </div>
    </div>
  )
}
