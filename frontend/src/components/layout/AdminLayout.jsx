import { NavLink, Outlet, useLocation } from 'react-router-dom'
import {
  LayoutDashboard, AlertCircle, Star, Bus, MapPinned,
  Table2, Users, Bell, Clock,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'

const TAB_DEFS = [
  { to: '/admin',               key: 'tab_dashboard',     icon: LayoutDashboard, exact: true },
  { to: '/admin/reports',       key: 'tab_reports',       icon: AlertCircle },
  { to: '/admin/reviews',       key: 'tab_reviews',       icon: Star },
  { to: '/admin/lines',         key: 'tab_lines',         icon: Bus },
  { to: '/admin/stations',      key: 'tab_stations',      icon: MapPinned },
  { to: '/admin/timetables',    key: 'tab_timetables',    icon: Table2 },
  { to: '/admin/users',         key: 'tab_users',         icon: Users },
  { to: '/admin/notifications', key: 'tab_notifications', icon: Bell },
  { to: '/admin/delays',        key: 'tab_delays',        icon: Clock },
]

export function AdminLayout() {
  const { t } = useTranslation('admin-dashboard')
  const location = useLocation()

  return (
    <div className="space-y-4">
      {/* Tab bar */}
      <div className="border-b border-border overflow-x-auto">
        <nav className="inline-flex min-w-full gap-0.5 sm:gap-1" aria-label="Admin navigation">
          {TAB_DEFS.map(({ to, key, icon: Icon, exact }) => {
            const isActive = exact
              ? location.pathname === to
              : location.pathname === to || location.pathname.startsWith(to + '/')
            return (
              <NavLink
                key={to}
                to={to}
                className={`inline-flex shrink-0 items-center gap-1.5 border-b-2 px-3 py-2.5 text-sm font-medium whitespace-nowrap transition ${
                  isActive
                    ? 'border-accent text-ink'
                    : 'border-transparent text-muted hover:border-border hover:text-ink'
                }`}
              >
                <Icon size={14} aria-hidden />
                <span>{t(key)}</span>
              </NavLink>
            )
          })}
        </nav>
      </div>

      {/* Page content */}
      <Outlet />
    </div>
  )
}
