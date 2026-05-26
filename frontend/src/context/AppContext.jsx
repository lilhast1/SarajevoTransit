import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react'
import { saveAuthSession, getAuthSession, clearAuthSession, enrichSessionWithMetadata, secondsUntilExpiry } from '../utils/authStorage'
import { gatewayClient } from '../services/gatewayClient'
import { transitApi } from '../services/transitApi'
import i18n from '../i18n'

const STORAGE_KEYS = {
  theme: 'sarajevo-transit-theme',
  favorites: 'sarajevo-transit-favorites',
  history: 'sarajevo-transit-history',
  language: 'sarajevo-transit-language',
  textSize: 'sarajevo-transit-text-size',
  highContrast: 'sarajevo-transit-high-contrast',
}

const SUPPORTED_LANGUAGES = ['en', 'bs', 'sr', 'hr']

function getInitialLanguage() {
  const stored = window.localStorage.getItem(STORAGE_KEYS.language)
  if (SUPPORTED_LANGUAGES.includes(stored)) return stored
  return 'en'
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
  const [language, setLanguageState] = useState(getInitialLanguage)
  const [textSize, setTextSize] = useState(() => window.localStorage.getItem(STORAGE_KEYS.textSize) || 'normal')
  const [highContrast, setHighContrast] = useState(() => window.localStorage.getItem(STORAGE_KEYS.highContrast) === 'true')
  const [session, setSession] = useState(() => getAuthSession())
  const [favorites, setFavorites] = useState(() => readStorage(STORAGE_KEYS.favorites, { lines: [], stops: [] }))
  const [tripHistory, setTripHistory] = useState(() => readStorage(STORAGE_KEYS.history, []))
  const [sessionModal, setSessionModal] = useState({ open: false, refreshExpired: false })
  const [subscribedLines, setSubscribedLines] = useState({})
  const [subscriptionsRefreshKey, setSubscriptionsRefreshKey] = useState(0)
  const [unreadCount, setUnreadCount] = useState(0)
  const [recentNotifications, setRecentNotifications] = useState([])

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    window.localStorage.setItem(STORAGE_KEYS.theme, theme)
  }, [theme])

  useEffect(() => {
    document.documentElement.lang = language
    window.localStorage.setItem(STORAGE_KEYS.language, language)
    i18n.changeLanguage(language)
  }, [language])

  useEffect(() => {
    document.documentElement.style.fontSize = textSize === 'large' ? '112%' : ''
    window.localStorage.setItem(STORAGE_KEYS.textSize, textSize)
  }, [textSize])

  useEffect(() => {
    document.documentElement.classList.toggle('high-contrast', highContrast)
    window.localStorage.setItem(STORAGE_KEYS.highContrast, String(highContrast))
  }, [highContrast])

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

  // Load active subscriptions from backend when user logs in
  useEffect(() => {
    if (!session?.userId) {
      setSubscribedLines({})
      return
    }
    let active = true
    transitApi.getUserSubscriptions(session.userId).then((subs) => {
      if (!active) return
      const map = {}
      subs.forEach((sub) => {
        if (sub.isActive) map[sub.lineId] = sub.id
      })
      setSubscribedLines(map)
    }).catch(() => {
      // Silently fail — subscriptions are not critical for page rendering
    })
    return () => { active = false }
  }, [session?.userId, subscriptionsRefreshKey])

  // Poll unread notifications count every 30s when logged in
  useEffect(() => {
    if (!session?.userId) {
      setUnreadCount(0)
      setRecentNotifications([])
      return
    }
    let active = true

    async function poll() {
      try {
        const countResult = await transitApi.getUnreadCount(session.userId)
        if (!active) return
        const count = typeof countResult === 'object' ? (countResult.count ?? 0) : Number(countResult) || 0

        setUnreadCount((prev) => {
          if (count > prev) {
            transitApi.getUnreadNotifications(session.userId)
              .then((items) => {
                const latest = items?.[0]
                window.dispatchEvent(new CustomEvent('new-notification', {
                  detail: { title: latest?.title ?? null },
                }))
              })
              .catch(() => window.dispatchEvent(new CustomEvent('new-notification', { detail: { title: null } })))
          }
          return count
        })
      } catch {
        // Silently fail — notifications are not critical
      }
    }

    poll()
    const id = setInterval(poll, 10000)
    return () => { active = false; clearInterval(id) }
  }, [session?.userId])

  // Show browser notification when new-notification event fires
  useEffect(() => {
    function handleNewNotification(e) {
      if (Notification.permission === 'granted') {
        new Notification('SarajevoTransit', {
          body: e.detail?.title ?? 'You have a new notification.',
          icon: '/favicon.ico',
        })
      }
    }
    window.addEventListener('new-notification', handleNewNotification)
    return () => window.removeEventListener('new-notification', handleNewNotification)
  }, [])

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
      language,
      setLanguage: (lang) => { if (SUPPORTED_LANGUAGES.includes(lang)) setLanguageState(lang) },
      textSize,
      toggleTextSize: () => setTextSize((s) => (s === 'normal' ? 'large' : 'normal')),
      highContrast,
      toggleHighContrast: () => setHighContrast((v) => !v),
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
      subscribedLines,
      isLineSubscribed: (lineId) => Object.prototype.hasOwnProperty.call(subscribedLines, lineId),
      subscribeToLine: async (lineId, lineCode, lineName, startInterval, endInterval, daysOfWeek) => {
        const sub = await transitApi.subscribeToLine({ userId: session?.userId, lineId, lineCode, lineName, startInterval, endInterval, daysOfWeek })
        setSubscribedLines((prev) => ({ ...prev, [lineId]: sub.id }))
        setFavorites((prev) => ({
          ...prev,
          lines: prev.lines.includes(lineId) ? prev.lines : [...prev.lines, lineId],
        }))
        return sub
      },
      unsubscribeFromLine: async (lineId) => {
        const subId = subscribedLines[lineId]
        if (!subId) return
        await transitApi.unsubscribeFromLine(subId)
        setSubscribedLines((prev) => {
          const next = { ...prev }
          delete next[lineId]
          return next
        })
        setFavorites((prev) => ({
          ...prev,
          lines: prev.lines.filter((id) => id !== lineId),
        }))
      },
      refreshSubscriptions: () => setSubscriptionsRefreshKey((k) => k + 1),
      updateLineSubscription: async (subscriptionId, data) => {
        return transitApi.updateSubscription(subscriptionId, data)
      },
      unreadCount,
      recentNotifications,
      fetchRecentNotifications: async () => {
        if (!session?.userId) return
        try {
          const data = await transitApi.getUnreadNotifications(session.userId)
          setRecentNotifications(data || [])
        } catch {
          setRecentNotifications([])
        }
      },
      markAsRead: async (id) => {
        await transitApi.markAsRead(id)
        setRecentNotifications((prev) => prev.filter((n) => n.id !== id))
        setUnreadCount((prev) => Math.max(0, prev - 1))
      },
      markAllAsRead: async () => {
        if (!session?.userId) return
        await transitApi.markAllAsRead(session.userId)
        setUnreadCount(0)
        setRecentNotifications([])
      },
      requestNotificationPermission: async () => {
        if (!('Notification' in window)) return 'denied'
        if (Notification.permission === 'default') {
          return Notification.requestPermission()
        }
        return Notification.permission
      },
      tripHistory,
      addTripHistoryItem: (item) => setTripHistory((current) => [item, ...current].slice(0, 30)),
    }),
    [favorites, session, sessionModal, refreshSession, theme, language, textSize, highContrast, tripHistory, subscribedLines, subscriptionsRefreshKey, unreadCount, recentNotifications],
  )

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useAppContext() {
  const context = useContext(AppContext)
  if (!context) throw new Error('useAppContext must be used inside AppProvider')
  return context
}
