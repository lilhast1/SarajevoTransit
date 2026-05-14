import { Heart, AlertCircle } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { ErrorAlert } from '../components/common/Alerts'
import { LoadingSpinner, EmptyState } from '../components/common/LoadingStates'
import { TransitMap } from '../components/map/TransitMap'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

/**
 * Line Detail Page - demonstrates:
 * - Client-side routing with URL parameters
 * - Dynamic content loading based on route param
 * - Map visualization
 * - Optimistic UI updates for favorites
 */
export function LineDetailPage() {
  const { id } = useParams()
  const lineId = Number(id)
  const { favorites, toggleFavoriteLine } = useAppContext()

  const [line, setLine] = useState(null)
  const [directions, setDirections] = useState([])
  const [selectedDirectionId, setSelectedDirectionId] = useState(null)
  const [stops, setStops] = useState([])
  const [polyline, setPolyline] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)

    const loadLineData = async () => {
      try {
        const [lineResponse, directionResponse] = await Promise.all([
          transitApi.getLineById(lineId),
          transitApi.getDirectionsByLine(lineId),
        ])

        if (!active) return

        setLine(lineResponse)
        setDirections(directionResponse)

        const selected = directionResponse[0]?.id || null
        setSelectedDirectionId(selected)

        if (selected) {
          const [directionStops, directionPolyline] = await Promise.all([
            transitApi.getDirectionStations(selected),
            transitApi.getDirectionPolyline(selected),
          ])
          if (active) {
            setStops(directionStops)
            setPolyline(directionPolyline)
          }
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Failed to load line details')
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadLineData()

    return () => {
      active = false
    }
  }, [lineId])

  useEffect(() => {
    if (!selectedDirectionId) return
    let active = true

    const loadDirectionData = async () => {
      try {
        const [directionStops, directionPolyline] = await Promise.all([
          transitApi.getDirectionStations(selectedDirectionId),
          transitApi.getDirectionPolyline(selectedDirectionId),
        ])
        if (active) {
          setStops(directionStops)
          setPolyline(directionPolyline)
        }
      } catch (err) {
        console.error('Failed to load direction data:', err)
      }
    }

    loadDirectionData()

    return () => {
      active = false
    }
  }, [selectedDirectionId])

  const favorite = useMemo(() => favorites.lines.includes(lineId), [favorites.lines, lineId])

  if (loading) {
    return (
      <div className="space-y-4">
        <LoadingSpinner label="Loading line details..." />
      </div>
    )
  }

  if (error) {
    return (
      <div className="space-y-4">
        <ErrorAlert
          error={error}
          onDismiss={() => setError(null)}
        />
        <Link to="/lines" className="inline-flex items-center gap-2 text-sm font-medium text-accent hover:underline">
          ← Back to lines
        </Link>
      </div>
    )
  }

  if (!line) {
    return (
      <EmptyState
        icon={AlertCircle}
        title="Line not found"
        description="The line you're looking for doesn't exist or has been removed."
        action={
          <Link
            to="/lines"
            className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-2 text-sm font-medium text-white transition hover:bg-accent/90"
          >
            Back to lines
          </Link>
        }
      />
    )
  }

  return (
    <div className="space-y-4">
      <PanelCard>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <LineBadge line={line} />
            <h2 className="mt-2 text-xl font-semibold text-ink">{line.name}</h2>
            <p className="mt-1 text-sm text-muted">
              {directions.length} direction{directions.length !== 1 ? 's' : ''} available
            </p>
          </div>

          <button
            type="button"
            onClick={() => toggleFavoriteLine(lineId)}
            className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition ${
              favorite
                ? 'border-accent bg-accent text-white'
                : 'border-border text-ink hover:bg-surface-alt'
            }`}
          >
            <Heart size={16} fill={favorite ? 'currentColor' : 'none'} />
            {favorite ? 'Favorited' : 'Add to favorites'}
          </button>
        </div>
      </PanelCard>

      <div className="grid gap-4 lg:grid-cols-[1fr_1fr]">
        <PanelCard tone="default">
          <h3 className="text-base font-semibold text-ink">Directions</h3>
          {directions.length === 0 ? (
            <p className="mt-3 text-sm text-muted">No directions found.</p>
          ) : (
            <div className="mt-3 grid gap-2">
              {directions.map((direction) => (
                <button
                  key={direction.id}
                  type="button"
                  onClick={() => setSelectedDirectionId(direction.id)}
                  className={`rounded-lg border p-3 text-left text-sm transition ${
                    selectedDirectionId === direction.id
                      ? 'border-accent bg-accent/5 ring-1 ring-accent'
                      : 'border-border bg-surface-soft hover:bg-surface-alt'
                  }`}
                >
                  <p className="font-semibold text-ink">{direction.name}</p>
                  <p className="text-xs text-muted">{direction.directionLabel}</p>
                </button>
              ))}
            </div>
          )}
        </PanelCard>

        <PanelCard tone="default">
          <h3 className="text-base font-semibold text-ink">Stops</h3>
          {stops.length === 0 ? (
            <p className="mt-3 text-sm text-muted">Select a direction to see stops.</p>
          ) : (
            <div className="mt-3 max-h-80 space-y-1 overflow-auto">
              {stops.map((stop, index) => (
                <div key={stop.id} className="flex items-start gap-2 rounded-lg px-2 py-1 text-sm hover:bg-surface-alt">
                  <span className="flex-shrink-0 rounded-full bg-accent/20 px-2 font-semibold text-accent">
                    {stop.stopSequence}
                  </span>
                  <div className="flex-1">
                    <p className="font-medium text-ink">{stop.stationName}</p>
                    {stop.travelTimeFromPrevSeconds > 0 && (
                      <p className="text-xs text-muted">
                        {Math.round(stop.travelTimeFromPrevSeconds / 60)} min from previous
                      </p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </PanelCard>
      </div>

      {polyline.length > 0 && (
        <PanelCard className="p-0">
          <TransitMap
            className="h-96"
            polylines={[{ positions: polyline, color: '#f59e0b' }]}
          />
        </PanelCard>
      )}

      <div>
        <Link
          to="/lines"
          className="inline-flex items-center gap-2 text-sm font-medium text-accent transition hover:underline"
        >
          ← Back to all lines
        </Link>
      </div>
    </div>
  )
}
