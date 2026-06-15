import { useEffect, useState, useCallback } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, AlertCircle, Star, Bus, MapPinned,
  Table2, Users, Bell, Clock, Truck, UserCog, Award, RefreshCw, AlertTriangle,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { gatewayClient } from '../../services/gatewayClient'

const TAB_DEFS = [
  { to: '/admin',               key: 'tab_dashboard',     icon: LayoutDashboard, exact: true, badge: 'needsRebuild' },
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
  const [needsRebuild, setNeedsRebuild] = useState(false)

  const fetchAlerts = useCallback(async () => {
    try {
      const alerts = await gatewayClient.getMaintenanceAlerts()
      setMaintenanceAlertCount(Array.isArray(alerts) ? alerts.length : 0)
    } catch {
      // silently ignore
    }
  }, [])

  const fetchRebuildState = useCallback(async () => {
    try {
      const state = await gatewayClient.getOtpRebuildState()
      setNeedsRebuild(!!state?.needsRebuild)
    } catch {
      // silently ignore
    }
  }, [])

  useEffect(() => {
    fetchAlerts()
    fetchRebuildState()
  }, [fetchAlerts, fetchRebuildState])

  useEffect(() => {
    const interval = setInterval(fetchRebuildState, 30_000)
    return () => clearInterval(interval)
  }, [fetchRebuildState])

  const badges = {
    maintenanceAlerts: maintenanceAlertCount,
    needsRebuild: needsRebuild ? 1 : 0,
  }

  return (
    <div className="space-y-5">
      {needsRebuild && (
        <div className="flex items-center justify-between gap-3 rounded-panel border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-200">
          <div className="flex items-center gap-2">
            <AlertTriangle size={16} className="shrink-0" />
            <span className="font-medium">OTP graph needs rebuild</span>
            <span className="text-amber-600 dark:text-amber-400">&mdash; routing data has changed</span>
          </div>
          <NavLink
            to="/admin"
            className="shrink-0 rounded-panel border border-amber-400 bg-amber-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-amber-600 dark:border-amber-600 dark:bg-amber-700 dark:hover:bg-amber-600"
          >
            View &amp; Schedule
          </NavLink>
        </div>
      )}

      <div className="overflow-x-auto pb-1">
        <nav className="inline-flex min-w-full gap-0.5 border-b border-border pb-0 sm:gap-1" aria-label="Admin navigation">
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
                <Icon size={15} aria-hidden />
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

      <Outlet />
    </div>
  )
}
