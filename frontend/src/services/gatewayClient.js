const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed with status ${response.status}`)
  }

  if (response.status === 204) return null
  return response.json()
}

export const gatewayClient = {
  // ── Lines ──────────────────────────────────────────────────────────────
  getLines: (query = '') => request(`/api/v1/lines${query}`),
  getLineById: (lineId) => request(`/api/v1/lines/${lineId}`),

  // ── Directions ─────────────────────────────────────────────────────────
  /** GET /api/v1/directions?lineId=&activeOnly= */
  getDirections: (query = '') => request(`/api/v1/directions${query}`),
  /** GET /api/v1/directions/{id}/stations */
  getDirectionStations: (directionId) => request(`/api/v1/directions/${directionId}/stations`),
  /** GET /api/v1/directions/{id}/geojson  →  GeoJSON Feature with LineString geometry */
  getDirectionGeoJson: (directionId) => request(`/api/v1/directions/${directionId}/geojson`),

  // ── Stations ───────────────────────────────────────────────────────────
  /** GET /api/v1/stations?name=&activeOnly= */
  getStations: (query = '') => request(`/api/v1/stations${query}`),
  /** GET /api/v1/stations/{id} */
  getStationById: (stationId) => request(`/api/v1/stations/${stationId}`),

  // ── Timetables ─────────────────────────────────────────────────────────
  /** GET /api/v1/timetables?lineId=&directionId=&activeOnly= */
  getTimetables: (query = '') => request(`/api/v1/timetables${query}`),

  // ── Route planning ─────────────────────────────────────────────────────
  /** GET /api/v1/routes/optimal?fromLat=&fromLon=&toLat=&toLon=&… */
  getOptimalRoute: (query) => request(`/api/v1/routes/optimal?${query}`),
}
