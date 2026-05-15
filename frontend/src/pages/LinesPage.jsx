import { ArrowLeft, Route } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { TransitMap } from '../components/map/TransitMap'
import { transitApi } from '../services/transitApi'

const typeFilters = ['all', 'tram', 'bus', 'trolleybus', 'minibus']

export function LinesPage() {
  const [lines, setLines] = useState([])
  const [query, setQuery] = useState('')
  const [type, setType] = useState('all')
  const [loading, setLoading] = useState(true)

  const [selectedLineId, setSelectedLineId] = useState(null)
  const [selectedLine, setSelectedLine] = useState(null)
  const [directions, setDirections] = useState([])
  const [selectedDirectionId, setSelectedDirectionId] = useState(null)
  const [stops, setStops] = useState([])
  const [polyline, setPolyline] = useState([])
  const [detailLoading, setDetailLoading] = useState(false)

  const detailMode = selectedLineId !== null

  useEffect(() => {
    let active = true
    setLoading(true)
    transitApi
      .getLines({ search: query, vehicleType: type === 'all' ? '' : type, activeOnly: true })
      .then((response) => {
        if (active) setLines(response)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [query, type])

  useEffect(() => {
    if (!selectedLineId) return
    let active = true
    setDetailLoading(true)

    Promise.all([transitApi.getLineById(selectedLineId), transitApi.getDirectionsByLine(selectedLineId)])
      .then(async ([lineResponse, directionResponse]) => {
        if (!active) return
        setSelectedLine(lineResponse)
        setDirections(directionResponse)

        if (directionResponse.length > 0) {
          let foundDirectionId = null

          // Try to find the first direction that has a valid polyline
          for (const direction of directionResponse) {
            const dPolyline = await transitApi.getDirectionPolyline(direction.id)
            if (!active) return

            if (dPolyline && dPolyline.length > 1) {
              foundDirectionId = direction.id
              break 
            }
          }

          // Fallback to first direction if none had geometry
          if (!foundDirectionId) {
            foundDirectionId = directionResponse[0].id
          }

          setSelectedDirectionId(foundDirectionId)
        } else {
          setSelectedDirectionId(null)
          setStops([])
          setPolyline([])
        }
      })
      .finally(() => {
        if (active) setDetailLoading(false)
      })

    return () => {
      active = false
    }
  }, [selectedLineId])

  useEffect(() => {
    if (!selectedDirectionId) return
    let active = true

    Promise.all([
      transitApi.getDirectionStations(selectedDirectionId),
      transitApi.getDirectionPolyline(selectedDirectionId),
    ]).then(([directionStops, directionPolyline]) => {
      if (!active) return
      setStops(directionStops)
      setPolyline(directionPolyline)
    })

    return () => {
      active = false
    }
  }, [selectedDirectionId])

  const countLabel = useMemo(() => `${lines.length} line${lines.length === 1 ? '' : 's'}`, [lines])

  const openLineDetail = (lineId) => {
    setSelectedLineId(lineId)
    setSelectedLine(null)
    setDirections([])
    setSelectedDirectionId(null)
    setStops([])
    setPolyline([])
  }

  const backToSearch = () => {
    setSelectedLineId(null)
    setSelectedLine(null)
    setDirections([])
    setSelectedDirectionId(null)
    setStops([])
    setPolyline([])
  }

  const isPolylineEmpty = useMemo(() => {
    if (!polyline || polyline.length <= 1) return true
    return false
  }, [polyline])

  return (
    <div className="space-y-4">
      <PanelCard tone="soft">
        {!detailMode ? (
          <>
            <h2 className="text-xl font-semibold text-ink">Line Search</h2>
            <p className="mt-1 text-sm text-muted">Browse all active public transport lines in Sarajevo.</p>

            <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
              <label htmlFor="line-search" className="sr-only">
                Search lines
              </label>
              <input
                id="line-search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search by line number or name"
                className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
              />
              <label htmlFor="line-type-filter" className="sr-only">
                Filter line type
              </label>
              <select
                id="line-type-filter"
                value={type}
                onChange={(event) => setType(event.target.value)}
                className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
              >
                {typeFilters.map((option) => (
                  <option key={option} value={option}>
                    {option === 'all' ? 'All types' : option}
                  </option>
                ))}
              </select>
            </div>

            <p className="mt-3 text-xs font-medium uppercase tracking-[0.12em] text-muted">{countLabel}</p>
          </>
        ) : (
          <>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                {selectedLine ? <LineBadge line={selectedLine} /> : null}
                <h2 className="mt-2 text-xl font-semibold text-ink">{selectedLine?.name || 'Loading line...'}</h2>
                <p className="mt-1 text-sm text-muted">Direction-aware stop list and route preview.</p>
              </div>

              <button
                type="button"
                onClick={backToSearch}
                className="inline-flex items-center gap-2 rounded-panel border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
              >
                <ArrowLeft size={14} />
                Back to search
              </button>
            </div>

            {directions.length > 0 ? (
              <div className="mt-4 flex flex-wrap gap-2">
                {directions.map((direction) => (
                  <button
                    key={direction.id}
                    type="button"
                    onClick={() => setSelectedDirectionId(direction.id)}
                    className={`rounded-lg border px-3 py-2 text-sm font-medium transition ${
                      selectedDirectionId === direction.id
                        ? 'border-accent bg-accent text-white'
                        : 'border-border text-ink hover:bg-surface-alt'
                    }`}
                  >
                    {direction.directionLabel} · {direction.name}
                  </button>
                ))}
              </div>
            ) : null}
          </>
        )}
      </PanelCard>

      <div className="flex flex-col gap-4 sm:flex-row">
        <PanelCard className="min-h-[520px] sm:w-1/3 sm:shrink-0">
          {!detailMode ? (
            <>
              <h3 className="mb-3 text-base font-semibold text-ink">Matching lines</h3>
              {loading ? <p className="text-sm text-muted">Loading lines...</p> : null}
              {!loading && lines.length === 0 ? <p className="text-sm text-muted">No lines match your search.</p> : null}

              <div className="grid gap-2">
                {lines.map((line) => (
                  <button
                    key={line.id}
                    type="button"
                    onClick={() => openLineDetail(line.id)}
                    className="rounded-panel border border-border bg-surface-soft px-3 py-3 text-left transition hover:bg-surface-alt"
                  >
                    <LineBadge line={line} />
                    <p className="mt-2 text-sm font-semibold text-ink">{line.name}</p>
                  </button>
                ))}
              </div>
            </>
          ) : (
            <>
              <h3 className="mb-3 text-base font-semibold text-ink">Stations</h3>
              {detailLoading ? <p className="text-sm text-muted">Loading line details...</p> : null}

              {!detailLoading && stops.length === 0 ? (
                <p className="text-sm text-muted">No stations available for this direction.</p>
              ) : null}

              {stops.length > 0 ? (
                <ol className="divide-y divide-border rounded-lg border border-border bg-surface">
                  {stops.map((stop) => (
                    <li key={stop.id} className="px-3 py-2 text-sm text-ink">
                      <span className="mr-2 text-xs font-semibold text-muted">#{stop.stopSequence}</span>
                      {stop.stationName}
                    </li>
                  ))}
                </ol>
              ) : null}
            </>
          )}
        </PanelCard>

        <PanelCard className="min-h-[520px] flex-1">
          <h3 className="mb-3 text-base font-semibold text-ink">Route map</h3>

          {!detailMode ? (
            <div className="flex h-[460px] items-center justify-center rounded-lg border border-dashed border-border bg-surface-soft px-4 text-center">
              <div>
                <Route className="mx-auto mb-2 text-muted" size={22} />
                <p className="text-sm text-muted">Select a line to preview its route map and stations.</p>
              </div>
            </div>
          ) : detailLoading ? (
            <div className="flex h-[460px] items-center justify-center rounded-lg border border-border bg-surface-soft">
              <p className="text-sm text-muted">Loading route map...</p>
            </div>
          ) : isPolylineEmpty ? (
            <div className="flex h-[460px] flex-col items-center justify-center rounded-lg border border-dashed border-border bg-surface-soft px-4 text-center">
              <Route className="mb-2 text-muted" size={24} />
              <p className="text-sm font-medium text-ink">No route geometry available</p>
              <p className="mt-1 text-xs text-muted">The polyline data for this direction is currently unavailable.</p>
            </div>
          ) : (
            <TransitMap
              className="h-[460px]"
              polyline={polyline}
              focusPositions={polyline}
              focusKey={`${selectedLineId || 'none'}-${selectedDirectionId || 'none'}`}
            />
          )}
        </PanelCard>
      </div>
    </div>
  )
}
