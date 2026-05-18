import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { saveAuthSession, getAuthSession, clearAuthSession, enrichSessionWithMetadata } from '../utils/authStorage'

const STORAGE_KEYS = {
  theme: 'sarajevo-transit-theme',
  favorites: 'sarajevo-transit-favorites',
  history: 'sarajevo-transit-history',
}

const AppContext = createContext(null)

function getInitialTheme() {
  const stored = window.localStorage.getItem(STORAGE_KEYS.theme)
  if (stored === 'dark' || stored === 'light') return stored
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function readStorage(key, fallback) {
  const raw = window.localStorage.getItem(key)
  if (!raw) return fallback
  try {
    return JSON.parse(raw)
  } catch {
    return fallback
  }
}

export function AppProvider({ children }) {
  const [theme, setTheme] = useState(getInitialTheme)
  const [session, setSession] = useState(() => getAuthSession())
  const [favorites, setFavorites] = useState(() => readStorage(STORAGE_KEYS.favorites, { lines: [], stops: [] }))
  const [tripHistory, setTripHistory] = useState(() => readStorage(STORAGE_KEYS.history, []))

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    window.localStorage.setItem(STORAGE_KEYS.theme, theme)
  }, [theme])

  useEffect(() => {
    if (session) {
      saveAuthSession(enrichSessionWithMetadata(session))
    } else {
      clearAuthSession()
    }
  }, [session])

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEYS.favorites, JSON.stringify(favorites))
  }, [favorites])

  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEYS.history, JSON.stringify(tripHistory.slice(0, 30)))
  }, [tripHistory])

  const value = useMemo(
    () => ({
      theme,
      setTheme,
      toggleTheme: () => setTheme((current) => (current === 'dark' ? 'light' : 'dark')),
      session,
      isAuthenticated: Boolean(session?.accessToken),
      isAdmin: session?.role === 'ADMIN',
      login: (payload) => setSession(payload),
      logout: () => setSession(null),
      favorites,
      toggleFavoriteLine: (lineId) => {
        setFavorites((current) => ({
          ...current,
          lines: current.lines.includes(lineId)
            ? current.lines.filter((id) => id !== lineId)
            : [...current.lines, lineId],
        }))
      },
      toggleFavoriteStop: (stopId) => {
        setFavorites((current) => ({
          ...current,
          stops: current.stops.includes(stopId)
            ? current.stops.filter((id) => id !== stopId)
            : [...current.stops, stopId],
        }))
      },
      tripHistory,
      addTripHistoryItem: (item) => setTripHistory((current) => [item, ...current].slice(0, 30)),
    }),
    [favorites, session, theme, tripHistory],
  )

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useAppContext() {
  const context = useContext(AppContext)
  if (!context) throw new Error('useAppContext must be used inside AppProvider')
  return context
}
