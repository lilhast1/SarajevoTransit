import { ArrowLeft, ArrowUpDown, ChevronDown, Loader2, MapPin, Route, Search } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { TransitMap } from '../components/map/TransitMap'
import { ErrorAlert } from '../components/common/Alerts'
import { sarajevoCenter } from '../data/mockTransitData'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'
import { VEHICLE_TYPE_META_BY_ID, getColorForLegRouteType, withAlpha } from '../constants/vehicleColors'
import { formatDepartureFromTimestamp, formatDurationFromSeconds } from '../utils/formatters'

const POLL_INTERVAL_MS = 5000
const STATIONARY_TIMEOUT_MS = 60 * 1000
const MOVEMENT_THRESHOLD_METERS = 3

const VEHICLE_TYPE_META = VEHICLE_TYPE_META_BY_ID

function lightenColor(hex, amount) {
  const hexStr = String(hex || '').replace('#', '')
  if (!/^[0-9a-fA-F]{6}$/.test(hexStr)) return hex
  const channel = (start) => {
    const value = parseInt(hexStr.slice(start, start + 2), 16)
    const next = value + (255 - value) * amount
    return Math.max(0, Math.min(255, Math.round(next)))
  }
  const r = channel(0).toString(16).padStart(2, '0')
  const g = channel(2).toString(16).padStart(2, '0')
  const b = channel(4).toString(16).padStart(2, '0')
  return `#${r}${g}${b}`
}

function getLegDisplayLabel(leg) {
  const mode = String(leg?.mode || '').toUpperCase().trim()
  if (mode === 'WALK') return 'WALK'

  const lineCode = String(leg?.lineCode || '').trim()
  const lineName = String(leg?.lineName || '').trim()

  if (lineCode && lineName) return `${lineCode} - ${lineName}`
  if (lineCode) return lineCode
  if (lineName) return lineName
  if (mode) return mode
  return 'Transit'
}

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
  const { t } = useTranslation('routePlanner')
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

  let text = t('status_idle')

  if (status === 'loading') {
    text = t('status_updating')
  } else if (status === 'ok') {
    text = t('status_updated', { seconds: Math.floor(elapsedMs / 1000) })
  } else if (status === 'error') {
    text = t('status_retrying')
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
  const { t } = useTranslation('routePlanner')
  const navigate = useNavigate()
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
  const [expandedLegIndex, setExpandedLegIndex] = useState(null)
  const [highlightedVisitedStopId, setHighlightedVisitedStopId] = useState(null)
  const [pickingMode, setPickingMode] = useState('none')
  const [activeVehicleTypes, setActiveVehicleTypes] = useState([1, 2, 3, 4])
  const [vehicles, setVehicles] = useState([])
  const [pollingStatus, setPollingStatus] = useState('idle')
  const [lastVehiclesUpdatedAt, setLastVehiclesUpdatedAt] = useState(null)
  const [plannerError, setPlannerError] = useState(null)
  const [fromFocused, setFromFocused] = useState(false)
  const [toFocused, setToFocused] = useState(false)

  const pollTimerRef = useRef(null)
  const vehicleMovementRef = useRef({})

  useEffect(() => {
    let active = true

    const loadStops = async () => {
      try {
        setPlannerError(null)
        const response = await transitApi.getStops()
        if (active) setStops(response)
      } catch (err) {
        if (active) {
          setStops([])
          setPlannerError(err.message || t('load_failed'))
        }
      }
    }

    loadStops()

    return () => {
      active = false
    }
  }, [t])

  useEffect(() => {
    const saved = sessionStorage.getItem('routePlanner')
    if (!saved) return
    try {
      const state = JSON.parse(saved)
      if (state.routeMode && state.results?.length) {
        setFromStop(state.fromStop)
        setToStop(state.toStop)
        setFromQuery(state.fromQuery || '')
        setToQuery(state.toQuery || '')
        setResults(state.results)
        setRouteMode(true)
        setSelectedItineraryIndex(state.selectedItineraryIndex ?? 0)
        setSelectedDetailIndex(state.selectedDetailIndex ?? null)
        setSelectedLegIndex(state.selectedLegIndex ?? null)
        setExpandedLegIndex(state.expandedLegIndex ?? null)
      }
    } catch {
      sessionStorage.removeItem('routePlanner')
    }
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
      const color = getColorForLegRouteType(leg.routeType, leg.mode)
      const legLabel = getLegDisplayLabel(leg)
      const dimmed = selectedLegIndex !== null && legIndex !== selectedLegIndex
      return {
        positions: leg.path || [],
        color: dimmed ? '#94a3b8' : color,
        opacity: dimmed ? 0.32 : 0.92,
        weight: dimmed ? 4 : 7,
        casingOpacity: dimmed ? 0.2 : 0.78,
        legIndex,
        label: `${legLabel}: ${leg.fromName} -> ${leg.toName} (${Math.round(leg.distanceMeters / 100) / 10} km)`,
      }
    })
  }, [displayItinerary, selectedLegIndex])

  const legStops = useMemo(() => {
    if (!displayItinerary?.legs) return []
    const stopsSet = new Set()
    const collected = []

    displayItinerary.legs.forEach((leg, index) => {
      const legColor = getColorForLegRouteType(leg.routeType, leg.mode)
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

  const visitedMapStops = useMemo(() => {
    if (!displayItinerary?.legs) return []
    const stopByName = new Map(stops.map((s) => [s.name, s]))
    const seen = new Set()
    const result = []
    displayItinerary.legs.forEach((leg) => {
      if (!leg.visitedStops) return
      const legColor = getColorForLegRouteType(leg.routeType, leg.mode)
      leg.visitedStops.forEach((vs) => {
        if (seen.has(vs.name)) return
        seen.add(vs.name)
        const localStop = stopByName.get(vs.name)
        result.push({
          id: localStop?.id ?? vs.name,
          name: vs.name,
          latitude: localStop?.latitude ?? null,
          longitude: localStop?.longitude ?? null,
          color: legColor,
          hoverColor: lightenColor(legColor, 0.3),
        })
      })
    })
    return result
  }, [displayItinerary, stops])

  const allMapStops = useMemo(() => {
    const seen = new Set()
    return [...legStops, ...visitedMapStops].filter((stop) => {
      if (stop.latitude == null || stop.longitude == null) return false
      const key = stop.name
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
  }, [legStops, visitedMapStops])

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
    setPlannerError(null)
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
      setExpandedLegIndex(null)
      setHighlightedVisitedStopId(null)

      sessionStorage.setItem('routePlanner', JSON.stringify({
        fromStop,
        toStop,
        fromQuery,
        toQuery,
        results: response.itineraries,
        routeMode: true,
        selectedItineraryIndex: 0,
        selectedDetailIndex: null,
        selectedLegIndex: null,
        expandedLegIndex: null,
      }))

      if (response.itineraries?.[0]) {
        const primaryTransitLeg = response.itineraries[0].legs.find((leg) => String(leg.mode || '').toUpperCase() !== 'WALK')
        addTripHistoryItem({
          id: Date.now(),
          fromStop: fromStop.name,
          toStop: toStop.name,
          durationMinutes: Math.round((response.itineraries[0].durationSeconds || 0) / 60),
          lineCode: primaryTransitLeg?.lineCode || primaryTransitLeg?.lineName || primaryTransitLeg?.mode || 'Mixed',
          traveledAt: new Date().toISOString(),
        })
      }
    } catch (err) {
      setResults([])
      setSelectedItineraryIndex(0)
      setSelectedDetailIndex(null)
      setSelectedLegIndex(null)
      setExpandedLegIndex(null)
      setHighlightedVisitedStopId(null)
      setPlannerError(err.message || t('load_failed'))
    } finally {
      setLoading(false)
    }
  }

  const exitRouteMode = () => {
    sessionStorage.removeItem('routePlanner')
    setRouteMode(false)
    setLoading(false)
    setResults([])
    setSelectedItineraryIndex(0)
    setSelectedDetailIndex(null)
    setSelectedLegIndex(null)
    setExpandedLegIndex(null)
    setHighlightedVisitedStopId(null)
    setFromFocused(false)
    setToFocused(false)
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

  const handleShuffle = () => {
    const tempStop = fromStop
    const tempQuery = fromQuery
    setFromStop(toStop)
    setFromQuery(toQuery)
    setToStop(tempStop)
    setToQuery(tempQuery)
  }

  const handleUseCurrentLocation = (target) => {
    if (!navigator.geolocation) return
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords
        const stop = { id: 'current-location', name: 'Current Location', latitude, longitude }
        if (target === 'from') {
          setFromStop(stop)
          setFromQuery('Current Location')
        } else {
          setToStop(stop)
          setToQuery('Current Location')
        }
      },
      () => {},
    )
  }

  const toggleVehicleType = (typeId) => {
    setActiveVehicleTypes((current) =>
      current.includes(typeId) ? current.filter((id) => id !== typeId) : [...current, typeId].sort(),
    )
  }

  return (
    <div className="relative">
      <div className="grid gap-4 lg:grid-cols-[minmax(260px,25%)_1fr]">
        <aside className="order-2 lg:order-1 rounded-panel border border-border bg-surface p-4 lg:max-h-[calc(100vh-8rem)] lg:overflow-auto">
          <div className="flex items-center gap-2 text-ink mb-3">
            <Route size={18} />
            <h2 className="text-base font-semibold">{t('title')}</h2>
          </div>

          {plannerError ? <ErrorAlert error={plannerError} onDismiss={() => setPlannerError(null)} className="mb-3" /> : null}

          <div className="relative mb-2">
            <label htmlFor="planner-from" className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">{t('from')}</label>
            <input
              id="planner-from"
              value={fromQuery}
              onChange={(e) => { setFromQuery(e.target.value); setFromStop(null) }}
              onFocus={() => setFromFocused(true)}
              onBlur={() => setTimeout(() => setFromFocused(false), 200)}
              placeholder={t('from_placeholder')}
              className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
            />
            {(fromFocused || (fromQuery && !fromStop)) && !fromStop && (
              <div className="absolute left-0 right-0 top-full z-20 mt-2 grid max-h-56 gap-1 overflow-auto rounded-panel border border-border bg-surface p-2 shadow-lg">
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => handleUseCurrentLocation('from')}
                  className="flex items-center gap-2 rounded-panel px-2 py-1.5 text-left text-sm text-ink transition hover:bg-surface-alt"
                >
                  <span className="text-accent">📍</span>
                  Use current location
                </button>
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => { setPickingMode('from'); setFromFocused(false) }}
                  className="flex items-center gap-2 rounded-panel px-2 py-1.5 text-left text-sm text-ink transition hover:bg-surface-alt"
                >
                  <MapPin size={14} className="text-accent" />
                  {t('pick_from_map')}
                </button>
                {fromQuery && fromMatches.length > 0 && <div className="my-1 border-t border-border" />}
                {fromMatches.map((stop) => (
                  <button
                    key={stop.id}
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => { setFromStop(stop); setFromQuery(stop.name) }}
                    className="rounded-panel px-2 py-1.5 text-left text-sm text-ink transition hover:bg-surface-alt"
                  >
                    {stop.name}
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="flex items-end gap-2 mb-2">
            <div className="relative flex-1">
              <label htmlFor="planner-to" className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">{t('to')}</label>
              <input
                id="planner-to"
                value={toQuery}
                onChange={(e) => { setToQuery(e.target.value); setToStop(null) }}
                onFocus={() => setToFocused(true)}
                onBlur={() => setTimeout(() => setToFocused(false), 200)}
                placeholder={t('to_placeholder')}
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-accent focus:outline-none"
              />
              {(toFocused || (toQuery && !toStop)) && !toStop && (
                <div className="absolute left-0 right-0 top-full z-20 mt-2 grid max-h-56 gap-1 overflow-auto rounded-panel border border-border bg-surface p-2 shadow-lg">
                  <button
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => handleUseCurrentLocation('to')}
                    className="flex items-center gap-2 rounded-panel px-2 py-1.5 text-left text-sm text-ink transition hover:bg-surface-alt"
                  >
                    <span className="text-accent">📍</span>
                    Use current location
                  </button>
                  <button
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => { setPickingMode('to'); setToFocused(false) }}
                    className="flex items-center gap-2 rounded-panel px-2 py-1.5 text-left text-sm text-ink transition hover:bg-surface-alt"
                  >
                    <MapPin size={14} className="text-accent" />
                    {t('pick_from_map')}
                  </button>
                  {toQuery && toMatches.length > 0 && <div className="my-1 border-t border-border" />}
                  {toMatches.map((stop) => (
                    <button
                      key={stop.id}
                      type="button"
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => { setToStop(stop); setToQuery(stop.name) }}
                      className="rounded-panel px-2 py-1.5 text-left text-sm text-ink transition hover:bg-surface-alt"
                    >
                      {stop.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <button
              type="button"
              onClick={handleShuffle}
              disabled={!fromStop && !toStop}
              title="Swap locations"
              className="mb-0.5 flex h-[38px] w-[38px] items-center justify-center rounded-panel border border-border bg-surface text-muted transition hover:bg-surface-alt hover:text-ink disabled:opacity-40"
            >
              <ArrowUpDown size={15} />
            </button>
          </div>

          <button
            type="button"
            onClick={onPlanRoute}
            disabled={!fromStop || !toStop || loading}
            className="mb-4 inline-flex w-full items-center justify-center gap-2 rounded-panel border border-accent bg-accent px-4 py-2 text-sm font-semibold text-white transition disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? <Loader2 size={15} className="animate-spin" /> : <Search size={15} />}
            {loading ? t('planning') : t('find_route')}
          </button>

          {results.length > 0 && (
            <div className="border-t border-border pt-3 space-y-3">
              {loading ? (
                <div className="flex h-32 items-center justify-center gap-2 text-sm text-muted">
                  <Loader2 size={16} className="animate-spin" />
                  {t('finding')}
                </div>
              ) : selectedDetailIndex !== null && results[selectedDetailIndex] ? (
                <div className="space-y-3">
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedDetailIndex(null)
                      setSelectedLegIndex(null)
                      setExpandedLegIndex(null)
                      setHighlightedVisitedStopId(null)
                    }}
                    className="inline-flex items-center gap-2 rounded-panel border border-border bg-surface-soft px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
                  >
                    <ArrowLeft size={14} />
                    {t('back_to_routes')}
                  </button>

                  <h3 className="text-base font-semibold text-ink">{t('route_details')}</h3>
                  <div className="grid gap-2">
                    {results[selectedDetailIndex].legs?.map((leg, idx) => {
                      const legColor = getColorForLegRouteType(leg.routeType, leg.mode)
                      const isExpanded = expandedLegIndex === idx
                      const hasVisitedStops = leg.visitedStops?.length > 0
                      return (
                        <div key={`${leg.lineCode || leg.lineName || leg.mode}-${idx}`}>
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedLegIndex(idx)
                              setExpandedLegIndex(isExpanded ? null : idx)
                            }}
                            className="w-full rounded-panel border p-3 text-left text-sm transition"
                            style={{
                              borderColor:
                                selectedLegIndex === idx
                                  ? legColor
                                  : withAlpha(legColor, 0.45),
                              backgroundColor:
                                selectedLegIndex === idx
                                  ? withAlpha(legColor, theme === 'dark' ? 0.24 : 0.16)
                                  : withAlpha(legColor, theme === 'dark' ? 0.18 : 0.1),
                            }}
                          >
                            <div className="flex items-center justify-between gap-2">
                              <div className="min-w-0">
                                <p className="font-semibold truncate" style={{ color: legColor }}>{getLegDisplayLabel(leg)}</p>
                                <p className="mt-1 text-muted truncate">
                                  {leg.fromName} {'->'} {leg.toName}
                                </p>
                                <p className="mt-1 text-xs text-muted">{t('distance', { distance: Math.round(leg.distanceMeters) })}</p>
                              </div>
                              {hasVisitedStops && (
                                <ChevronDown
                                  size={16}
                                  className="shrink-0 text-muted transition-transform duration-200"
                                  style={{ transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)' }}
                                />
                              )}
                            </div>
                          </button>
                          {isExpanded && hasVisitedStops && (
                            <div className="mx-1 mt-1 mb-2 divide-y divide-border rounded-lg border border-border bg-surface">
                              {leg.visitedStops.map((stop, stopIdx) => {
                                const localStop = stops.find((s) => s.name === stop.name)
                                const stopId = localStop?.id ?? `${idx}-${stopIdx}`
                                const isHovered = highlightedVisitedStopId === stopId
                                return (
                                  <div
                                    key={stop.gtfsId || `vs-${idx}-${stopIdx}`}
                                    role="button"
                                    tabIndex={0}
                                    onClick={() => localStop && navigate(`/stops/${localStop.id}`)}
                                    onMouseEnter={() => setHighlightedVisitedStopId(stopId)}
                                    onMouseLeave={() => setHighlightedVisitedStopId(null)}
                                    onKeyDown={(e) => { if (e.key === 'Enter' && localStop) navigate(`/stops/${localStop.id}`) }}
                                    className={`flex cursor-pointer items-start gap-2 px-3 py-2 text-sm text-ink transition ${
                                      isHovered ? 'bg-accent/10' : 'hover:bg-surface-alt'
                                    }`}
                                  >
                                    <span className="mt-0.5 flex-shrink-0 rounded-full bg-accent/20 px-2 text-xs font-semibold text-accent">
                                      #{stopIdx + 1}
                                    </span>
                                    <span className="font-medium">{stop.name}</span>
                                  </div>
                                )
                              })}
                            </div>
                          )}
                        </div>
                      )
                    })}
                  </div>
                </div>
              ) : (
                <div className="space-y-3">
                  <h3 className="text-base font-semibold text-ink">{t('found_routes')}</h3>
                  {results.length === 0 ? (
                    <p className="text-sm text-muted">{t('no_routes')}</p>
                  ) : (
                    results.map((itinerary, index) => (
                      (() => {
                        const primaryLeg = itinerary.legs?.find((leg) => String(leg.mode || '').toUpperCase() !== 'WALK') || itinerary.legs?.[0]
                        const cardColor = getColorForLegRouteType(primaryLeg?.routeType, primaryLeg?.mode)
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
                          <span className="text-muted">{t('transfers', { count: itinerary.transfers })}</span>
                        </div>
                        <p className="mt-1 text-xs text-muted">
                          {t('departure', { time: formatDepartureFromTimestamp(itinerary.legs?.[0]?.startTime) })}
                        </p>
                        <ol className="mt-2 space-y-1">
                          {itinerary.legs?.map((leg, legIndex) => (
                            <li key={`${index}-${legIndex}`} className="flex items-center gap-2 text-xs text-ink">
                              <span
                                className="h-2 w-2 rounded-full"
                                style={{ backgroundColor: getColorForLegRouteType(leg.routeType, leg.mode) }}
                              />
                              <span className="font-semibold" style={{ color: getColorForLegRouteType(leg.routeType, leg.mode) }}>
                                {getLegDisplayLabel(leg)}
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

              <button
                type="button"
                onClick={exitRouteMode}
                className="inline-flex w-full items-center justify-center gap-2 rounded-panel border border-border bg-surface-soft px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
              >
                <ArrowLeft size={14} />
                {t('back_to_map')}
              </button>
            </div>
          )}
        </aside>

        <section className="order-1 lg:order-2 relative overflow-visible rounded-panel border border-border bg-surface">
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

          <TransitMap
            className="h-[calc(100vh-9rem)] min-h-[520px] lg:h-[calc(100vh-8rem)]"
            stops={allMapStops}
            stopStyle="dot"
            vehicles={vehicles}
            polylines={coloredPolylines}
            focusPositions={focusPositions}
            focusKey={focusKey}
            highlightedStopId={highlightedVisitedStopId}
            onStopClick={(stop) => {
              if (typeof stop.id === 'number') navigate(`/stops/${stop.id}`)
            }}
            onStopHover={(stop) => setHighlightedVisitedStopId(stop.id)}
            onStopHoverEnd={() => setHighlightedVisitedStopId(null)}
            onLegPathClick={(legIndex) => setSelectedLegIndex(legIndex)}
            startPin={fromStop ? [fromStop.latitude, fromStop.longitude] : null}
            endPin={toStop ? [toStop.latitude, toStop.longitude] : null}
            onMapClick={handleMapClick}
          />
        </section>
      </div>
    </div>
  )
}
