/**
 * Auth Storage Utilities
 * Handles JWT token persistence and retrieval with security best practices
 */

const STORAGE_KEY = 'sarajevo-transit-auth'

/**
 * Store authentication session (including JWT token)
 */
export function saveAuthSession(session) {
  if (!session) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

/**
 * Retrieve stored authentication session
 */
export function getAuthSession() {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (!stored) return null
  try {
    return JSON.parse(stored)
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

/**
 * Clear authentication session
 */
export function clearAuthSession() {
  localStorage.removeItem(STORAGE_KEY)
}

/**
 * Get the current access token (for use in API headers)
 */
export function getAccessToken() {
  const session = getAuthSession()
  return session?.accessToken || null
}

/**
 * Check if token is expired (simple client-side check)
 * Note: This is not cryptographically secure, just a UX optimization
 */
export function isTokenExpired(session) {
  if (!session?.expiresIn) return false
  const storedTime = session._storedAt || Date.now()
  const expirationTime = storedTime + session.expiresIn * 1000
  return Date.now() > expirationTime
}

/**
 * Add metadata to session for expiration tracking
 */
export function enrichSessionWithMetadata(session) {
  return {
    ...session,
    _storedAt: Date.now(),
  }
}
