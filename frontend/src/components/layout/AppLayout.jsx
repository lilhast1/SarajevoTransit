import {
  Bus,
  Car,
  ChevronRight,
  CircleUserRound,
  ClipboardList,
  X,
  LogIn,
  LayoutDashboard,
  MapPinned,
  Menu,
  Moon,
  Route,
  Sun,
  Table,
  Ticket,
  Truck,
  UserRound,
  Contrast,
} from 'lucide-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAppContext } from '../../context/AppContext'
import { SessionExpiryModal } from '../common/SessionExpiryModal'
import { NotificationBell } from '../common/NotificationBell'
import { LanguagePicker } from '../common/LanguagePicker'
import { getAuthSession } from '../../utils/authStorage'

function NavItem({ item, onClick }) {
  const Icon = item.icon
  return (
    <NavLink
      to={item.to}
      onClick={onClick}
      className={({ isActive }) =>
        `flex items-center justify-between gap-2 rounded-panel border px-3 py-2 text-sm font-medium transition ${
          isActive
          ? 'border-accent bg-accent text-white'
          : 'border-border text-muted hover:bg-surface-alt hover:text-ink'
        }`
      }
    >
      <span className="flex items-center gap-2">
        <Icon size={16} aria-hidden="true" />
        <span>{item.label}</span>
      </span>
      <ChevronRight size={14} aria-hidden="true" />
    </NavLink>
  )
}

export function AppLayout() {
  const { theme, toggleTheme, isAuthenticated, isAdmin, textSize, toggleTextSize, highContrast, toggleHighContrast } = useAppContext()
  const { t } = useTranslation('nav')
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const [session, setSession] = useState(() => getAuthSession())

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  const navItems = useMemo(() => {
    const items = [
      { to: '/', label: t('route_planner'), icon: Route },
      { to: '/lines', label: t('lines'), icon: Bus },
      { to: '/stops', label: t('stops'), icon: MapPinned },
      { to: '/timetable', label: t('timetable'), icon: Table },
      { to: '/vehicles', label: t('vehicles'), icon: Truck },
      { to: '/report', label: t('report'), icon: Bus },
      ...(session?.role && session.role !== 'ADMIN' ? [{ to: '/my-reports', label: t('my_reports'), icon: ClipboardList }] : []),
      { to: '/tickets', label: t('tickets'), icon: Ticket },
      { to: '/auth', label: t('auth'), icon: LogIn },
      { to: '/profile', label: t('profile'), icon: UserRound },
    ]
    if (session?.role === 'DRIVER') items.push({ to: '/driver', label: t('driver_portal'), icon: Car })
    if (isAdmin) items.push({ to: '/admin', label: t('admin_panel'), icon: LayoutDashboard })
    return items
  }, [session?.role, isAdmin, t])

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-border bg-surface/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-[1500px] items-center justify-between gap-3 px-4 py-3">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">{t('brand')}</p>
            <h1 className="text-lg font-semibold text-ink">{t('subtitle')}</h1>
          </div>

          <div className="flex items-center gap-2">
            <p className="hidden text-xs text-muted sm:block" aria-live="polite">
              {isAuthenticated ? t('signed_in') : t('guest')}
            </p>
            <button
              type="button"
              onClick={toggleTheme}
              className="rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
              aria-label={t('toggle_theme')}
            >
              {theme === 'dark' ? <Sun size={17} aria-hidden="true" /> : <Moon size={17} aria-hidden="true" />}
            </button>
            <LanguagePicker />
            <NotificationBell />
            <button
              type="button"
              className="rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
              onClick={() => setMenuOpen((value) => !value)}
              aria-label={t('toggle_menu')}
              aria-expanded={menuOpen}
              aria-controls="sidebar-nav"
            >
              {menuOpen ? <X size={18} aria-hidden="true" /> : <Menu size={18} aria-hidden="true" />}
            </button>
          </div>
        </div>
      </header>

      {menuOpen ? (
        <>
          <button
            type="button"
            aria-label={t('close_menu')}
            className="fixed inset-0 z-[1100] bg-black/35"
            onClick={() => setMenuOpen(false)}
          />
          <aside
            id="sidebar-nav"
            className="fixed right-0 top-0 z-[1200] h-full w-[min(360px,88vw)] overflow-y-auto border-l border-border bg-surface p-4 shadow-xl"
            aria-label="Navigation menu"
          >
            <div className="mb-4 flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2 text-ink">
                <CircleUserRound size={18} aria-hidden="true" />
                <span className="text-sm font-semibold">
                  {isAuthenticated ? (session?.role ? `${session.role}` : t('signed_in')) : t('guest_mode')}
                </span>
              </div>
              <button
                type="button"
                className="rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
                onClick={() => setMenuOpen(false)}
                aria-label={t('close_menu')}
              >
                <X size={16} aria-hidden="true" />
              </button>
            </div>

            <nav className="grid gap-2" aria-label="Main navigation">
              {navItems.map((item) => (
                <NavItem key={item.to} item={item} onClick={() => setMenuOpen(false)} />
              ))}
            </nav>

            <div className="mt-4 border-t border-border pt-4">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted">{t('accessibility')}</p>
              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={toggleTextSize}
                  className={`rounded-panel border px-3 py-1.5 text-sm font-bold transition ${
                    textSize === 'large'
                      ? 'border-accent bg-accent text-white'
                      : 'border-border text-muted hover:bg-surface-alt hover:text-ink'
                  }`}
                  aria-pressed={textSize === 'large'}
                  aria-label={t('text_size_large')}
                >
                  {t('text_size_normal')} / {t('text_size_large')}
                </button>
                <button
                  type="button"
                  onClick={toggleHighContrast}
                  className={`flex items-center gap-1.5 rounded-panel border px-3 py-1.5 text-sm font-medium transition ${
                    highContrast
                      ? 'border-accent bg-accent text-white'
                      : 'border-border text-muted hover:bg-surface-alt hover:text-ink'
                  }`}
                  aria-pressed={highContrast}
                  aria-label={t('high_contrast')}
                >
                  <Contrast size={14} aria-hidden="true" />
                  {t('high_contrast')}
                </button>
              </div>
            </div>
          </aside>
        </>
      ) : null}

      <main className="mx-auto w-full max-w-[1500px] px-4 py-4">
        <Outlet />
      </main>

      <SessionExpiryModal />
    </div>
  )
}
