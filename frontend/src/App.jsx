import { Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout.jsx'
import { ProtectedRoute } from './hooks/useAuth.jsx'
import { AuthPage } from './pages/AuthPage.jsx'
import { LineDetailPage } from './pages/LineDetailPage.jsx'
import { LinesPage } from './pages/LinesPage.jsx'
import { NotFoundPage } from './pages/NotFoundPage.jsx'
import { ProfilePage } from './pages/ProfilePage.jsx'
import { ReportProblemPage } from './pages/ReportProblemPage.jsx'
import { RoutePlannerPage } from './pages/RoutePlannerPage.jsx'
import { StopDetailPage } from './pages/StopDetailPage.jsx'
import { StopsPage } from './pages/StopsPage.jsx'
import { TimetablePage } from './pages/TimetablePage.jsx'
import { VehiclesPage } from './pages/VehiclesPage.jsx'

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
        <Route path="/vehicles" element={<VehiclesPage />} />
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

        <Route
          path="/report"
          element={
            <ProtectedRoute>
              <ReportProblemPage />
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
