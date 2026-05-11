import { Heart, RefreshCw } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { TransitMap } from '../components/map/TransitMap'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

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

  useEffect(() => {
    let active = true
    setLoading(true)

    Promise.all([transitApi.getLineById(lineId), transitApi.getDirectionsByLine(lineId)])
      .then(async ([lineResponse, directionResponse]) => {
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
          if (!active) return
          setStops(directionStops)
          setPolyline(directionPolyline)
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [lineId])

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

  const favorite = useMemo(() => favorites.lines.includes(lineId), [favorites.lines, lineId])

  if (loading) {
    return (
      <PanelCard tone="soft">
        <p className="text-sm text-muted">Loading line details...</p>
      </PanelCard>
    )
  }

  if (!line) {
    return (
      <PanelCard>
        <p className="text-sm text-muted">Line not found.</p>
        <Link to="/lines" className="mt-3 inline-block text-sm font-medium text-accent">
          Back to lines
        </Link>
      </PanelCard>
    )
  }

  return (
    <div className="space-y-4">
      <PanelCard>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <LineBadge line={line} />
            <h2 className="mt-2 text-xl font-semibold text-ink">{line.name}</h2>
            <p className="mt-1 text-sm text-muted">Direction-aware stop list and route preview.</p>
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
            <Heart size={16} />
            {favorite ? 'Favorited' : 'Add to favourites'}
          </button>
        </div>

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

          <Link
            to="/lines"
            className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-2 text-sm text-muted transition hover:bg-surface-alt"
          >
            <RefreshCw size={15} />
            Change line
          </Link>
        </div>
      </PanelCard>

      <div className="grid gap-4 xl:grid-cols-[1.05fr_1fr]">
        <PanelCard>
          <h3 className="text-base font-semibold text-ink">Stops</h3>
          <ol className="mt-3 divide-y divide-border rounded-lg border border-border bg-surface">
            {stops.map((stop) => (
              <li
                key={stop.id}
                className="px-3 py-2 text-sm text-ink"
              >
                <span className="mr-2 text-xs font-semibold text-muted">#{stop.stopSequence}</span>
                {stop.stationName}
              </li>
            ))}
          </ol>
        </PanelCard>

        <PanelCard>
          <h3 className="mb-3 text-base font-semibold text-ink">Route map</h3>
          <TransitMap polyline={polyline} className="h-[380px]" />
        </PanelCard>
      </div>
    </div>
  )
}
