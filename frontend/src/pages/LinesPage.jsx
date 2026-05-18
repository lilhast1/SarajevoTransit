import { ArrowLeft, Route } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { LoadingSkeletons, EmptyState } from '../components/common/LoadingStates'
import { AlertCircle, RefreshCw } from 'lucide-react'
import { TransitMap } from '../components/map/TransitMap'
import { transitApi } from '../services/transitApi'

const typeFilters = ['all', 'tram', 'bus', 'trolleybus', 'minibus']

/**
 * Lines Discovery Page - demonstrates:
 * - Client-side filtering (no full page reload)
 * - Real-time search API integration
 * - Loading and error states
 * - SPA navigation to detail pages
 */
export function LinesPage() {
  const [lines, setLines] = useState([])
  const [query, setQuery] = useState('')
  const [type, setType] = useState('all')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

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
    setError(null)

    const fetchLines = async () => {
      try {
        const response = await transitApi.getLines({
          search: query,
          vehicleType: type === 'all' ? '' : type,
          activeOnly: true,
        })
        if (active) {
          setLines(response)
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Failed to load lines')
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    fetchLines()

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

  const handleRetry = () => {
    setError(null)
    setLoading(true)
  }

  return (
    <div className="space-y-4">
      <PanelCard tone="soft">
        <h2 className="text-xl font-semibold text-ink">Public Transport Lines</h2>
        <p className="mt-1 text-sm text-muted">
          Browse and filter all active public transport lines in Sarajevo. Click on a line to see detailed route information,
          stops, and timetables.
        </p>

        <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
          <div>
            <label htmlFor="line-search" className="mb-1 block text-xs font-semibold uppercase tracking-[0.12em] text-muted">
              Search
            </label>
            <input
              id="line-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by line number or name"
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
            />
          </div>
          <div>
            <label htmlFor="line-type-filter" className="mb-1 block text-xs font-semibold uppercase tracking-[0.12em] text-muted">
              Type
            </label>
            <select
              id="line-type-filter"
              value={type}
              onChange={(event) => setType(event.target.value)}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring md:w-auto"
            >
              {typeFilters.map((option) => (
                <option key={option} value={option}>
                  {option === 'all' ? 'All types' : option}
                </option>
              ))}
            </select>
          </div>
        </div>

        <p className="mt-3 text-xs font-medium uppercase tracking-[0.12em] text-muted">{countLabel}</p>
      </PanelCard>

      {error && (
        <ErrorAlert
          error={error}
          onDismiss={() => setError(null)}
        />
      )}

      <div className="grid gap-3">
        {loading ? (
          <LoadingSkeletons count={3} />
        ) : null}

        {!loading && error && (
          <EmptyState
            icon={AlertCircle}
            title="Failed to load lines"
            description={error}
            action={
              <button
                onClick={handleRetry}
                className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-2 text-sm font-medium text-white transition hover:bg-accent/90"
              >
                <RefreshCw size={14} />
                Try again
              </button>
            }
          />
        )}

        {!loading && !error && lines.length === 0 ? (
          <EmptyState
            title="No lines found"
            description={query || type !== 'all' ? 'Try adjusting your search or filters' : 'No active lines available'}
          />
        ) : null}

        {!loading && !error && lines.map((line) => (
          <PanelCard
            key={line.id}
            className="flex items-center justify-between gap-3 transition hover:bg-surface-alt"
            tone="default"
          >
            <div>
              <LineBadge line={line} />
              <p className="mt-2 text-base font-semibold text-ink">{line.name}</p>
              {line.description && <p className="text-sm text-muted">{line.description}</p>}
            </div>
            <Link
              to={`/lines/${line.id}`}
              className="flex-shrink-0 rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface"
            >
              Details
            </Link>
          </PanelCard>
        ))}
      </div>
    </div>
  )
}
