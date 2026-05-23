import { useEffect, useMemo, useState } from 'react'
import { CircleMarker, MapContainer, Marker, Polyline, Popup, TileLayer, Tooltip, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { sarajevoCenter } from '../../data/mockTransitData'
import { useAppContext } from '../../context/AppContext'

function markerSvg(color, label) {
  return `
  <svg width="38" height="50" viewBox="0 0 38 50" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="${label}">
    <path d="M19 1C9.07 1 1 9.07 1 19c0 13.43 18 30 18 30s18-16.57 18-30C37 9.07 28.93 1 19 1z" fill="${color}" stroke="rgba(16,24,35,0.24)" stroke-width="1.2"/>
    <circle cx="19" cy="19" r="7" fill="rgba(255,255,255,0.88)"/>
  </svg>`
}

function createIcon(color, label, zoom) {
  // Base size at zoom 13 is [38, 50]
  // We scale linearly or with a power function. Let's try power of 1.2 per zoom level
  const scale = Math.pow(1.15, zoom - 13)
  const width = Math.max(8, 38 * scale)
  const height = Math.max(10, 50 * scale)
  const svg = markerSvg(color, label)
  const svgBase64 = window.btoa(unescape(encodeURIComponent(svg)))

  return L.icon({
    iconUrl: `data:image/svg+xml;base64,${svgBase64}`,
    iconSize: [width, height],
    iconAnchor: [width / 2, height],
    popupAnchor: [0, -height],
  })
}

export function TransitMap({
  polyline = [],
  polylines = [], // [{ positions, color }]
  stops = [],
  vehicles = [],
  startPin,
  endPin,
  className = 'h-80',
  highlight = '#e63946',
  onMapClick,
  focusPositions = [],
  focusKey,
  onLegPathClick,
  highlightedStopId,
  onStopClick,
  onStopHover,
  onStopHoverEnd,
  stopStyle = 'pin',
}) {
  const { theme } = useAppContext()
  const [zoom, setZoom] = useState(13)
  const center = useMemo(() => {
    if (polylines.length > 0 && polylines[0].positions.length > 0) return polylines[0].positions[0]
    if (polyline.length > 0) return polyline[0]
    if (stops.length > 0) return [stops[0].latitude, stops[0].longitude]
    if (vehicles.length > 0) return [vehicles[0].latitude, vehicles[0].longitude]
    return sarajevoCenter
  }, [polyline, polylines, stops, vehicles])

  return (
    <div className={`relative overflow-hidden ${className}`}>
      <MapContainer 
        center={center} 
        zoom={13} 
        scrollWheelZoom 
        className="z-10 h-full w-full"
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
          subdomains="abcd"
          url={theme === 'dark'
            ? 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
            : 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png'}
        />

        <MapEvents onMapClick={onMapClick} onZoomChange={setZoom} />
        <MapViewportController focusPositions={focusPositions} focusKey={focusKey} />

        {polyline.length > 1 ? <Polyline positions={polyline} pathOptions={{ color: highlight, weight: 5 }} /> : null}

        {polylines.map((pl, idx) => (
          pl.positions && pl.positions.length > 1 ? (
            <Polyline
              key={`${idx}-casing`}
              positions={pl.positions}
              pathOptions={{
                color: theme === 'dark' ? '#111827' : '#f8fafc',
                weight: 10,
                opacity: pl.casingOpacity ?? 0.85,
              }}
            />
          ) : null
        ))}

        {polylines.map((pl, idx) => (
          pl.positions && pl.positions.length > 1 ? (
            <Polyline
              key={idx}
              positions={pl.positions}
              pathOptions={{
                color: pl.color || highlight,
                weight: pl.weight || 6,
                opacity: pl.opacity ?? 0.9,
              }}
              eventHandlers={
                onLegPathClick && Number.isInteger(pl.legIndex)
                  ? { click: () => onLegPathClick(pl.legIndex) }
                  : undefined
              }
            >
              {pl.label && <Popup>{pl.label}</Popup>}
            </Polyline>
          ) : null
        ))}

        {vehicles.map((vehicle) => (
          <Marker
            key={vehicle.id}
            position={[vehicle.latitude, vehicle.longitude]}
            icon={createVehicleIcon({
              color: vehicle.color || '#334155',
              headingDeg: vehicle.headingDeg,
              isMoving: vehicle.isMoving,
            })}
          >
            <Popup>
              <div className="space-y-1">
                <p className="text-sm font-semibold">Line {vehicle.lineCode || '--'}</p>
                <p className="text-xs text-slate-600">{vehicle.name || 'Vehicle position'}</p>
                <p className="text-xs text-slate-600">{vehicle.typeLabel || 'Transit vehicle'}</p>
              </div>
            </Popup>
          </Marker>
        ))}

        {stops.map((stop) => {
          const isHighlighted = stop.id === highlightedStopId
          const stopColor = isHighlighted
            ? (stop.hoverColor || stop.color || '#c9c9c9')
            : (stop.color || '#e63946')

          if (stopStyle === 'dot') {
            return (
              <CircleMarker
                key={stop.id}
                center={[stop.latitude, stop.longitude]}
                radius={isHighlighted ? 8 : 5}
                pathOptions={{
                  color: '#ffffff',
                  weight: isHighlighted ? 2 : 1.5,
                  fillColor: stopColor,
                  fillOpacity: 1,
                  opacity: 1,
                }}
                eventHandlers={{
                  click: () => onStopClick && onStopClick(stop),
                  mouseover: () => onStopHover && onStopHover(stop),
                  mouseout: () => onStopHoverEnd && onStopHoverEnd(stop),
                }}
              >
                <Tooltip
                  direction="top"
                  offset={[0, -10]}
                  opacity={1}
                  permanent={isHighlighted}
                >
                  {stop.name}
                </Tooltip>
              </CircleMarker>
            )
          }

          return (
            <Marker 
              key={stop.id} 
              position={[stop.latitude, stop.longitude]} 
              icon={createIcon(
                stopColor,
                stop.name || 'Transit stop',
                zoom,
              )}
              eventHandlers={{
                click: () => onStopClick && onStopClick(stop),
                mouseover: () => onStopHover && onStopHover(stop),
                mouseout: () => onStopHoverEnd && onStopHoverEnd(stop),
              }}
            >
              <Popup>{stop.name}</Popup>
            </Marker>
          )
        })}

        {startPin ? (
          <Marker position={startPin} icon={createIcon('#198754', 'Start point', zoom)}>
            <Popup>Start</Popup>
          </Marker>
        ) : null}

        {endPin ? (
          <Marker position={endPin} icon={createIcon('#0d6efd', 'Destination point', zoom)}>
            <Popup>Destination</Popup>
          </Marker>
        ) : null}
      </MapContainer>
    </div>
  )
}

function MapViewportController({ focusPositions, focusKey }) {
  const map = useMap()

  useEffect(() => {
    if (!Array.isArray(focusPositions) || focusPositions.length === 0) return
    const validPositions = focusPositions.filter(
      (position) => Array.isArray(position) && position.length === 2,
    )
    if (validPositions.length === 0) return

    if (validPositions.length === 1) {
      map.setView(validPositions[0], Math.max(map.getZoom(), 15), { animate: true })
      return
    }

    const bounds = L.latLngBounds(validPositions)
    map.fitBounds(bounds, {
      animate: true,
      padding: [40, 40],
      maxZoom: 16,
    })
  }, [focusKey, focusPositions, map])

  return null
}

function createVehicleIcon({ color, headingDeg = 0, isMoving = false }) {
  const indicator = isMoving
    ? `<g transform="translate(11 11) rotate(${headingDeg})">
         <polygon points="0,-4.6 4.2,3 -4.2,3" fill="#ffffff" />
       </g>`
    : '<circle cx="11" cy="11" r="3" fill="#ffffff" />'

  return L.divIcon({
    className: 'vehicle-marker-icon',
    html: `<svg width="22" height="22" viewBox="0 0 22 22" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <circle cx="11" cy="11" r="10" fill="${color}" stroke="#ffffff" stroke-width="2" />
      ${indicator}
    </svg>`,
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  })
}

function MapEvents({ onMapClick, onZoomChange }) {
  useMapEvents({
    click(event) {
      if (onMapClick) {
        onMapClick(event.latlng)
      }
    },
    zoomend(event) {
      if (onZoomChange) {
        onZoomChange(event.target.getZoom())
      }
    },
  })
  return null
}
