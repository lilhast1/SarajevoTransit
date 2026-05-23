import { ArrowLeft, Route } from 'lucide-react'
import { useMemo, useState } from 'react'
import { LineBadge } from '../common/LineBadge'
import { PanelCard } from '../common/PanelCard'
import { TransitMap } from '../map/TransitMap'
import { getVehicleTypeMetaByName } from '../../constants/vehicleColors'

function adjustHexColor(hexColor, amount) {
  const hex = String(hexColor || '').replace('#', '')
  if (!/^[0-9a-fA-F]{6}$/.test(hex)) return hexColor

  const channel = (start) => {
    const value = parseInt(hex.slice(start, start + 2), 16)
    const next = amount >= 0
      ? value + (255 - value) * amount
      : value * (1 + amount)
    return Math.max(0, Math.min(255, Math.round(next)))
  }

  const r = channel(0).toString(16).padStart(2, '0')
  const g = channel(2).toString(16).padStart(2, '0')
  const b = channel(4).toString(16).padStart(2, '0')
  return `#${r}${g}${b}`
}

export function LineDetailLayout({
  line,
  directions = [],
  selectedDirectionId,
  onSelectDirection,
  stops = [],
  polyline = [],
  detailLoading = false,
  onBack,
  onStopClick,
  backLabel = 'Back',
  topAction,
  directionAction,
  title,
  subtitle = 'Direction-aware stop list and route preview.',
  mapHeightClass = 'h-[460px]',
  mapContainerClassName = 'h-[520px] flex-1 flex flex-col',
}) {
  const [highlightedStopId, setHighlightedStopId] = useState(null)
  const lineColor = useMemo(() => {
    const typeMeta = getVehicleTypeMetaByName(line?.vehicleTypeName || 'bus')
    return typeMeta?.color || '#3b82f6'
  }, [line?.vehicleTypeName])

  const mapStops = useMemo(
    () =>
      stops
        .filter((stop) => stop.stationLatitude != null && stop.stationLongitude != null)
        .map((stop) => ({
          id: stop.stationId,
          name: stop.stationName,
          latitude: Number(stop.stationLatitude),
          longitude: Number(stop.stationLongitude),
          stopSequence: stop.stopSequence,
          color: adjustHexColor(lineColor, -0.22),
          hoverColor: adjustHexColor(lineColor, 0.2),
        })),
    [lineColor, stops],
  )

  const isPolylineEmpty = useMemo(() => !polyline || polyline.length <= 1, [polyline])

  return (
    <>
      <PanelCard tone="soft">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            {line ? <LineBadge line={line} /> : null}
            <h2 className="mt-2 text-xl font-semibold text-ink">{title || line?.name || 'Loading line...'}</h2>
            <p className="mt-1 text-sm text-muted">{subtitle}</p>
          </div>

          <div className="flex items-center gap-2">
            {topAction}
            {onBack ? (
              <button
                type="button"
                onClick={onBack}
                className="inline-flex items-center gap-2 rounded-panel border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
              >
                <ArrowLeft size={14} />
                {backLabel}
              </button>
            ) : null}
          </div>
        </div>

        {directions.length > 0 ? (
          <div className="mt-4 flex flex-wrap items-end gap-2">
            <div className="flex flex-wrap gap-2">
              {directions.map((direction) => (
                <button
                  key={direction.id}
                  type="button"
                  onClick={() => onSelectDirection(direction.id)}
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
            {directionAction ? <div className="ml-auto">{directionAction}</div> : null}
          </div>
        ) : null}
      </PanelCard>

      <div className="flex flex-col gap-4 sm:flex-row">
        <PanelCard className="h-[520px] sm:w-1/3 sm:shrink-0 flex flex-col">
          <h3 className="mb-3 text-base font-semibold text-ink">Stations</h3>
          {detailLoading ? <p className="text-sm text-muted">Loading line details...</p> : null}

          {!detailLoading && stops.length === 0 ? (
            <p className="text-sm text-muted">No stations available for this direction.</p>
          ) : null}

          {stops.length > 0 ? (
            <div className="min-h-0 flex-1 overflow-y-auto pr-1">
              <ol className="divide-y divide-border rounded-lg border border-border bg-surface">
                {stops.map((stop) => (
                  <li key={stop.id}>
                    <button
                      type="button"
                      onClick={() => onStopClick && onStopClick(stop.stationId)}
                      onMouseEnter={() => setHighlightedStopId(stop.stationId)}
                      onMouseLeave={() => setHighlightedStopId(null)}
                      className="flex w-full items-start gap-2 px-3 py-2 text-left text-sm text-ink transition hover:bg-surface-alt"
                    >
                      <span className="mt-0.5 flex-shrink-0 rounded-full bg-accent/20 px-2 text-xs font-semibold text-accent">
                        #{stop.stopSequence}
                      </span>
                      <div className="flex-1">
                        <p className="font-medium text-ink">{stop.stationName}</p>
                        {stop.travelTimeFromPrevSeconds > 0 ? (
                          <p className="text-xs text-muted">
                            {Math.round(stop.travelTimeFromPrevSeconds / 60)} min from previous
                          </p>
                        ) : null}
                      </div>
                    </button>
                  </li>
                ))}
              </ol>
            </div>
          ) : null}
        </PanelCard>

        <PanelCard className={mapContainerClassName}>
          <h3 className="mb-3 text-base font-semibold text-ink">Route map</h3>

          {detailLoading ? (
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
              className={mapHeightClass}
              polylines={[{ positions: polyline, color: lineColor }]}
              stops={mapStops}
              stopStyle="dot"
              highlightedStopId={highlightedStopId}
              onStopClick={(stop) => onStopClick && onStopClick(stop.id)}
              onStopHover={(stop) => setHighlightedStopId(stop.id)}
              onStopHoverEnd={() => setHighlightedStopId(null)}
              focusPositions={polyline}
              focusKey={`${line?.id || 'none'}-${selectedDirectionId || 'none'}`}
            />
          )}
        </PanelCard>
      </div>
    </>
  )
}
