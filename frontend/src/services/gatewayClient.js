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
    if (response.status === 401) {
      window.dispatchEvent(new CustomEvent('session-expired'))
    }
    throw new Error(await buildErrorMessage(response))
  }

  if (response.status === 204) return null
  return response.json()
}

export const gatewayClient = {
  // ── Auth / users ────────────────────────────────────────────────────────
  refreshAccessToken: (refreshToken) =>
    request('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),
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

  getStopLines: (stopId) => request(`/api/v1/routes/stop-lines?stopId=1:stop-${stopId}`),
  getStopDepartures: (stopId, limit = 5) => request(`/api/v1/routes/stop-departures?stopId=1:stop-${stopId}&limit=${limit}`),

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

  // ── Finance / Tickets ──────────────────────────────────────────────────
  /** POST /api/finance/purchase */
  purchaseTicket: (payload) =>
    request('/api/finance/purchase', {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  /** GET /api/finance/wallet/{userId}?page=0&size=20 */
  getWallet: (userId, query = '') =>
    request(`/api/finance/wallet/${userId}${query}`, { token: getAccessToken() }),
  /** GET /api/payments/methods/{userId} */
  getPaymentMethods: (userId) =>
    request(`/api/payments/methods/${userId}`, { token: getAccessToken() }),
  /** POST /api/payments/methods */
  addPaymentMethod: (payload) =>
    request('/api/payments/methods', {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  /** DELETE /api/payments/methods/{id} */
  removePaymentMethod: (methodId) =>
    request(`/api/payments/methods/${methodId}`, {
      method: 'DELETE',
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

  // ── Admin: Reports ────────────────────────────────────────────────────────
  getReports: (query = '') => request(`/api/v1/reports${query}`, { token: getAccessToken() }),
  getReportById: (id) => request(`/api/v1/reports/${id}`, { token: getAccessToken() }),
  updateReportStatus: (id, status) =>
    request(`/api/v1/reports/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
      token: getAccessToken(),
    }),
  deleteReport: (id) =>
    request(`/api/v1/reports/${id}`, { method: 'DELETE', token: getAccessToken() }),

  // ── Admin: Reviews ────────────────────────────────────────────────────────
  getReviews: (query = '') => request(`/api/v1/reviews${query}`, { token: getAccessToken() }),
  updateReviewModeration: (id, moderationStatus) =>
    request(`/api/v1/reviews/${id}/moderation-status`, {
      method: 'PATCH',
      body: JSON.stringify({ moderationStatus }),
      token: getAccessToken(),
    }),
  deleteReview: (id) =>
    request(`/api/v1/reviews/${id}`, { method: 'DELETE', token: getAccessToken() }),

  // ── Admin: Lines (writes) ─────────────────────────────────────────────────
  createLine: (payload) =>
    request('/api/v1/lines', {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  updateLine: (id, payload) =>
    request(`/api/v1/lines/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  deleteLine: (id) =>
    request(`/api/v1/lines/${id}`, { method: 'DELETE', token: getAccessToken() }),

  // ── Admin: Stations (writes) ──────────────────────────────────────────────
  createStation: (payload) =>
    request('/api/v1/stations', {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  updateStation: (id, payload) =>
    request(`/api/v1/stations/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  deleteStation: (id) =>
    request(`/api/v1/stations/${id}`, { method: 'DELETE', token: getAccessToken() }),

  // ── Admin: Timetables ────────────────────────────────────────────────────
  createTimetable: (payload) =>
    request('/api/v1/timetables', { method: 'POST', body: JSON.stringify(payload), token: getAccessToken() }),
  deleteTimetable: (id) =>
    request(`/api/v1/timetables/${id}`, { method: 'DELETE', token: getAccessToken() }),

  // ── Admin: Subscriptions ─────────────────────────────────────────────────
  getSubscriptionsByLine: (lineId, page = 0, size = 10) =>
    request(`/subscriptions/line/${lineId}?page=${page}&size=${size}&sort=createdAt,desc`, { token: getAccessToken() }),

  // ── Admin: Users ──────────────────────────────────────────────────────────
  getAllUsers: (query = '') => request(`/api/v1/users${query}`, { token: getAccessToken() }),
  getUserById: (id) => request(`/api/v1/users/${id}`, { token: getAccessToken() }),
  deleteUser: (id) =>
    request(`/api/v1/users/${id}`, { method: 'DELETE', token: getAccessToken() }),

  // ── Admin: Notifications ──────────────────────────────────────────────────
  getAllNotifications: (query = '') =>
    request(`/notifications${query}`, { token: getAccessToken() }),
  createNotification: (payload) =>
    request('/notifications', {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  broadcastNotification: (payload) =>
    request('/notifications/broadcast', {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  deleteNotification: (id) =>
    request(`/notifications/${id}`, { method: 'DELETE', token: getAccessToken() }),

  // ── User Notifications ──────────────────────────────────────────────────
  getUserNotifications: (userId, page = 0, size = 20) =>
    request(`/notifications/user/${userId}?page=${page}&size=${size}&sort=sentAt,desc`, { token: getAccessToken() }),
  getUnreadCount: (userId) =>
    request(`/notifications/user/${userId}/unread/count`, { token: getAccessToken() }),
  getUnreadNotifications: (userId) =>
    request(`/notifications/user/${userId}/unread`, { token: getAccessToken() }),
  markAsRead: (id) =>
    request(`/notifications/${id}/read`, { method: 'PATCH', token: getAccessToken() }),
  markAllAsRead: (userId) =>
    request(`/notifications/user/${userId}/read-all`, { method: 'PATCH', token: getAccessToken() }),

  // ── Subscriptions (Notification Service) ─────────────────────────────────
  createSubscription: (payload) =>
    request('/subscriptions', {
      method: 'POST',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  deactivateSubscription: (id) =>
    request(`/subscriptions/${id}/deactivate`, {
      method: 'PATCH',
      token: getAccessToken(),
    }),
  activateSubscription: (id) =>
    request(`/subscriptions/${id}/activate`, {
      method: 'PATCH',
      token: getAccessToken(),
    }),
  updateSubscription: (id, payload) =>
    request(`/subscriptions/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
      token: getAccessToken(),
    }),
  deleteSubscription: (id) =>
    request(`/subscriptions/${id}`, { method: 'DELETE', token: getAccessToken() }),
  getUserSubscriptions: (userId) =>
    request(`/subscriptions/user/${userId}/active`, { token: getAccessToken() }),
  getAllUserSubscriptions: (userId) =>
    request(`/subscriptions/user/${userId}`, { token: getAccessToken() }),
  getSubscriptionById: (id) =>
    request(`/subscriptions/${id}`, { token: getAccessToken() }),
}
