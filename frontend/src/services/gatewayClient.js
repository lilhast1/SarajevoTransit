import { getAccessToken } from '../utils/authStorage'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

async function buildErrorMessage(response) {
  const contentType = response.headers.get('content-type') || ''

  if (contentType.includes('application/json')) {
    try {
      const payload = await response.json()
      if (Array.isArray(payload?.validationErrors) && payload.validationErrors.length > 0) {
        return payload.validationErrors.join(', ')
      }
      if (typeof payload?.message === 'string' && payload.message.trim().length > 0) {
        return payload.message
      }
    } catch {
      return `Request failed with status ${response.status}`
    }
  }

  try {
    const text = await response.text()
    if (text && text.trim().length > 0) return text
  } catch {
    return `Request failed with status ${response.status}`
  }

  return `Request failed with status ${response.status}`
}

async function request(path, options = {}) {
  const { token, headers: customHeaders, ...restOptions } = options

  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(customHeaders || {}),
    },
    ...restOptions,
  })

  if (!response.ok) {
    throw new Error(await buildErrorMessage(response))
  }

  if (response.status === 204) return null
  return response.json()
}

export const gatewayClient = {
  // ── Auth / users ────────────────────────────────────────────────────────
  login: (payload) =>
    request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  register: (payload) =>
    request('/api/v1/users', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

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

  /** GET /api/v1/users/me */
  getCurrentUser: () => request('/api/v1/users/me'),

  // ── Vehicles ──────────────────────────────────────────────────────────
  /** GET /api/vehicles?size=200 */
  getVehicles: (query = '') => request(`/api/vehicles${query}`),
  /** GET /api/vehicles/{id} */
  getVehicleById: (vehicleId) => request(`/api/vehicles/${vehicleId}`),
  /** POST /api/vehicles */
  addVehicle: (payload) =>
    request('/api/vehicles', { method: 'POST', body: JSON.stringify(payload), token: getAccessToken() }),
  /** PATCH /api/vehicles/{id}/status */
  updateVehicleStatus: (vehicleId, status) =>
    request(`/api/vehicles/${vehicleId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
      token: getAccessToken(),
    }),
  /** GET /api/vehicles/{id}/service-records */
  getVehicleServiceRecords: (vehicleId) => request(`/api/vehicles/${vehicleId}/service-records`),
  /** POST /api/vehicles/{id}/service-records */
  addServiceRecord: (vehicleId, payload) =>
    request(`/api/vehicles/${vehicleId}/service-records`, {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  /** GET /api/vehicles/{id}/location/history?from=ISO&to=ISO */
  getVehicleLocationHistory: (vehicleId, from, to) =>
    request(
      `/api/vehicles/${vehicleId}/location/history?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
    ),
  /** POST /api/vehicles/{vehicleId}/status/requests */
  createStatusRequest: (vehicleId, payload) =>
    request(`/api/vehicles/${vehicleId}/status/requests`, {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  /** GET /api/vehicles/{vehicleId}/status/requests?status=PENDING */
  getVehicleStatusRequests: (vehicleId, status) =>
    request(`/api/vehicles/${vehicleId}/status/requests${status ? `?status=${status}` : ''}`),
  /** GET /api/vehicles/status/requests?requestedByUserId={userId} */
  getMyStatusRequests: (userId) =>
    request(`/api/vehicles/status/requests?requestedByUserId=${userId}`),
  /** PATCH /api/vehicles/status/requests/{requestId}/resolve */
  resolveStatusRequest: (requestId, payload) =>
    request(`/api/vehicles/status/requests/${requestId}/resolve`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),

  // ── Problem reports ───────────────────────────────────────────────────
  /** POST /api/v1/reports */
  createProblemReport: (payload) =>
    request('/api/v1/reports', {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  /** POST /api/v1/auth/logout */
  logout: () => request('/api/v1/auth/logout', { method: 'POST' }),
}
