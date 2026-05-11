import {
  Bus,
  LogIn,
  MapPinned,
  Menu,
  Moon,
  Route,
  Sun,
  Table,
  UserRound,
} from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { useState } from 'react'
import { useAppContext } from '../../context/AppContext'

const navItems = [
  { to: '/', label: 'Route Planner', icon: Route },
  { to: '/lines', label: 'Lines', icon: Bus },
  { to: '/stops', label: 'Stops', icon: MapPinned },
  { to: '/timetable', label: 'Timetable', icon: Table },
  { to: '/auth', label: 'Auth', icon: LogIn },
  { to: '/profile', label: 'Profile', icon: UserRound },
]

function NavItem({ item }) {
  const Icon = item.icon
  return (
    <NavLink
      to={item.to}
      className={({ isActive }) =>
        `flex items-center gap-2 rounded-panel border px-3 py-2 text-sm font-medium transition ${
          isActive
            ? 'border-accent bg-accent text-white'
            : 'border-border text-muted hover:bg-surface-alt hover:text-ink'
        }`
      }
    >
      <Icon size={16} />
      <span>{item.label}</span>
    </NavLink>
  )
}

export function AppLayout() {
  const { theme, toggleTheme, isAuthenticated } = useAppContext()
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-border bg-surface/95 backdrop-blur">
        <div className="mx-auto flex w-full max-w-[1500px] items-center justify-between gap-3 px-4 py-3">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">SarajevoTransit</p>
            <h1 className="text-lg font-semibold text-ink">Public Transport</h1>
          </div>

          <button
            type="button"
            className="rounded-panel border border-border p-2 text-muted md:hidden"
            onClick={() => setMenuOpen((value) => !value)}
            aria-label="Toggle menu"
          >
            <Menu size={18} />
          </button>

          <nav className="hidden items-center gap-2 md:flex">
            {navItems.map((item) => (
              <NavItem key={item.to} item={item} />
            ))}
          </nav>

          <div className="hidden items-center gap-2 md:flex">
            <p className="text-xs text-muted">{isAuthenticated ? 'Signed in' : 'Guest'}</p>
            <button
              type="button"
              onClick={toggleTheme}
              className="rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
              aria-label="Toggle theme"
            >
              {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
            </button>
          </div>
        </div>

        {menuOpen ? (
          <div className="border-t border-border px-4 py-3 md:hidden">
            <nav className="grid gap-2">
              {navItems.map((item) => (
                <NavItem key={item.to} item={item} />
              ))}
            </nav>
            <div className="mt-3 flex items-center justify-between">
              <p className="text-xs text-muted">{isAuthenticated ? 'Signed in' : 'Guest'} mode</p>
              <button
                type="button"
                onClick={toggleTheme}
                className="rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
                aria-label="Toggle theme"
              >
                {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
              </button>
            </div>
          </div>
        ) : null}
      </header>

      <main className="mx-auto w-full max-w-[1500px] px-4 py-4">
        <Outlet />
      </main>
    </div>
  )
}
