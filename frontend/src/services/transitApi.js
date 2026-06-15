import {
  sarajevoCenter,
  vehicleTypes,
} from '../data/mockTransitData'
import { gatewayClient } from './gatewayClient'
import { VEHICLE_TYPE_META_BY_ID, getVehicleTypeMetaByName } from '../constants/vehicleColors'

const vehicleTypeById = VEHICLE_TYPE_META_BY_ID

// ─── helpers ──────────────────────────────────────────────────────────────────

function withVehicleType(line) {
  if (line.vehicleTypeName) return line
  const type = vehicleTypes.find((item) => item.id === line.vehicleTypeId)
  return { ...line, vehicleTypeName: type?.name || 'bus' }
}

function filterLinesByQuery(items, search, vehicleType) {
  return items.filter((line) => {
    const matchesSearch =
      !search || `${line.code} ${line.name}`.toLowerCase().includes(search.toLowerCase())
    const matchesVehicle = !vehicleType || line.vehicleTypeName === vehicleType
    return matchesSearch && matchesVehicle
  })
}

function buildRouteQuery(params) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') query.set(key, value)
  })
  return query.toString()
}

function getDayFilter(dayType) {
  if (dayType === 'saturday') return 6
  if (dayType === 'sunday') return 7
  return null
}

// ─── GeoJSON → Leaflet polyline conversion ────────────────────────────────────
// The backend returns GeoJSON [lon, lat] pairs; Leaflet expects [lat, lon].
function geoJsonToLeafletPolyline(feature) {
  const coords = feature?.geometry?.coordinates
  if (!Array.isArray(coords) || coords.length === 0) return null
  return coords.map(([lon, lat]) => [Number(lat), Number(lon)])
}

function decodePolyline(encoded) {
  if (!encoded) return []
  const poly = []
  let index = 0, len = encoded.length
  let lat = 0, lng = 0
  while (index < len) {
    let b, shift = 0, result = 0
    do {
      b = encoded.charCodeAt(index++) - 63
      result |= (b & 0x1f) << shift
      shift += 5
    } while (b >= 0x20)
    const dlat = ((result & 1) ? ~(result >> 1) : (result >> 1))
    lat += dlat
    shift = 0
    result = 0
    do {
      b = encoded.charCodeAt(index++) - 63
      result |= (b & 0x1f) << shift
      shift += 5
    } while (b >= 0x20)
    const dlng = ((result & 1) ? ~(result >> 1) : (result >> 1))
    lng += dlng
    poly.push([lat / 1e5, lng / 1e5])
  }
  return poly
}

function normalizeNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function extractCoordinates(item) {
  const latitude =
    normalizeNumber(item?.latitude) ??
    normalizeNumber(item?.lat) ??
    normalizeNumber(item?.gpsLatitude) ??
    normalizeNumber(item?.y) ??
    normalizeNumber(item?.position?.latitude) ??
    normalizeNumber(item?.position?.lat)

  const longitude =
    normalizeNumber(item?.longitude) ??
    normalizeNumber(item?.lon) ??
    normalizeNumber(item?.lng) ??
    normalizeNumber(item?.gpsLongitude) ??
    normalizeNumber(item?.x) ??
    normalizeNumber(item?.position?.longitude) ??
    normalizeNumber(item?.position?.lon) ??
    normalizeNumber(item?.position?.lng)

  if (latitude === null || longitude === null) return null
  return { latitude, longitude }
}

function resolveTypeInfo(item, requestedTypeIds = []) {
  const rawType =
    normalizeNumber(item?.vehicleTypeId) ??
    normalizeNumber(item?.vehicle_type_id) ??
    normalizeNumber(item?.typeId) ??
    normalizeNumber(item?.vehicleType?.id) ??
    normalizeNumber(item?.line?.vehicleTypeId)

  if (rawType && vehicleTypeById[rawType]) return vehicleTypeById[rawType]

  const rawTypeName = String(item?.vehicleType || item?.type || '').toLowerCase().trim()
  if (rawTypeName) {
    const matched = getVehicleTypeMetaByName(rawTypeName)
    if (matched) return matched
  }

  if (requestedTypeIds.length === 1 && vehicleTypeById[requestedTypeIds[0]]) {
    return vehicleTypeById[requestedTypeIds[0]]
  }

  return {
    id: 0,
    key: 'transit',
    label: 'Transit',
    color: '#6366f1',
  }
}

function normalizeVehiclePosition(item, fallbackId, typeInfo) {
  const coords = extractCoordinates(item)
  if (!coords) return null

  return {
    id: item?.id || item?.vehicleId || item?.vehicle_id || `${typeInfo.id}-${fallbackId}`,
    lineCode:
      String(
        item?.lineCode || item?.line?.code || item?.code || item?.vehicle_code || item?.vehicleCode || '',
      ).trim() || '--',
    name:
      String(
        item?.lineName || item?.line?.name || item?.name || item?.vehicle_name || item?.vehicleName || item?.direction || '',
      ).trim() || undefined,
    latitude: coords.latitude,
    longitude: coords.longitude,
    typeId: typeInfo.id,
    type: typeInfo.key,
    typeLabel: typeInfo.label,
    color: typeInfo.color,
    heading: normalizeNumber(item?.heading) ?? normalizeNumber(item?.bearing) ?? undefined,
    speed: normalizeNumber(item?.speed) ?? undefined,
    raw: item,
  }
}

async function fetchVehiclePositionsByTypes(typeIds) {
  const csvTypes = typeIds.join(',')
  const response = await fetch(`/jp-api/api/lines/vehicle/positions/${csvTypes}`, {
    headers: {
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    throw new Error(`Vehicle positions request failed (${response.status})`)
  }

  if (response.status === 204) return []
  const payload = await response.json()
  if (!Array.isArray(payload)) return []

  return payload
    .map((item, index) => normalizeVehiclePosition(item, index, resolveTypeInfo(item, typeIds)))
    .filter(Boolean)
}

// ─── API ──────────────────────────────────────────────────────────────────────

export const transitApi = {
  // ── Lines ────────────────────────────────────────────────────────────────
  async getLines({ search = '', vehicleType = '', activeOnly = true } = {}) {
    const query = buildRouteQuery({
      activeOnly,
      vehicleTypeId: vehicleTypes.find((type) => type.name === vehicleType)?.id,
    })

    const response = await gatewayClient.getLines(query ? `?${query}` : '')
    const shaped = response.map(withVehicleType)
    return filterLinesByQuery(shaped, search, vehicleType)
  },

  async getLineById(lineId) {
    const line = await gatewayClient.getLineById(lineId)
    return withVehicleType(line)
  },

  // ── Directions ───────────────────────────────────────────────────────────
  /**
   * GET /api/v1/directions?lineId=&activeOnly=true
   * Returns DirectionResponse[] – fields: id, lineId, code, name, directionLabel, isActive, …
   */
  async getDirectionsByLine(lineId) {
    const query = buildRouteQuery({ lineId, activeOnly: true })
    const response = await gatewayClient.getDirections(`?${query}`)
    return response.filter((d) => d.isActive)
  },

  /**
   * GET /api/v1/directions/{id}/stations
   * Returns DirectionStationResponse[] – fields: id, directionId, stationId, stationName,
   * stopSequence, travelTimeFromPrevSeconds
   */
  async getDirectionStations(directionId) {
    return gatewayClient.getDirectionStations(directionId)
  },

  /**
   * GET /api/v1/directions/{id}/geojson
   * Returns a GeoJSON Feature with a LineString geometry (coordinates: [[lon,lat], …]).
   * We convert to [[lat,lon], …] for Leaflet.
   */
  async getDirectionPolyline(directionId) {
    const feature = await gatewayClient.getDirectionGeoJson(directionId)
    const polyline = geoJsonToLeafletPolyline(feature)
    return polyline || [sarajevoCenter]
  },

  // ── Stations ─────────────────────────────────────────────────────────────
  /**
   * GET /api/v1/stations?name=&activeOnly=true
   * Returns StationResponse[] – fields: id, code, name, address, latitude, longitude, isActive
   */
  async getStops({ search = '' } = {}) {
    const query = buildRouteQuery({ activeOnly: true, name: search || undefined })
    const response = await gatewayClient.getStations(query ? `?${query}` : '')
    // Client-side filter is kept as belt-and-suspenders since the backend filters by exact prefix
    return response.filter(
      (s) => !search || s.name.toLowerCase().includes(search.toLowerCase()),
    )
  },

  /**
   * GET /api/v1/stations/{id}
   * Enriched with lines + next departures (assembled client-side from directions/timetables).
   */
  async getStopById(stopId) {
    const stop = await gatewayClient.getStationById(stopId)
    if (!stop) return null

    let lines = []
    let departures = []

    try {
      const linesResponse = await gatewayClient.getStopLines(stopId)
      if (linesResponse && linesResponse.lines) {
        lines = linesResponse.lines.map((l, idx) => {
          return {
            id: l.shortName || idx,
            gtfsId: Number(l.gtfsId.split('-')[1]) || idx,
            code: l.shortName || '?',
            name: l.longName || l.shortName || 'Unknown',
            vehicleTypeName: l.mode ? l.mode.toLowerCase() : 'bus',
          }
        })
      }
    } catch (e) {
      console.warn('Failed to fetch stop lines from GraphQL proxy:', e)
    }

    try {
      departures = await this.getStopDepartures(stopId, 5)
    } catch (e) {
      console.warn('Failed to fetch stop departures', e)
    }

    return {
      ...stop,
      lines,
      departures,
    }
  },

  async getStopDepartures(stopId, limit = 5) {
    const depResponse = await gatewayClient.getStopDepartures(stopId, limit)
    if (!depResponse?.departures) return []

    return depResponse.departures.map((d, idx) => {
      const totalSeconds = d.realtimeDeparture ?? d.scheduledDeparture ?? 0
      const hours = Math.floor(totalSeconds / 3600) % 24
      const minutes = Math.floor((totalSeconds % 3600) / 60)
      const timeStr = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`
      const gtfsId = Number(d.gtfsId.split('-')[1])

      return {
        id: `${d.lineShortName}-${totalSeconds}-${idx}`,
        gtfsId: gtfsId,
        departureTime: timeStr,
        lineCode: d.lineShortName || '?',
        lineName: d.headsign || d.lineShortName,
        directionName: d.headsign || 'Unknown',
      }
    })
  },

  // ── Timetables ───────────────────────────────────────────────────────────
  /**
   * GET /api/v1/timetables?lineId=&directionId=&activeOnly=true
   * Returns TimetableResponse[] – fields: id, lineId, directionId, departureTime (LocalTime),
   * daysOfWeek (List<Short> 1-7 Mon-Sun), isActive, …
   *
   * The dayType is mapped to an integer weekday filter:
   *   weekday → 1-5, saturday → 6, sunday → 7
   */
  async getTimetable({ lineId, directionId, dayType = 'weekday' }) {
    const dayFilter = getDayFilter(dayType)
    const query = buildRouteQuery({ lineId, directionId, activeOnly: true })

    const response = await gatewayClient.getTimetables(query ? `?${query}` : '')
    return response
      .filter((item) => {
        if (!item.isActive) return false
        const days = item.daysOfWeek || []
        if (dayFilter) return days.includes(dayFilter)
        return days.some((d) => d >= 1 && d <= 5)
      })
      .sort((a, b) => {
        // departureTime comes back as "HH:mm:ss" or "HH:mm" from Spring LocalTime serialisation
        return String(a.departureTime).localeCompare(String(b.departureTime))
      })
  },

  // ── Route planning ────────────────────────────────────────────────────────
  /**
   * GET /api/v1/routes/optimal?fromLat=&fromLon=&toLat=&toLon=&numItineraries=…
   */
  async getOptimalRoute(params) {
    const query = buildRouteQuery(params)
    const response = await gatewayClient.getOptimalRoute(query)
    // Enrich legs with decoded polylines
    if (response.itineraries) {
      response.itineraries.forEach((itinerary) => {
        if (itinerary.legs) {
          itinerary.legs.forEach((leg) => {
            leg.path = decodePolyline(leg.points)
          })
        }
      })
    }
    return response
  },

  // ── Auth ───────────────────────────────────────────────────────────────────
  async register({ fullName, email, password }) {
    await gatewayClient.register({ fullName, email, password })
    return gatewayClient.login({ email, password })
  },

  async login({ email, password }) {
    return gatewayClient.login({ email, password })
  },

  // ── Subscriptions (Notification Service) ────────────────────────────────
  async subscribeToLine({ userId, lineId, lineCode, lineName, startInterval, endInterval, daysOfWeek }) {
    return gatewayClient.createSubscription({
      userId,
      lineId,
      lineCode,
      lineName,
      startInterval,
      endInterval,
      daysOfWeek,
    })
  },

  async unsubscribeFromLine(subscriptionId) {
    return gatewayClient.deactivateSubscription(subscriptionId)
  },

  async reactivateSubscription(subscriptionId) {
    return gatewayClient.activateSubscription(subscriptionId)
  },

  async updateSubscription(subscriptionId, data) {
    return gatewayClient.updateSubscription(subscriptionId, data)
  },

  async deleteSubscription(subscriptionId) {
    return gatewayClient.deleteSubscription(subscriptionId)
  },

  async getUserSubscriptions(userId) {
    return gatewayClient.getUserSubscriptions(userId)
  },

  async getAllUserSubscriptions(userId) {
    return gatewayClient.getAllUserSubscriptions(userId)
  },

  // ── User Profile ─────────────────────────────────────────────────────────
  async getUserProfile(userId) {
    return gatewayClient.getUserById(userId)
  },

  async updateUserProfile(userId, data) {
    return gatewayClient.updateUserProfile(userId, data)
  },

  async getUserPreferences(userId) {
    return gatewayClient.getUserPreferences(userId)
  },

  async updateUserPreferences(userId, data) {
    return gatewayClient.updateUserPreferences(userId, data)
  },

  async updateUserPassword(userId, data) {
    return gatewayClient.updateUserPassword(userId, data)
  },

  async getUserTravelHistory(userId, query = '') {
    return gatewayClient.getUserTravelHistory(userId, query)
  },

  async getUserSummary(userId) {
    return gatewayClient.getUserSummary(userId)
  },

  async generateUserLoyaltyCoupon(userId, data) {
    return gatewayClient.generateUserLoyaltyCoupon(userId, data)
  },

  async getUserLoyaltyBalance(userId) {
    return gatewayClient.getUserLoyaltyBalance(userId)
  },

  async getUserLoyaltyTransactions(userId, query = '') {
    return gatewayClient.getUserLoyaltyTransactions(userId, query)
  },

  async redeemUserLoyaltyPoints(userId, data) {
    return gatewayClient.redeemUserLoyaltyPoints(userId, data)
  },

  // ── Reviews ───────────────────────────────────────────────────────────────
  async createReview(payload) {
    return gatewayClient.createReview(payload)
  },

  async getReviewsByLine(lineId, page = 0) {
    return gatewayClient.getReviewsByLine(lineId, page)
  },

  async getReviewSummary(lineId) {
    return gatewayClient.getReviewSummary(lineId)
  },

  async getUserReviews(userId) {
    return gatewayClient.getUserReviews(userId)
  },

  async getSubscriptionById(id) {
    return gatewayClient.getSubscriptionById(id)
  },

  // ── User Notifications ─────────────────────────────────────────────────
  async getUserNotifications(userId, page) {
    return gatewayClient.getUserNotifications(userId, page)
  },

  async getUnreadCount(userId) {
    return gatewayClient.getUnreadCount(userId)
  },

  async getUnreadNotifications(userId) {
    return gatewayClient.getUnreadNotifications(userId)
  },

  async markAsRead(id) {
    return gatewayClient.markAsRead(id)
  },

  async markAllAsRead(userId) {
    return gatewayClient.markAllAsRead(userId)
  },

  async getProfileSnapshot(favorites) {
    const [favoriteLines, favoriteStops, suggestedLines] = await Promise.all([
      Promise.all((favorites.lines || []).map((lineId) => this.getLineById(lineId).catch(() => null))),
      Promise.all((favorites.stops || []).map((stopId) => gatewayClient.getStationById(stopId).catch(() => null))),
      this.getLines({ activeOnly: true }).then((items) => items.slice(0, 3)),
    ])

    return {
      favoriteLines: favoriteLines.filter(Boolean),
      favoriteStops: favoriteStops.filter(Boolean),
      suggestedLines,
    }
  },

  async getStopsByLine(lineId) {
    const directionItems = await this.getDirectionsByLine(lineId)
    if (!directionItems.length) return []

    const directionStops = await Promise.all(
      directionItems.map((direction) => this.getDirectionStations(direction.id).catch(() => [])),
    )

    const byStationId = new Map()
    directionStops.flat().forEach((stop) => {
      if (!byStationId.has(stop.stationId)) {
        byStationId.set(stop.stationId, {
          id: stop.stationId,
          name: stop.stationName,
        })
      }
    })

    return Array.from(byStationId.values())
  },

  async getVehiclePositions(vehicleTypeIds = [1, 2, 3, 4]) {
    const validTypeIds = Array.from(
      new Set(
        vehicleTypeIds
          .map((item) => Number(item))
          .filter((item) => Number.isFinite(item) && vehicleTypeById[item]),
      ),
    )

    if (validTypeIds.length === 0) return []
    return fetchVehiclePositionsByTypes(validTypeIds)
  },
}
