import { Search } from 'lucide-react'
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

  const primary = results[0]
  const legStops = (primary?.legs || [])
    .flatMap((leg) => [leg.fromName, leg.toName])
    .filter((value, index, array) => array.indexOf(value) === index)
    .map((name, index) => ({
      id: `${name}-${index}`,
      name,
      latitude: sarajevoCenter[0] + index * 0.005,
      longitude: sarajevoCenter[1] + index * 0.004,
    }))

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
        numItineraries: 3,
      })
      setResults(response.itineraries || [])

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

  return (
    <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
      <section className="order-2 border border-border bg-surface xl:order-1 xl:h-[calc(100vh-8rem)]">
        <TransitMap
          className="h-[500px] sm:h-[600px] md:h-[700px] xl:h-full"
          stops={legStops}
          startPin={fromStop ? [fromStop.latitude, fromStop.longitude] : null}
          endPin={toStop ? [toStop.latitude, toStop.longitude] : null}
        />
      </section>

      <div className="order-1 space-y-4 xl:order-2 xl:max-h-[calc(100vh-8rem)] xl:overflow-auto">
        <PanelCard tone="soft">
          <h2 className="text-xl font-semibold text-ink">Route Planner</h2>
          <p className="mt-1 text-sm text-muted">
            Search by stop names, then fetch optimal route via gateway.
          </p>

          <div className="mt-4 space-y-3">
            <div>
              <label htmlFor="planner-from" className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">From</label>
              <input
                id="planner-from"
                value={fromQuery}
                onChange={(event) => {
                  setFromQuery(event.target.value)
                  setFromStop(null)
                }}
                placeholder="Start stop"
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink"
              />
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
              <input
                id="planner-to"
                value={toQuery}
                onChange={(event) => {
                  setToQuery(event.target.value)
                  setToStop(null)
                }}
                placeholder="Destination stop"
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink"
              />
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
                  <div key={index} className="rounded-panel border border-border bg-surface-soft p-3">
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
                        <li key={legIndex} className="text-sm text-ink">
                          <span className="font-semibold">{leg.mode}</span> · {leg.fromName} → {leg.toName}
                        </li>
                      ))}
                    </ol>
                  </div>
                ))
              )}
            </div>
          </PanelCard>
        </div>
      </div>
    </div>
  )
}
