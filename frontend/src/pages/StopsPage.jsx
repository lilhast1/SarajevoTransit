import { AlertCircle, MapPin, RefreshCw } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { PanelCard } from '../components/common/PanelCard'
import { ErrorAlert } from '../components/common/Alerts'
import { LoadingSkeletons, EmptyState } from '../components/common/LoadingStates'
import { transitApi } from '../services/transitApi'

/**
 * Stops Discovery Page - demonstrates:
 * - Real-time search & filtering
 * - Loading and error states
 * - Optimized re-renders with useMemo
 * - SPA navigation to detail pages
 * - Empty state handling
 */
export function StopsPage() {
  const [allStops, setAllStops] = useState([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [hasInitialized, setHasInitialized] = useState(false)

  // Initial load of all stops
  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)

    const loadStops = async () => {
      try {
        const response = await transitApi.getStops()
        if (active) {
          setAllStops(response)
          setHasInitialized(true)
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Failed to load stops')
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadStops()

    return () => {
      active = false
    }
  }, [])

  // Client-side search filtering (no API call for every keystroke)
  const filteredStops = useMemo(() => {
    if (!query) return allStops

    const lowerQuery = query.toLowerCase()
    return allStops.filter((stop) => {
      const matchesName = stop.name?.toLowerCase().includes(lowerQuery)
      const matchesAddress = stop.address?.toLowerCase().includes(lowerQuery)
      const matchesCode = stop.code?.toLowerCase().includes(lowerQuery)
      return matchesName || matchesAddress || matchesCode
    })
  }, [allStops, query])

  const resultCount = useMemo(
    () => `${filteredStops.length} stop${filteredStops.length !== 1 ? 's' : ''}`,
    [filteredStops],
  )

  const handleClearSearch = () => setQuery('')

  const handleRetry = () => {
    setError(null)
    setLoading(true)
  }

  return (
    <div className="space-y-4">
      <PanelCard tone="soft">
        <h2 className="text-xl font-semibold text-ink">Transit Stops</h2>
        <p className="mt-1 text-sm text-muted">
          Find all transit stops in Sarajevo. Search by name, address, or stop code. Click a stop to view
          which lines serve it and upcoming departures.
        </p>

        <div className="mt-4 flex gap-2">
          <div className="flex-1">
            <label htmlFor="stop-search" className="mb-1 block text-xs font-semibold uppercase tracking-[0.12em] text-muted">
              Search stops
            </label>
            <input
              id="stop-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by name, address, or code..."
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
              autoFocus
            />
          </div>
          {query && (
            <div className="flex items-end">
              <button
                type="button"
                onClick={handleClearSearch}
                className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
              >
                Clear
              </button>
            </div>
          )}
        </div>

        <p className="mt-3 text-xs font-medium uppercase tracking-[0.12em] text-muted">{resultCount}</p>
      </PanelCard>

      {error && (
        <ErrorAlert
          error={error}
          onDismiss={() => setError(null)}
        />
      )}

      <div className="grid gap-3">
        {/* Loading state */}
        {loading && !hasInitialized ? (
          <LoadingSkeletons count={3} />
        ) : null}

        {/* Error state */}
        {!loading && error ? (
          <EmptyState
            icon={AlertCircle}
            title="Failed to load stops"
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
        ) : null}

        {/* Empty search results */}
        {!loading && hasInitialized && filteredStops.length === 0 ? (
          <EmptyState
            icon={MapPin}
            title="No stops found"
            description={
              query 
                ? `No stops match "${query}". Try a different search.` 
                : 'No stops available'
            }
            action={
              query ? (
                <button
                  onClick={handleClearSearch}
                  className="inline-flex items-center gap-2 text-sm font-medium text-accent transition hover:underline"
                >
                  Clear search to see all stops
                </button>
              ) : null
            }
          />
        ) : null}

        {/* List of stops */}
        {hasInitialized &&
          filteredStops.map((stop) => (
            <PanelCard
              key={stop.id}
              className="flex items-center justify-between gap-3 transition hover:bg-surface-alt"
              tone="default"
            >
              <div className="flex-1">
                <p className="font-semibold text-ink">{stop.name}</p>
                {stop.address && <p className="text-sm text-muted">{stop.address}</p>}
                {stop.code && <p className="mt-1 text-xs text-muted">Code: {stop.code}</p>}
              </div>
              <Link
                to={`/stops/${stop.id}`}
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
