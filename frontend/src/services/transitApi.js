import {
  directionStations,
  directions,
  lines as mockLines,
  mockUsers,
  routePolylines,
  sarajevoCenter,
  stations,
  timetables,
  vehicleTypes,
} from '../data/mockTransitData'
import { gatewayClient } from './gatewayClient'

const isGatewayOnly = true

function withVehicleType(line) {
  if (line.vehicleTypeName) return line
  const type = vehicleTypes.find((item) => item.id === line.vehicleTypeId)
  return { ...line, vehicleTypeName: type?.name || 'bus' }
}

function filterLinesByQuery(items, search, vehicleType) {
  return items.filter((line) => {
    const matchesSearch = !search
      || `${line.code} ${line.name}`.toLowerCase().includes(search.toLowerCase())
    const matchesVehicle = !vehicleType || line.vehicleTypeName === vehicleType
    return matchesSearch && matchesVehicle
  })
}

function getDirectionStations(directionId) {
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

function getStopsByLine(lineId) {
  const lineDirections = directions.filter((direction) => direction.lineId === Number(lineId))
  const stopIds = new Set()

  lineDirections.forEach((direction) => {
    ;(directionStations[direction.id] || []).forEach((stationId) => stopIds.add(stationId))
  })

  return stations.filter((station) => stopIds.has(station.id))
}

function getDayFilter(dayType) {
  if (dayType === 'saturday') return 6
  if (dayType === 'sunday') return 7
  return null
}

function getNextDeparturesForStop(stopId) {
  const relatedDirections = directions.filter((direction) =>
    (directionStations[direction.id] || []).includes(Number(stopId)),
  )

  const relatedDirectionIds = relatedDirections.map((direction) => direction.id)

  return timetables
    .filter((row) => relatedDirectionIds.includes(row.directionId) && row.isActive)
    .sort((a, b) => a.departureTime.localeCompare(b.departureTime))
    .slice(0, 8)
    .map((row) => {
      const direction = directions.find((item) => item.id === row.directionId)
      const line = mockLines.find((item) => item.id === row.lineId)
      return {
        id: row.id,
        departureTime: row.departureTime,
        lineCode: line.code,
        lineName: line.name,
        directionName: direction.name,
      }
    })
}

function buildRouteQuery(params) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') query.set(key, value)
  })
  return query.toString()
}

export const transitApi = {
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

  async getDirectionsByLine(lineId) {
    return directions.filter((direction) => direction.lineId === Number(lineId) && direction.isActive)
  },

  async getDirectionStations(directionId) {
    return getDirectionStations(Number(directionId))
  },

  async getDirectionPolyline(directionId) {
    return routePolylines[Number(directionId)] || [sarajevoCenter]
  },

  async getStops({ search = '' } = {}) {
    return stations.filter((stop) => stop.name.toLowerCase().includes(search.toLowerCase()))
  },

  async getStopById(stopId) {
    const stop = stations.find((item) => item.id === Number(stopId))
    if (!stop) return null

    const servedDirections = directions.filter((direction) =>
      (directionStations[direction.id] || []).includes(stop.id),
    )

    const servedLineIds = Array.from(new Set(servedDirections.map((direction) => direction.lineId)))

    return {
      ...stop,
      lines: mockLines.filter((line) => servedLineIds.includes(line.id)),
      departures: getNextDeparturesForStop(stop.id),
    }
  },

  async getTimetable({ lineId, directionId, dayType = 'weekday' }) {
    const requestedLineId = Number(lineId)
    const requestedDirectionId = Number(directionId)
    const dayFilter = getDayFilter(dayType)

    return timetables
      .filter((item) => {
        if (!item.isActive) return false
        if (requestedLineId && item.lineId !== requestedLineId) return false
        if (requestedDirectionId && item.directionId !== requestedDirectionId) return false
        if (dayFilter) return item.daysOfWeek.includes(dayFilter)
        return item.daysOfWeek.some((day) => day >= 1 && day <= 5)
      })
      .sort((a, b) => a.departureTime.localeCompare(b.departureTime))
  },

  async getOptimalRoute(params) {
    const query = buildRouteQuery(params)
    try {
      return await gatewayClient.getOptimalRoute(query)
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

  async register({ fullName, email, password }) {
    const existing = mockUsers.find((user) => user.email.toLowerCase() === email.toLowerCase())
    if (existing) throw new Error('Email already in use')

    const nextId = mockUsers.length + 1
    mockUsers.push({ id: nextId, fullName, email, password })

    return {
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: nextId,
      email,
      fullName,
      role: 'USER',
      gatewayOnly: isGatewayOnly,
    }
  },

  async login({ email, password }) {
    const user = mockUsers.find(
      (candidate) =>
        candidate.email.toLowerCase() === email.toLowerCase() && candidate.password === password,
    )

    if (!user) throw new Error('Invalid email or password')

    return {
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: user.id,
      email: user.email,
      fullName: user.fullName,
      role: 'USER',
      gatewayOnly: isGatewayOnly,
    }
  },

  async getProfileSnapshot(favorites) {
    return {
      favoriteLines: mockLines.filter((line) => favorites.lines.includes(line.id)),
      favoriteStops: stations.filter((stop) => favorites.stops.includes(stop.id)),
      suggestedLines: mockLines.slice(0, 3),
    }
  },

  async getStopsByLine(lineId) {
    return getStopsByLine(lineId)
  },
}
