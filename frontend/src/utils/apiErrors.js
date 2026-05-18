/**
 * API Error Handling
 * Provides consistent error messages and categorization
 */

export class ApiError extends Error {
  constructor(message, status, originalError) {
    super(message)
    this.status = status
    this.originalError = originalError
    this.isNetworkError = status === 0 || status === 'NETWORK'
    this.isAuthError = status === 401 || status === 403
    this.isServerError = status >= 500
    this.isClientError = status >= 400 && status < 500
  }
}

/**
 * Classify and format API error for user display
 */
export function getErrorMessage(error) {
  if (error.isNetworkError) {
    return 'Network error. Please check your connection or try again.'
  }
  if (error.isAuthError) {
    return 'Authentication failed. Please login again.'
  }
  if (error.isServerError) {
    return 'Server error. Please try again later.'
  }
  if (error.isClientError) {
    // Check if there's a specific message from the server
    return error.message || 'Invalid request. Please check your input.'
  }
  return error.message || 'An unexpected error occurred.'
}

/**
 * Wrap API calls with error handling
 */
export async function withErrorHandling(apiCall, fallbackValue = null) {
  try {
    return await apiCall()
  } catch (error) {
    console.error('API Error:', error)
    if (fallbackValue !== null) {
      return fallbackValue
    }
    throw error
  }
}
