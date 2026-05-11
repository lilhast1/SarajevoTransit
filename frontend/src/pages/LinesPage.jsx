import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { transitApi } from '../services/transitApi'

const typeFilters = ['all', 'tram', 'bus', 'trolleybus', 'minibus']

export function LinesPage() {
  const [lines, setLines] = useState([])
  const [query, setQuery] = useState('')
  const [type, setType] = useState('all')
  const [loading, setLoading] = useState(true)

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

  const countLabel = useMemo(() => `${lines.length} line${lines.length === 1 ? '' : 's'}`, [lines])

  return (
    <div className="space-y-4">
      <PanelCard tone="soft">
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
      </PanelCard>

      <div className="grid gap-3">
        {loading ? (
          <PanelCard>
            <p className="text-sm text-muted">Loading lines...</p>
          </PanelCard>
        ) : null}

        {!loading && lines.length === 0 ? (
          <PanelCard>
            <p className="text-sm text-muted">No lines match your search.</p>
          </PanelCard>
        ) : null}

        {lines.map((line) => (
          <PanelCard key={line.id} className="flex items-center justify-between gap-3" tone="default">
            <div>
              <LineBadge line={line} />
              <p className="mt-2 text-base font-semibold text-ink">{line.name}</p>
            </div>
            <Link
              to={`/lines/${line.id}`}
              className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
            >
              View details
            </Link>
          </PanelCard>
        ))}
      </div>
    </div>
  )
}
