import { Heart } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { TransitMap } from '../components/map/TransitMap'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'
import { minutesUntil } from '../utils/formatters'

export function StopDetailPage() {
  const { id } = useParams()
  const stopId = Number(id)
  const { favorites, toggleFavoriteStop } = useAppContext()

  const [stop, setStop] = useState(null)
  const [loading, setLoading] = useState(true)
  const [departuresLimit, setDeparturesLimit] = useState(5)
  const [loadingMore, setLoadingMore] = useState(false)

  const [selectedPolyline, setSelectedPolyline] = useState(null)
  const [focusPositions, setFocusPositions] = useState([])
  const [focusKey, setFocusKey] = useState(0)

  // const handleLineClick = async (lineCode) => {
  //   try {
  //     const lines = await transitApi.getLines({ search: lineCode })
  //     const localLine = lines.find((l) => l.code === lineCode)
  //     if (!localLine) return
      
  //     const directions = await transitApi.getDirectionsByLine(localLine.id)
  //     if (!directions || directions.length === 0) return
      
  //     const polyline = await transitApi.getDirectionPolyline(directions[0].id)
  //     if (polyline && polyline.length > 0) {
  //       setSelectedPolyline(polyline)
  //       setFocusPositions(polyline)
  //       setFocusKey((k) => k + 1)
  //     }
  //   } catch (e) {
  //     console.warn('Failed to load polyline', e)
  //   }
  // }

  const handleLineClick = (lineId) => {
    // Reditect to line detail page
    window.location.href = `/lines/${lineId}`
  }

  const handleLoadMore = async () => {
    const newLimit = departuresLimit + 5
    setLoadingMore(true)
    try {
      const moreDeps = await transitApi.getStopDepartures(stopId, newLimit)
      setStop((s) => ({ ...s, departures: moreDeps }))
      setDeparturesLimit(newLimit)
    } finally {
      setLoadingMore(false)
    }
  }

  useEffect(() => {
    let active = true
    setLoading(true)
    transitApi
      .getStopById(stopId)
      .then((response) => {
        if (active) setStop(response)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [stopId])

  const favorite = useMemo(() => favorites.stops.includes(stopId), [favorites.stops, stopId])

  if (loading) {
    return (
      <PanelCard tone="soft">
        <p className="text-sm text-muted">Loading stop detail...</p>
      </PanelCard>
    )
  }

  if (!stop) {
    return (
      <PanelCard>
        <p className="text-sm text-muted">Stop not found.</p>
        <Link to="/stops" className="mt-3 inline-block text-sm font-medium text-accent">
          Back to stops
        </Link>
      </PanelCard>
    )
  }

  return (
    <div className="space-y-4">
      <PanelCard>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-ink">{stop.name}</h2>
            <p className="mt-1 text-sm text-muted">{stop.address}</p>
          </div>

          <button
            type="button"
            onClick={() => toggleFavoriteStop(stopId)}
            className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition ${
              favorite
                ? 'border-accent bg-accent text-white'
                : 'border-border text-ink hover:bg-surface-alt'
            }`}
          >
            <Heart size={16} />
            {favorite ? 'Favorited' : 'Add to favourites'}
          </button>
        </div>
      </PanelCard>

      <PanelCard>
        <h3 className="mb-3 text-base font-semibold text-ink">Stop map</h3>
        <TransitMap
          stops={[stop]}
          polyline={selectedPolyline || []}
          focusPositions={focusPositions}
          focusKey={focusKey}
          className="h-[300px]"
        />
      </PanelCard>

      <div className="grid gap-4 lg:grid-cols-[1fr_1fr]">
        <PanelCard tone="default" className="flex h-[400px] flex-col">
          <h3 className="shrink-0 text-base font-semibold text-ink">Lines serving this stop</h3>
          <div className="mt-3 grid flex-1 content-start gap-2 overflow-y-auto pr-2">
            {stop.lines.map((line) => (
              console.log(line) ||
              <div
                key={line.id}
                className="cursor-pointer rounded-lg border border-border bg-surface-soft px-3 py-2 transition hover:border-accent hover:bg-surface-alt"
                onClick={() => handleLineClick(line.gtfsId)}
              >
                <LineBadge line={line} />
                <p className="mt-1 text-sm font-medium text-ink">{line.name}</p>
              </div>
            ))}
          </div>
        </PanelCard>

        <PanelCard tone="default" className="flex h-[400px] flex-col">
          <h3 className="shrink-0 text-base font-semibold text-ink">Next departures</h3>
          <div className="mt-3 flex-1 space-y-2 overflow-y-auto pr-2">
            {stop.departures.map((departure) => (
              <div
                key={`${departure.id}-${departure.departureTime}`}
                className="flex cursor-pointer items-center justify-between rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm transition hover:border-accent hover:bg-surface-alt"
                onClick={() => handleLineClick(departure.gtfsId) /* Use gtfsId to identify line in line detail page */ }
              >
                <div>
                  <p className="font-semibold text-ink">{departure.lineCode}</p>
                  <p className="text-xs text-muted">{departure.directionName}</p>
                </div>
                <div className="text-right">
                  <p className="font-semibold text-ink">{departure.departureTime}</p>
                  <p className="text-xs text-muted">in {minutesUntil(departure.departureTime)} min</p>
                </div>
              </div>
            ))}
            <button
              onClick={handleLoadMore}
              disabled={loadingMore}
              className="mt-2 w-full rounded-lg py-2 text-sm font-medium text-accent transition hover:bg-surface-soft disabled:opacity-50"
            >
              {loadingMore ? 'Loading...' : 'Show more'}
            </button>
          </div>
        </PanelCard>
      </div>
    </div>
  )
}
