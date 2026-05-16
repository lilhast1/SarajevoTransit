export const VEHICLE_TYPE_META_BY_ID = {
  1: { id: 1, key: 'minibus', label: 'Minibus', color: '#f97316' },
  2: { id: 2, key: 'bus', label: 'Bus', color: '#3b82f6' },
  3: { id: 3, key: 'trolleybus', label: 'Trolleybus', color: '#10b981' },
  4: { id: 4, key: 'tram', label: 'Tram', color: '#e63946' },
}

export const VEHICLE_TYPE_META_BY_KEY = Object.values(VEHICLE_TYPE_META_BY_ID).reduce((acc, item) => {
  acc[item.key] = item
  return acc
}, {})

const LEG_MODE_COLORS = {
  WALK: '#94a3b8',
  TRAM: VEHICLE_TYPE_META_BY_KEY.tram.color,
  BUS: VEHICLE_TYPE_META_BY_KEY.bus.color,
  TROLLEYBUS: VEHICLE_TYPE_META_BY_KEY.trolleybus.color,
  MINIBUS: VEHICLE_TYPE_META_BY_KEY.minibus.color,
  SUBWAY: '#8b5cf6',
  RAIL: '#6366f1',
  CABLE_CAR: '#ec4899',
  FERRY: '#06b6d4',
  TRANSIT: '#6366f1',
}

export function getVehicleTypeMetaByName(name) {
  if (!name) return null
  return VEHICLE_TYPE_META_BY_KEY[String(name).toLowerCase().trim()] || null
}

export function getColorForLegMode(mode) {
  const normalized = String(mode || '').toUpperCase().trim()
  if (!normalized) return LEG_MODE_COLORS.TRANSIT
  if (LEG_MODE_COLORS[normalized]) return LEG_MODE_COLORS[normalized]
  if (normalized.includes('TROLLEY')) return LEG_MODE_COLORS.TROLLEYBUS
  if (normalized.includes('TRAM')) return LEG_MODE_COLORS.TRAM
  if (normalized.includes('MINIBUS')) return LEG_MODE_COLORS.MINIBUS
  if (normalized.includes('BUS')) return LEG_MODE_COLORS.BUS
  if (normalized.includes('WALK')) return LEG_MODE_COLORS.WALK
  return LEG_MODE_COLORS.TRANSIT
}

export function withAlpha(hexColor, alpha) {
  const clean = String(hexColor || '').replace('#', '')
  const normalized = clean.length === 3
    ? clean.split('').map((char) => `${char}${char}`).join('')
    : clean

  if (!/^[0-9a-fA-F]{6}$/.test(normalized)) return hexColor

  const r = parseInt(normalized.slice(0, 2), 16)
  const g = parseInt(normalized.slice(2, 4), 16)
  const b = parseInt(normalized.slice(4, 6), 16)
  const clampedAlpha = Math.max(0, Math.min(1, Number(alpha)))

  return `rgba(${r}, ${g}, ${b}, ${clampedAlpha})`
}
