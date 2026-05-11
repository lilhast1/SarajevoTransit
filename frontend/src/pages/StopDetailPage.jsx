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

      <div className="grid gap-4 lg:grid-cols-[1fr_1fr]">
        <PanelCard tone="default">
          <h3 className="text-base font-semibold text-ink">Lines serving this stop</h3>
          <div className="mt-3 grid gap-2">
            {stop.lines.map((line) => (
              <div key={line.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2">
                <LineBadge line={line} />
                <p className="mt-1 text-sm font-medium text-ink">{line.name}</p>
              </div>
            ))}
          </div>
        </PanelCard>

        <PanelCard tone="default">
          <h3 className="text-base font-semibold text-ink">Next departures</h3>
          <div className="mt-3 space-y-2">
            {stop.departures.map((departure) => (
              <div
                key={`${departure.id}-${departure.departureTime}`}
                className="flex items-center justify-between rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm"
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
          </div>
        </PanelCard>
      </div>

      <PanelCard>
        <h3 className="mb-3 text-base font-semibold text-ink">Stop map</h3>
        <TransitMap stops={[stop]} className="h-[300px]" />
      </PanelCard>
    </div>
  )
}
