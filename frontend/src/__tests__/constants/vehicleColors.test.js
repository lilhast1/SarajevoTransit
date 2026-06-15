import { describe, it, expect } from 'vitest'
import {
  withAlpha,
  getVehicleTypeMetaByName,
  getColorForLegMode,
  getColorForLegRouteType,
  VEHICLE_TYPE_META_BY_ID,
} from '../../constants/vehicleColors'

describe('withAlpha', () => {
  it('converts hex to rgba', () => {
    expect(withAlpha('#ff0000', 0.5)).toBe('rgba(255, 0, 0, 0.5)')
  })

  it('converts 3-digit hex', () => {
    expect(withAlpha('#f00', 1)).toBe('rgba(255, 0, 0, 1)')
  })

  it('clamps alpha above 1', () => {
    expect(withAlpha('#ffffff', 2)).toBe('rgba(255, 255, 255, 1)')
  })

  it('clamps alpha below 0', () => {
    expect(withAlpha('#000000', -1)).toBe('rgba(0, 0, 0, 0)')
  })

  it('returns input for invalid hex', () => {
    expect(withAlpha('not-a-color', 0.5)).toBe('not-a-color')
  })
})

describe('getVehicleTypeMetaByName', () => {
  it('returns meta for bus', () => {
    const meta = getVehicleTypeMetaByName('bus')
    expect(meta).toBeDefined()
    expect(meta.key).toBe('bus')
    expect(meta.color).toBe(VEHICLE_TYPE_META_BY_ID[2].color)
  })

  it('returns meta for tram', () => {
    const meta = getVehicleTypeMetaByName('tram')
    expect(meta).toBeDefined()
    expect(meta.key).toBe('tram')
  })

  it('is case-insensitive', () => {
    expect(getVehicleTypeMetaByName('BUS')).toEqual(getVehicleTypeMetaByName('bus'))
  })

  it('returns null for unknown type', () => {
    expect(getVehicleTypeMetaByName('spaceship')).toBeNull()
  })

  it('returns null for null input', () => {
    expect(getVehicleTypeMetaByName(null)).toBeNull()
  })
})

describe('getColorForLegMode', () => {
  it('returns a color for BUS', () => {
    const color = getColorForLegMode('BUS')
    expect(color).toBeTruthy()
    expect(color).toBe(VEHICLE_TYPE_META_BY_ID[2].color)
  })

  it('returns a color for WALK', () => {
    expect(getColorForLegMode('WALK')).toBe('#94a3b8')
  })

  it('returns transit color for unknown mode', () => {
    expect(getColorForLegMode('HELICOPTER')).toBeTruthy()
  })

  it('handles null gracefully', () => {
    expect(getColorForLegMode(null)).toBeTruthy()
  })
})

describe('getColorForLegRouteType', () => {
  it('returns bus color for route type 3', () => {
    expect(getColorForLegRouteType(3)).toBe(VEHICLE_TYPE_META_BY_ID[2].color)
  })

  it('falls back to mode color for unknown route type', () => {
    expect(getColorForLegRouteType(999, 'BUS')).toBe(getColorForLegMode('BUS'))
  })

  it('falls back to mode when routeType is null', () => {
    expect(getColorForLegRouteType(null, 'WALK')).toBe(getColorForLegMode('WALK'))
  })
})
