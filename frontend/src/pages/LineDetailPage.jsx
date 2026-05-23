import { Heart, AlertCircle } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { LineDetailLayout } from '../components/lines/LineDetailLayout'
import { ErrorAlert } from '../components/common/Alerts'
import { EmptyState } from '../components/common/LoadingStates'
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
  const navigate = useNavigate()
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

  const handleStopClick = (stopId) => {
    navigate(`/stops/${stopId}`)
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

  if (!loading && !line) {
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
      <LineDetailLayout
        line={line}
        directions={directions}
        selectedDirectionId={selectedDirectionId}
        onSelectDirection={setSelectedDirectionId}
        stops={stops}
        polyline={polyline}
        detailLoading={loading}
        onBack={() => navigate('/lines')}
        backLabel="Back to lines"
        onStopClick={handleStopClick}
        subtitle="Direction-aware stop list and route preview."
        directionAction={(
          <button
            type="button"
            onClick={() => toggleFavoriteLine(lineId)}
            className={`inline-flex items-center gap-2 rounded-panel border px-3 py-2 text-sm font-medium transition ${
              favorite
                ? 'border-accent bg-accent text-white'
                : 'border-border text-ink hover:bg-surface-alt'
            }`}
          >
            <Heart size={16} fill={favorite ? 'currentColor' : 'none'} />
            {favorite ? 'Favorited' : 'Add to favorites'}
          </button>
        )}
      />
    </div>
  )
}
