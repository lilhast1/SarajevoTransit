import { Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout.jsx'
import { ProtectedRoute } from './hooks/useAuth.jsx'
import { AdminRoute } from './hooks/useAdmin.jsx'
import { AuthPage } from './pages/AuthPage.jsx'
import { LineDetailPage } from './pages/LineDetailPage.jsx'
import { LinesPage } from './pages/LinesPage.jsx'
import { NotFoundPage } from './pages/NotFoundPage.jsx'
import { NotificationsPage } from './pages/NotificationsPage.jsx'
import { ProfilePage } from './pages/ProfilePage.jsx'
import { ReportProblemPage } from './pages/ReportProblemPage.jsx'
import { RoutePlannerPage } from './pages/RoutePlannerPage.jsx'
import { StopDetailPage } from './pages/StopDetailPage.jsx'
import { StopsPage } from './pages/StopsPage.jsx'
import { TimetablePage } from './pages/TimetablePage.jsx'
import { VehiclesPage } from './pages/VehiclesPage.jsx'
import { VehicleDetailPage } from './pages/VehicleDetailPage.jsx'
import { DriverPage } from './pages/DriverPage.jsx'
import { TicketsPage } from './pages/TicketsPage.jsx'

import { MyReportsPage } from './pages/MyReportsPage.jsx'
import { MyReportDetailPage } from './pages/MyReportDetailPage.jsx'
import { AdminLayout } from './components/layout/AdminLayout.jsx'
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage.jsx'
import { AdminReportsPage } from './pages/admin/AdminReportsPage.jsx'
import { AdminReportDetailPage } from './pages/admin/AdminReportDetailPage.jsx'
import { AdminReviewsPage } from './pages/admin/AdminReviewsPage.jsx'
import { AdminLinesPage } from './pages/admin/AdminLinesPage.jsx'
import { AdminStationsPage } from './pages/admin/AdminStationsPage.jsx'
import { AdminTimetablePage } from './pages/admin/AdminTimetablePage.jsx'
import { AdminUsersPage } from './pages/admin/AdminUsersPage.jsx'
import { AdminNotificationsPage } from './pages/admin/AdminNotificationsPage.jsx'
import { AdminDelaysPage } from './pages/admin/AdminDelaysPage.jsx'
import { AdminVehiclesPage } from './pages/admin/AdminVehiclesPage.jsx'
import { AdminDriversPage } from './pages/admin/AdminDriversPage.jsx'
import { AdminTiersPage } from './pages/admin/AdminTiersPage.jsx'


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
        <Route path="/vehicles/:id" element={<VehicleDetailPage />} />
        <Route path="/driver" element={<DriverPage />} />
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
          path="/notifications"
          element={
            <ProtectedRoute>
              <NotificationsPage />
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

        <Route
          path="/my-reports"
          element={
            <ProtectedRoute>
              <MyReportsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/my-reports/:id"
          element={
            <ProtectedRoute>
              <MyReportDetailPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/tickets"
          element={
            <ProtectedRoute>
              <TicketsPage />
            </ProtectedRoute>
          }
        />

        {/* Admin routes - require ADMIN role */}
        <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="reports" element={<AdminReportsPage />} />
          <Route path="reports/:id" element={<AdminReportDetailPage />} />
          <Route path="reviews" element={<AdminReviewsPage />} />
          <Route path="lines" element={<AdminLinesPage />} />
          <Route path="stations" element={<AdminStationsPage />} />
          <Route path="timetables" element={<AdminTimetablePage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="notifications" element={<AdminNotificationsPage />} />
          <Route path="delays" element={<AdminDelaysPage />} />
          <Route path="vehicles" element={<AdminVehiclesPage />} />
          <Route path="drivers" element={<AdminDriversPage />} />
          <Route path="tiers" element={<AdminTiersPage />} />
        </Route>

        {/* 404 fallback */}
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
