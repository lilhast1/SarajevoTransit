import { describe, it, expect } from 'vitest'
import {
  formatDurationFromSeconds,
  formatDepartureFromTimestamp,
  prettyVehicleType,
} from '../../utils/formatters'

describe('formatDurationFromSeconds', () => {
  it('returns N/A for null', () => {
    expect(formatDurationFromSeconds(null)).toBe('N/A')
  })

  it('returns N/A for NaN', () => {
    expect(formatDurationFromSeconds(NaN)).toBe('N/A')
  })

  it('returns N/A for 0', () => {
    expect(formatDurationFromSeconds(0)).toBe('N/A')
  })

  it('formats under 60 minutes', () => {
    expect(formatDurationFromSeconds(600)).toBe('10 min')
  })

  it('formats exactly 1 hour', () => {
    expect(formatDurationFromSeconds(3600)).toBe('1h')
  })

  it('formats hours and minutes', () => {
    expect(formatDurationFromSeconds(5400)).toBe('1h 30m')
  })

  it('formats multiple hours', () => {
    expect(formatDurationFromSeconds(7200)).toBe('2h')
  })
})

describe('formatDepartureFromTimestamp', () => {
  it('returns --:-- for null', () => {
    expect(formatDepartureFromTimestamp(null)).toBe('--:--')
  })

  it('returns --:-- for undefined', () => {
    expect(formatDepartureFromTimestamp(undefined)).toBe('--:--')
  })

  it('returns a time string for a valid timestamp', () => {
    const result = formatDepartureFromTimestamp(new Date('2024-01-15T14:30:00').getTime())
    expect(result).toMatch(/\d{1,2}:\d{2}/)
  })
})

describe('prettyVehicleType', () => {
  it('returns "line" for null', () => {
    expect(prettyVehicleType(null)).toBe('line')
  })

  it('returns "line" for undefined', () => {
    expect(prettyVehicleType(undefined)).toBe('line')
  })

  it('capitalizes "bus"', () => {
    expect(prettyVehicleType('bus')).toBe('Bus')
  })

  it('capitalizes "tram"', () => {
    expect(prettyVehicleType('tram')).toBe('Tram')
  })

  it('capitalizes already-upper types', () => {
    expect(prettyVehicleType('BUS')).toBe('BUS')
  })
})
