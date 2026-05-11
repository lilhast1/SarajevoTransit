import { MapPin, Search } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { PanelCard } from '../components/common/PanelCard'
import { TransitMap } from '../components/map/TransitMap'
import { sarajevoCenter } from '../data/mockTransitData'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'
import { formatDepartureFromTimestamp, formatDurationFromSeconds } from '../utils/formatters'

function getCoordsFromStop(stop) {
  if (!stop) return null
  return { lat: stop.latitude, lon: stop.longitude }
}

export function RoutePlannerPage() {
  const { addTripHistoryItem } = useAppContext()
  const [stops, setStops] = useState([])
  const [fromQuery, setFromQuery] = useState('')
  const [toQuery, setToQuery] = useState('')
  const [fromStop, setFromStop] = useState(null)
  const [toStop, setToStop] = useState(null)
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [showMobileSheet, setShowMobileSheet] = useState(true)
  const [selectedItineraryIndex, setSelectedItineraryIndex] = useState(0)
  const [pickingMode, setPickingMode] = useState('none') // 'none' | 'from' | 'to'

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

  const modeColors = {
    WALK: '#94a3b8',       // Slate (Gray)
    TRAM: '#f59e0b',       // Amber (Orange/Yellow) - Changed from Red to distinguish
    BUS: '#3b82f6',        // Blue
    TROLLEYBUS: '#10b981', // Emerald (Green)
    SUBWAY: '#8b5cf6',     // Violet (Purple)
    RAIL: '#6366f1',       // Indigo
    CABLE_CAR: '#ec4899',  // Pink
    FERRY: '#06b6d4',      // Cyan
    TRANSIT: '#6366f1',    // Indigo (Generic fallback)
  }

  const primary = results[selectedItineraryIndex]
  const coloredPolylines = useMemo(() => {
    if (!primary?.legs) return []
    return primary.legs.map((leg) => {
      const mode = leg.mode.toUpperCase()
      let color = modeColors[mode]
      if (!color) {
        if (mode.includes('BUS')) color = modeColors.BUS
        else if (mode.includes('TRAM')) color = modeColors.TRAM
        else if (mode.includes('TROLLEY')) color = modeColors.TROLLEYBUS
        else color = modeColors.TRANSIT
      }
      return {
        positions: leg.path || [],
        color: color,
        label: `${leg.mode}: ${leg.fromName} → ${leg.toName} (${Math.round(leg.distanceMeters / 100) / 10} km)`,
      }
    })
  }, [primary])

  const legStops = useMemo(() => {
    if (!primary?.legs) return []
    const stopsSet = new Set()
    const collected = []

    primary.legs.forEach((leg, index) => {
      // From stop
      if (!stopsSet.has(leg.fromName)) {
        stopsSet.add(leg.fromName)
        const firstPoint = leg.path?.[0]
        collected.push({
          id: `stop-from-${index}`,
          name: leg.fromName,
          latitude: firstPoint ? firstPoint[0] : sarajevoCenter[0],
          longitude: firstPoint ? firstPoint[1] : sarajevoCenter[1],
        })
      }
      // To stop
      if (!stopsSet.has(leg.toName)) {
        stopsSet.add(leg.toName)
        const lastPoint = leg.path?.[leg.path.length - 1]
        collected.push({
          id: `stop-to-${index}`,
          name: leg.toName,
          latitude: lastPoint ? lastPoint[0] : sarajevoCenter[0],
          longitude: lastPoint ? lastPoint[1] : sarajevoCenter[1],
        })
      }
    })
    return collected
  }, [primary])

  const onPlanRoute = async () => {
    if (!fromStop || !toStop) return

    const fromCoords = getCoordsFromStop(fromStop)
    const toCoords = getCoordsFromStop(toStop)
    if (!fromCoords || !toCoords) return

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

  const handleMapClick = (latlng) => {
    if (pickingMode === 'from') {
      setFromStop({
        id: 'custom-from',
        name: `Map point (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`,
        latitude: latlng.lat,
        longitude: latlng.lng,
      })
      setFromQuery(`Map point (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`)
      setPickingMode('none')
    } else if (pickingMode === 'to') {
      setToStop({
        id: 'custom-to',
        name: `Map point (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`,
        latitude: latlng.lat,
        longitude: latlng.lng,
      })
      setToQuery(`Map point (${latlng.lat.toFixed(4)}, ${latlng.lng.toFixed(4)})`)
      setPickingMode('none')
    }
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
      <section className="order-2 border border-border bg-surface xl:order-1 xl:h-[calc(100vh-8rem)]">
          <TransitMap
            className="h-[500px] sm:h-[600px] md:h-[700px] xl:h-full"
            stops={legStops}
            polylines={coloredPolylines}
            startPin={fromStop ? [fromStop.latitude, fromStop.longitude] : null}
            endPin={toStop ? [toStop.latitude, toStop.longitude] : null}
            onMapClick={handleMapClick}
          />
      </section>

      <div className="order-1 space-y-4 xl:order-2 xl:max-h-[calc(100vh-8rem)] xl:overflow-auto">
        <PanelCard tone="soft">
          <h2 className="text-xl font-semibold text-ink">Route Planner</h2>
          <p className="mt-1 text-sm text-muted">
            Search by name or click the <MapPin size={12} className="inline align-baseline" /> to pick from map.
          </p>

          <div className="mt-4 space-y-3">
            <div>
              <label htmlFor="planner-from" className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">From</label>
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
                      ? 'border-accent bg-accent text-white shadow-sm shadow-accent/20' 
                      : 'border-border bg-surface text-muted hover:bg-surface-alt hover:text-ink'
                  }`}
                >
                  <MapPin size={16} />
                </button>
              </div>
              {fromQuery && !fromStop ? (
                <div className="mt-2 grid gap-1 rounded-panel border border-border bg-surface p-2">
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

            <div>
              <label htmlFor="planner-to" className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">To</label>
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
                      ? 'border-accent bg-accent text-white shadow-sm shadow-accent/20' 
                      : 'border-border bg-surface text-muted hover:bg-surface-alt hover:text-ink'
                  }`}
                >
                  <MapPin size={16} />
                </button>
              </div>
              {toQuery && !toStop ? (
                <div className="mt-2 grid gap-1 rounded-panel border border-border bg-surface p-2">
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
              className="inline-flex w-full items-center justify-center gap-2 rounded-panel border border-accent bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Search size={15} />
              {loading ? 'Planning route...' : 'Find route'}
            </button>
          </div>
        </PanelCard>

        <button
          type="button"
          onClick={() => setShowMobileSheet((current) => !current)}
          className="w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm font-medium text-ink md:hidden"
        >
          {showMobileSheet ? 'Hide route results' : 'Show route results'}
        </button>

        <div className={`${showMobileSheet ? 'block' : 'hidden'} md:block`}>
          <PanelCard tone="default" className="md:max-h-[46vh] md:overflow-auto">
            <h3 className="text-base font-semibold text-ink">Journey options</h3>
            <div className="mt-3 grid gap-3">
              {results.length === 0 ? (
                <p className="text-sm text-muted">No route planned yet.</p>
              ) : (
                results.map((itinerary, index) => (
                  <button
                    key={index}
                    type="button"
                    onClick={() => setSelectedItineraryIndex(index)}
                    className={`rounded-panel border text-left p-3 transition transition-all duration-200 ${
                      selectedItineraryIndex === index
                        ? 'border-accent bg-accent/5 ring-1 ring-accent'
                        : 'border-border bg-surface-soft hover:bg-surface-alt'
                    }`}
                  >
                    <div className="flex flex-wrap gap-4 text-sm">
                      <span className="font-semibold text-ink">
                        {formatDurationFromSeconds(itinerary.durationSeconds)}
                      </span>
                      <span className="text-muted">Transfers: {itinerary.transfers}</span>
                      <span className="text-muted">
                        Departure: {formatDepartureFromTimestamp(itinerary.legs?.[0]?.startTime)}
                      </span>
                    </div>
                    <ol className="mt-3 space-y-1">
                      {itinerary.legs?.map((leg, legIndex) => (
                        <li key={legIndex} className="flex items-center gap-2 text-sm text-ink">
                          <span
                            className="h-2 w-2 rounded-full"
                            style={{ backgroundColor: modeColors[leg.mode.toUpperCase()] || '#6366f1' }}
                          />
                          <span className="font-semibold">{leg.mode}</span> · {leg.fromName} → {leg.toName}
                        </li>
                      ))}
                    </ol>
                  </button>
                ))
              )}
            </div>
          </PanelCard>
        </div>
      </div>
    </div>
  )
}
