import { describe, it, expect } from 'vitest'
import { ApiError, getErrorMessage } from '../../utils/apiErrors'

describe('ApiError', () => {
  it('sets isNetworkError for status 0', () => {
    const e = new ApiError('fail', 0)
    expect(e.isNetworkError).toBe(true)
    expect(e.isAuthError).toBe(false)
    expect(e.isServerError).toBe(false)
  })

  it('sets isNetworkError for status NETWORK', () => {
    const e = new ApiError('fail', 'NETWORK')
    expect(e.isNetworkError).toBe(true)
  })

  it('sets isAuthError for 401', () => {
    const e = new ApiError('unauth', 401)
    expect(e.isAuthError).toBe(true)
    expect(e.isClientError).toBe(true)
  })

  it('sets isAuthError for 403', () => {
    const e = new ApiError('forbidden', 403)
    expect(e.isAuthError).toBe(true)
  })

  it('sets isServerError for 500', () => {
    const e = new ApiError('server fail', 500)
    expect(e.isServerError).toBe(true)
    expect(e.isClientError).toBe(false)
  })

  it('sets isClientError for 400', () => {
    const e = new ApiError('bad request', 400)
    expect(e.isClientError).toBe(true)
    expect(e.isServerError).toBe(false)
  })

  it('sets isClientError for 404', () => {
    const e = new ApiError('not found', 404)
    expect(e.isClientError).toBe(true)
  })

  it('sets message correctly', () => {
    const e = new ApiError('test message', 400)
    expect(e.message).toBe('test message')
  })
})

describe('getErrorMessage', () => {
  it('returns network message for network errors', () => {
    const e = new ApiError('', 0)
    expect(getErrorMessage(e)).toMatch(/network/i)
  })

  it('returns auth message for auth errors', () => {
    const e = new ApiError('', 401)
    expect(getErrorMessage(e)).toMatch(/authentication/i)
  })

  it('returns server message for server errors', () => {
    const e = new ApiError('', 500)
    expect(getErrorMessage(e)).toMatch(/server/i)
  })

  it('returns error message for client errors', () => {
    const e = new ApiError('Invalid input', 400)
    expect(getErrorMessage(e)).toBe('Invalid input')
  })

  it('returns fallback for client error with no message', () => {
    const e = new ApiError('', 400)
    expect(getErrorMessage(e)).toBeTruthy()
  })
})
