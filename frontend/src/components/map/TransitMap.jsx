import { useMemo, useState } from 'react'
import { MapContainer, Marker, Polyline, Popup, TileLayer, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { sarajevoCenter } from '../../data/mockTransitData'

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

  return L.icon({
    iconUrl: `data:image/svg+xml;base64,${btoa(markerSvg(color, label))}`,
    iconSize: [width, height],
    iconAnchor: [width / 2, height],
    popupAnchor: [0, -height],
  })
}

export function TransitMap({
  polyline = [],
  polylines = [], // [{ positions, color }]
  stops = [],
  startPin,
  endPin,
  className = 'h-80',
  highlight = '#e63946',
  onMapClick,
}) {
  const [zoom, setZoom] = useState(13)
  const center = useMemo(() => {
    if (polylines.length > 0 && polylines[0].positions.length > 0) return polylines[0].positions[0]
    if (polyline.length > 0) return polyline[0]
    if (stops.length > 0) return [stops[0].latitude, stops[0].longitude]
    return sarajevoCenter
  }, [polyline, polylines, stops])

  return (
    <div className={`relative overflow-hidden ${className}`}>
      <MapContainer center={center} zoom={13} scrollWheelZoom className="z-10">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <MapEvents onMapClick={onMapClick} onZoomChange={setZoom} />

        {polyline.length > 1 ? <Polyline positions={polyline} pathOptions={{ color: highlight, weight: 5 }} /> : null}

        {polylines.map((pl, idx) => (
          pl.positions && pl.positions.length > 1 ? (
            <Polyline 
              key={idx} 
              positions={pl.positions} 
              pathOptions={{ color: pl.color || highlight, weight: 6, opacity: 0.8 }} 
            >
              {pl.label && <Popup>{pl.label}</Popup>}
            </Polyline>
          ) : null
        ))}

        {stops.map((stop) => (
          <Marker 
            key={stop.id} 
            position={[stop.latitude, stop.longitude]} 
            icon={createIcon('#e63946', 'Transit stop', zoom)}
          >
            <Popup>{stop.name}</Popup>
          </Marker>
        ))}

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
