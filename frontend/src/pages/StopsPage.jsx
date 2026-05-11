import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PanelCard } from '../components/common/PanelCard'
import { transitApi } from '../services/transitApi'

export function StopsPage() {
  const [query, setQuery] = useState('')
  const [stops, setStops] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    transitApi
      .getStops({ search: query })
      .then((response) => {
        if (active) setStops(response)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [query])

  return (
    <div className="space-y-4">
      <PanelCard tone="soft">
        <h2 className="text-xl font-semibold text-ink">Station / Stop Lookup</h2>
        <p className="mt-1 text-sm text-muted">Search stops and open detailed departures.</p>

        <label htmlFor="stop-search" className="sr-only">
          Search stop
        </label>
        <input
          id="stop-search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search stop by name"
          className="mt-4 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
        />
      </PanelCard>

      <div className="grid gap-3">
        {loading ? (
          <PanelCard>
            <p className="text-sm text-muted">Loading stops...</p>
          </PanelCard>
        ) : null}

        {!loading && stops.length === 0 ? (
          <PanelCard>
            <p className="text-sm text-muted">No stops found.</p>
          </PanelCard>
        ) : null}

        {stops.map((stop) => (
          <PanelCard key={stop.id} className="flex items-center justify-between gap-3">
            <div>
              <p className="text-base font-semibold text-ink">{stop.name}</p>
              <p className="text-sm text-muted">{stop.address}</p>
            </div>
            <Link
              to={`/stops/${stop.id}`}
              className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
            >
              Open
            </Link>
          </PanelCard>
        ))}
      </div>
    </div>
  )
}
