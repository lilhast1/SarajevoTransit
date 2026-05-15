import {
  Bus,
  ChevronRight,
  CircleUserRound,
  X,
  LogIn,
  MapPinned,
  Menu,
  Moon,
  Route,
  Sun,
  Table,
  UserRound,
} from 'lucide-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAppContext } from '../../context/AppContext'

const navItems = [
  { to: '/', label: 'Route Planner', icon: Route },
  { to: '/lines', label: 'Lines', icon: Bus },
  { to: '/stops', label: 'Stops', icon: MapPinned },
  { to: '/timetable', label: 'Timetable', icon: Table },
  { to: '/auth', label: 'Auth', icon: LogIn },
  { to: '/profile', label: 'Profile', icon: UserRound },
]

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
        <Icon size={16} />
        <span>{item.label}</span>
      </span>
      <ChevronRight size={14} />
    </NavLink>
  )
}

export function AppLayout() {
  const { theme, toggleTheme, isAuthenticated } = useAppContext()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-border bg-surface/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-[1500px] items-center justify-between gap-3 px-4 py-3">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">SarajevoTransit</p>
            <h1 className="text-lg font-semibold text-ink">Public Transport</h1>
          </div>

          <div className="flex items-center gap-2">
            <p className="hidden text-xs text-muted sm:block">{isAuthenticated ? 'Signed in' : 'Guest'}</p>
            <button
              type="button"
              onClick={toggleTheme}
              className="rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
              aria-label="Toggle theme"
            >
              {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
            </button>
            <button
              type="button"
              className="rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
              onClick={() => setMenuOpen((value) => !value)}
              aria-label="Toggle menu"
            >
              {menuOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
          </div>
        </div>
      </header>

      {menuOpen ? (
        <>
          <button
            type="button"
            aria-label="Close menu"
            className="fixed inset-0 z-[1100] bg-black/35"
            onClick={() => setMenuOpen(false)}
          />
          <aside className="fixed right-0 top-0 z-[1200] h-full w-[min(360px,88vw)] border-l border-border bg-surface p-4 shadow-xl">
            <div className="mb-4 flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2 text-ink">
                <CircleUserRound size={18} />
                <span className="text-sm font-semibold">{isAuthenticated ? 'Signed in' : 'Guest mode'}</span>
              </div>
              <button
                type="button"
                className="rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
                onClick={() => setMenuOpen(false)}
                aria-label="Close menu"
              >
                <X size={16} />
              </button>
            </div>

            <nav className="grid gap-2">
              {navItems.map((item) => (
                <NavItem key={item.to} item={item} onClick={() => setMenuOpen(false)} />
              ))}
            </nav>
          </aside>
        </>
      ) : null}

      <main className="mx-auto w-full max-w-[1500px] px-4 py-4">
        <Outlet />
      </main>
    </div>
  )
}
