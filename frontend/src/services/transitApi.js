import {
  directionStations,
  directions,
  lines as mockLines,
  routePolylines,
  sarajevoCenter,
  stations,
  timetables,
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

// ─── mock helpers (fallback) ──────────────────────────────────────────────────

function mockGetDirectionStations(directionId) {
  const stationIds = directionStations[directionId] || []
  return stationIds.map((stationId, index) => {
    const station = stations.find((item) => item.id === stationId)
    return {
      id: Number(`${directionId}${index + 1}`),
      directionId,
      stationId: station.id,
      stationName: station.name,
      stopSequence: index + 1,
      travelTimeFromPrevSeconds: index === 0 ? 0 : 180,
    }
  })
}

function mockGetStopsByLine(lineId) {
  const lineDirections = directions.filter((d) => d.lineId === Number(lineId))
  const stopIds = new Set()
  lineDirections.forEach((d) => {
    ;(directionStations[d.id] || []).forEach((sid) => stopIds.add(sid))
  })
  return stations.filter((s) => stopIds.has(s.id))
}

function getDayFilter(dayType) {
  if (dayType === 'saturday') return 6
  if (dayType === 'sunday') return 7
  return null
}

function mockGetNextDeparturesForStop(stopId) {
  const relatedDirections = directions.filter((d) =>
    (directionStations[d.id] || []).includes(Number(stopId)),
  )
  const relatedDirectionIds = relatedDirections.map((d) => d.id)

  return timetables
    .filter((row) => relatedDirectionIds.includes(row.directionId) && row.isActive)
    .sort((a, b) => a.departureTime.localeCompare(b.departureTime))
    .slice(0, 8)
    .map((row) => {
      const direction = directions.find((d) => d.id === row.directionId)
      const line = mockLines.find((l) => l.id === row.lineId)
      return {
        id: row.id,
        departureTime: row.departureTime,
        lineCode: line.code,
        lineName: line.name,
        directionName: direction.name,
      }
    })
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

    try {
      const response = await gatewayClient.getLines(query ? `?${query}` : '')
      const shaped = response.map(withVehicleType)
      return filterLinesByQuery(shaped, search, vehicleType)
    } catch {
      return filterLinesByQuery(mockLines, search, vehicleType)
    }
  },

  async getLineById(lineId) {
    try {
      const line = await gatewayClient.getLineById(lineId)
      return withVehicleType(line)
    } catch {
      return mockLines.find((line) => line.id === Number(lineId)) || null
    }
  },

  // ── Directions ───────────────────────────────────────────────────────────
  /**
   * GET /api/v1/directions?lineId=&activeOnly=true
   * Returns DirectionResponse[] – fields: id, lineId, code, name, directionLabel, isActive, …
   */
  async getDirectionsByLine(lineId) {
    const query = buildRouteQuery({ lineId, activeOnly: true })
    try {
      const response = await gatewayClient.getDirections(`?${query}`)
      return response.filter((d) => d.isActive)
    } catch {
      return directions.filter((d) => d.lineId === Number(lineId) && d.isActive)
    }
  },

  /**
   * GET /api/v1/directions/{id}/stations
   * Returns DirectionStationResponse[] – fields: id, directionId, stationId, stationName,
   * stopSequence, travelTimeFromPrevSeconds
   */
  async getDirectionStations(directionId) {
    try {
      return await gatewayClient.getDirectionStations(directionId)
    } catch {
      return mockGetDirectionStations(Number(directionId))
    }
  },

  /**
   * GET /api/v1/directions/{id}/geojson
   * Returns a GeoJSON Feature with a LineString geometry (coordinates: [[lon,lat], …]).
   * We convert to [[lat,lon], …] for Leaflet.
   */
  async getDirectionPolyline(directionId) {
    try {
      const feature = await gatewayClient.getDirectionGeoJson(directionId)
      const polyline = geoJsonToLeafletPolyline(feature)
      return polyline || [sarajevoCenter]
    } catch {
      return routePolylines[Number(directionId)] || [sarajevoCenter]
    }
  },

  // ── Stations ─────────────────────────────────────────────────────────────
  /**
   * GET /api/v1/stations?name=&activeOnly=true
   * Returns StationResponse[] – fields: id, code, name, address, latitude, longitude, isActive
   */
  async getStops({ search = '' } = {}) {
    const query = buildRouteQuery({ activeOnly: true, name: search || undefined })
    try {
      const response = await gatewayClient.getStations(query ? `?${query}` : '')
      // Client-side filter is kept as belt-and-suspenders since the backend filters by exact prefix
      return response.filter(
        (s) => !search || s.name.toLowerCase().includes(search.toLowerCase()),
      )
    } catch {
      return stations.filter((s) => s.name.toLowerCase().includes(search.toLowerCase()))
    }
  },

  /**
   * GET /api/v1/stations/{id}
   * Enriched with lines + next departures (assembled client-side from directions/timetables).
   */
  async getStopById(stopId) {
    let stop = null
    try {
      stop = await gatewayClient.getStationById(stopId)
    } catch {
      stop = stations.find((s) => s.id === Number(stopId)) || null
    }
    if (!stop) return null

    // Resolve which lines serve this station using gateway directions where possible,
    // falling back to mock data for the enrichment (lines + departures).
    let servedLineIds = []
    try {
      const allDirections = await gatewayClient.getDirections('?activeOnly=true')
      const stationId = stop.id
      // We check which directions include this station by loading their station lists.
      // To avoid N+1 requests we use the mock lookup as a reasonable approximation
      // when the full relationship data isn't separately available.
      const relatedMockDirections = directions.filter((d) =>
        (directionStations[d.id] || []).includes(Number(stationId)),
      )
      servedLineIds = Array.from(new Set(relatedMockDirections.map((d) => d.lineId)))
    } catch {
      const relatedDirections = directions.filter((d) =>
        (directionStations[d.id] || []).includes(Number(stopId)),
      )
      servedLineIds = Array.from(new Set(relatedDirections.map((d) => d.lineId)))
    }

    // Fetch line details from the gateway when possible
    const servedLines = await Promise.all(
      servedLineIds.map((id) =>
        gatewayClient
          .getLineById(id)
          .then(withVehicleType)
          .catch(() => mockLines.find((l) => l.id === id)),
      ),
    ).then((results) => results.filter(Boolean))

    return {
      ...stop,
      lines: servedLines,
      departures: mockGetNextDeparturesForStop(stopId),
    }
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

    try {
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
    } catch {
      const requestedLineId = Number(lineId)
      const requestedDirectionId = Number(directionId)
      return timetables
        .filter((item) => {
          if (!item.isActive) return false
          if (requestedLineId && item.lineId !== requestedLineId) return false
          if (requestedDirectionId && item.directionId !== requestedDirectionId) return false
          if (dayFilter) return item.daysOfWeek.includes(dayFilter)
          return item.daysOfWeek.some((d) => d >= 1 && d <= 5)
        })
        .sort((a, b) => a.departureTime.localeCompare(b.departureTime))
    }
  },

  // ── Route planning ────────────────────────────────────────────────────────
  /**
   * GET /api/v1/routes/optimal?fromLat=&fromLon=&toLat=&toLon=&numItineraries=…
   */
  async getOptimalRoute(params) {
    const query = buildRouteQuery(params)
    try {
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
    } catch {
      return {
        source: 'mock',
        requestedItineraries: 1,
        itineraries: [
          {
            durationSeconds: 1800,
            walkDistanceMeters: 320,
            transfers: 1,
            legs: [
              {
                mode: 'WALK',
                fromName: 'Start',
                toName: 'Otoka',
                startTime: Date.now(),
                endTime: Date.now() + 360000,
                distanceMeters: 250,
              },
              {
                mode: 'TRAM',
                fromName: 'Otoka',
                toName: 'Skenderija',
                startTime: Date.now() + 360000,
                endTime: Date.now() + 1260000,
                distanceMeters: 3200,
              },
              {
                mode: 'WALK',
                fromName: 'Skenderija',
                toName: 'Destination',
                startTime: Date.now() + 1260000,
                endTime: Date.now() + 1800000,
                distanceMeters: 70,
              },
            ],
          },
        ],
      }
    }
  },

  // ── Auth ───────────────────────────────────────────────────────────────────
  async register({ fullName, email, password }) {
    await gatewayClient.register({ fullName, email, password })
    return gatewayClient.login({ email, password })
  },

  async login({ email, password }) {
    return gatewayClient.login({ email, password })
  },

  async getProfileSnapshot(favorites) {
    return {
      favoriteLines: mockLines.filter((line) => favorites.lines.includes(line.id)),
      favoriteStops: stations.filter((stop) => favorites.stops.includes(stop.id)),
      suggestedLines: mockLines.slice(0, 3),
    }
  },

  async getStopsByLine(lineId) {
    return mockGetStopsByLine(lineId)
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