import { Navigate } from 'react-router-dom'
import { useAppContext } from '../context/AppContext'

export function AdminRoute({ children }) {
  const { isAuthenticated, isAdmin } = useAppContext()
  if (!isAuthenticated) return <Navigate to="/auth" replace />
  if (!isAdmin) return <Navigate to="/" replace />
  return children
}
