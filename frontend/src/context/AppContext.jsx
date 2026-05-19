import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react'
import { saveAuthSession, getAuthSession, clearAuthSession, enrichSessionWithMetadata, secondsUntilExpiry } from '../utils/authStorage'
import { gatewayClient } from '../services/gatewayClient'

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
  const [sessionModal, setSessionModal] = useState({ open: false, refreshExpired: false })

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

  // Show modal when access token is within 2 minutes of expiry
  useEffect(() => {
    if (!session) return
    const interval = setInterval(() => {
      const secs = secondsUntilExpiry(session)
      if (secs !== null && secs <= 120) {
        setSessionModal((m) => m.open ? m : { open: true, refreshExpired: false })
      }
    }, 30000)
    return () => clearInterval(interval)
  }, [session])

  // Also trigger modal on 401 from any API call
  useEffect(() => {
    function onSessionExpired() {
      if (session) setSessionModal({ open: true, refreshExpired: false })
    }
    window.addEventListener('session-expired', onSessionExpired)
    return () => window.removeEventListener('session-expired', onSessionExpired)
  }, [session])

  const refreshSession = useCallback(async () => {
    try {
      const newSession = await gatewayClient.refreshAccessToken(session?.refreshToken)
      setSession(newSession)
      setSessionModal({ open: false, refreshExpired: false })
    } catch {
      setSessionModal({ open: true, refreshExpired: true })
    }
  }, [session])

  const value = useMemo(
    () => ({
      theme,
      setTheme,
      toggleTheme: () => setTheme((current) => (current === 'dark' ? 'light' : 'dark')),
      session,
      isAuthenticated: Boolean(session?.accessToken),
      isAdmin: session?.role === 'ADMIN',
      login: (payload) => setSession(payload),
      logout: () => { setSession(null); setSessionModal({ open: false, refreshExpired: false }) },
      sessionModal,
      refreshSession,
      dismissSessionModal: () => setSessionModal({ open: false, refreshExpired: false }),
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
    [favorites, session, sessionModal, refreshSession, theme, tripHistory],
  )

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useAppContext() {
  const context = useContext(AppContext)
  if (!context) throw new Error('useAppContext must be used inside AppProvider')
  return context
}
