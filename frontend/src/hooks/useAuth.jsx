/**
 * Custom hooks for authentication and routing
 */

import { useAppContext } from '../context/AppContext'
import { Navigate } from 'react-router-dom'

/**
 * Hook to check if user is authenticated
 * Returns { isAuthenticated, isLoading, user }
 */
export function useAuth() {
  const { isAuthenticated, session } = useAppContext()
  return {
    isAuthenticated,
    isLoading: false,
    user: session,
  }
}

/**
 * Component that protects a route - redirects to auth if not authenticated
 * Usage: <ProtectedRoute><YourPage /></ProtectedRoute>
 */
export function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace />
  }

  return children
}
