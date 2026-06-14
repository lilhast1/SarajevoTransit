import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import {
  LayoutDashboard, AlertCircle, Star, Bus, MapPinned,
  Table2, Users, Bell, Clock, Truck, UserCog, Award,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { gatewayClient } from '../../services/gatewayClient'

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
  { to: '/admin/vehicles',      key: 'tab_fleet',         icon: Truck, badge: 'maintenanceAlerts' },
  { to: '/admin/drivers',       key: 'tab_drivers',       icon: UserCog },
  { to: '/admin/tiers',         key: 'tab_tiers',         icon: Award },
]

export function AdminLayout() {
  const { t } = useTranslation('admin-dashboard')
  const location = useLocation()
  const [maintenanceAlertCount, setMaintenanceAlertCount] = useState(0)

  useEffect(() => {
    gatewayClient.getMaintenanceAlerts()
      .then((alerts) => setMaintenanceAlertCount(Array.isArray(alerts) ? alerts.length : 0))
      .catch(() => {})
  }, [])

  const badges = { maintenanceAlerts: maintenanceAlertCount }

  return (
    <div className="space-y-4">
      {/* Tab bar */}
      <div className="border-b border-border overflow-x-auto">
        <nav className="inline-flex min-w-full gap-0.5 sm:gap-1" aria-label="Admin navigation">
          {TAB_DEFS.map(({ to, key, icon: Icon, exact, badge }) => {
            const isActive = exact
              ? location.pathname === to
              : location.pathname === to || location.pathname.startsWith(to + '/')
            const badgeCount = badge ? badges[badge] : 0
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
                {badgeCount > 0 && (
                  <span className="ml-0.5 rounded-full bg-red-500 px-1.5 py-0.5 text-[10px] font-bold text-white leading-none">
                    {badgeCount}
                  </span>
                )}
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
