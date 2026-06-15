import { describe, it, expect, beforeEach } from 'vitest'
import {
  saveAuthSession,
  getAuthSession,
  clearAuthSession,
  getAccessToken,
  isTokenExpired,
  enrichSessionWithMetadata,
  secondsUntilExpiry,
} from '../../utils/authStorage'

const STORAGE_KEY = 'sarajevo-transit-auth'

beforeEach(() => {
  localStorage.clear()
})

describe('saveAuthSession', () => {
  it('stores session as JSON', () => {
    const session = { accessToken: 'abc', role: 'USER' }
    saveAuthSession(session)
    expect(localStorage.getItem(STORAGE_KEY)).toBe(JSON.stringify(session))
  })

  it('removes key when called with null', () => {
    localStorage.setItem(STORAGE_KEY, '{}')
    saveAuthSession(null)
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })
})

describe('getAuthSession', () => {
  it('returns null when nothing stored', () => {
    expect(getAuthSession()).toBeNull()
  })

  it('returns parsed session', () => {
    const session = { accessToken: 'tok123' }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    expect(getAuthSession()).toEqual(session)
  })

  it('returns null and clears storage for corrupt JSON', () => {
    localStorage.setItem(STORAGE_KEY, 'not-json{')
    expect(getAuthSession()).toBeNull()
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })
})

describe('clearAuthSession', () => {
  it('removes the stored session', () => {
    localStorage.setItem(STORAGE_KEY, '{}')
    clearAuthSession()
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })
})

describe('getAccessToken', () => {
  it('returns null when no session', () => {
    expect(getAccessToken()).toBeNull()
  })

  it('returns accessToken from session', () => {
    saveAuthSession({ accessToken: 'my-token' })
    expect(getAccessToken()).toBe('my-token')
  })

  it('returns null when session has no accessToken', () => {
    saveAuthSession({ role: 'USER' })
    expect(getAccessToken()).toBeNull()
  })
})

describe('isTokenExpired', () => {
  it('returns false when session has no expiresIn', () => {
    expect(isTokenExpired({ accessToken: 'tok' })).toBe(false)
  })

  it('returns false when session is null', () => {
    expect(isTokenExpired(null)).toBe(false)
  })

  it('returns true when token is past expiry', () => {
    const session = {
      accessToken: 'tok',
      expiresIn: 3600,
      _storedAt: Date.now() - 7200 * 1000,
    }
    expect(isTokenExpired(session)).toBe(true)
  })

  it('returns false when token is still valid', () => {
    const session = {
      accessToken: 'tok',
      expiresIn: 3600,
      _storedAt: Date.now(),
    }
    expect(isTokenExpired(session)).toBe(false)
  })
})

describe('enrichSessionWithMetadata', () => {
  it('adds _storedAt timestamp', () => {
    const before = Date.now()
    const result = enrichSessionWithMetadata({ accessToken: 'tok' })
    expect(result._storedAt).toBeGreaterThanOrEqual(before)
    expect(result.accessToken).toBe('tok')
  })
})

describe('secondsUntilExpiry', () => {
  it('returns null when session is missing fields', () => {
    expect(secondsUntilExpiry(null)).toBeNull()
    expect(secondsUntilExpiry({ expiresIn: 3600 })).toBeNull()
    expect(secondsUntilExpiry({ _storedAt: Date.now() })).toBeNull()
  })

  it('returns positive seconds for a valid future session', () => {
    const session = { expiresIn: 3600, _storedAt: Date.now() }
    const secs = secondsUntilExpiry(session)
    expect(secs).toBeGreaterThan(3500)
    expect(secs).toBeLessThanOrEqual(3600)
  })

  it('returns negative seconds for an expired session', () => {
    const session = { expiresIn: 3600, _storedAt: Date.now() - 7200 * 1000 }
    expect(secondsUntilExpiry(session)).toBeLessThan(0)
  })
})
