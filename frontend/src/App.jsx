import { Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout.jsx'
import { ProtectedRoute } from './hooks/useAuth.jsx'
import { AuthPage } from './pages/AuthPage.jsx'
import { LineDetailPage } from './pages/LineDetailPage.jsx'
import { LinesPage } from './pages/LinesPage.jsx'
import { NotFoundPage } from './pages/NotFoundPage.jsx'
import { ProfilePage } from './pages/ProfilePage.jsx'
import { RoutePlannerPage } from './pages/RoutePlannerPage.jsx'
import { StopDetailPage } from './pages/StopDetailPage.jsx'
import { StopsPage } from './pages/StopsPage.jsx'
import { TimetablePage } from './pages/TimetablePage.jsx'

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        {/* Public routes */}
        <Route index element={<RoutePlannerPage />} />
        <Route path="/route-planner" element={<RoutePlannerPage />} />
        <Route path="/lines" element={<LinesPage />} />
        <Route path="/lines/:id" element={<LineDetailPage />} />
        <Route path="/stops" element={<StopsPage />} />
        <Route path="/stops/:id" element={<StopDetailPage />} />
        <Route path="/timetable" element={<TimetablePage />} />
        <Route path="/auth" element={<AuthPage />} />

        {/* Protected routes - require authentication */}
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />

        {/* 404 fallback */}
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
