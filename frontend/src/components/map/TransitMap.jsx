import { useMemo } from 'react'
import { MapContainer, Marker, Polyline, Popup, TileLayer } from 'react-leaflet'
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

const markerIcon = L.icon({
  iconUrl: `data:image/svg+xml;base64,${btoa(markerSvg('#e63946', 'Transit stop'))}`,
  iconSize: [38, 50],
  iconAnchor: [19, 50],
})

const startIcon = L.icon({
  iconUrl: `data:image/svg+xml;base64,${btoa(markerSvg('#198754', 'Start point'))}`,
  iconSize: [38, 50],
  iconAnchor: [19, 50],
})

const endIcon = L.icon({
  iconUrl: `data:image/svg+xml;base64,${btoa(markerSvg('#0d6efd', 'Destination point'))}`,
  iconSize: [38, 50],
  iconAnchor: [19, 50],
})

export function TransitMap({
  polyline = [],
  stops = [],
  startPin,
  endPin,
  className = 'h-80',
  highlight = '#e63946',
}) {
  const center = useMemo(() => {
    if (polyline.length > 0) return polyline[0]
    if (stops.length > 0) return [stops[0].latitude, stops[0].longitude]
    return sarajevoCenter
  }, [polyline, stops])

  return (
    <div className={`relative overflow-hidden ${className}`}>
      <MapContainer center={center} zoom={13} scrollWheelZoom className="z-10">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {polyline.length > 1 ? <Polyline positions={polyline} color={highlight} weight={5} /> : null}

        {stops.map((stop) => (
          <Marker key={stop.id} position={[stop.latitude, stop.longitude]} icon={markerIcon}>
            <Popup>{stop.name}</Popup>
          </Marker>
        ))}

        {startPin ? (
          <Marker position={startPin} icon={startIcon}>
            <Popup>Start</Popup>
          </Marker>
        ) : null}

        {endPin ? (
          <Marker position={endPin} icon={endIcon}>
            <Popup>Destination</Popup>
          </Marker>
        ) : null}
      </MapContainer>
    </div>
  )
}
