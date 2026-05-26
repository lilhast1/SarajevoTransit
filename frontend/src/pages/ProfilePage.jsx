import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import {
  CalendarDays,
  Coins,
  History,
  Languages,
  Loader2,
  LockKeyhole,
  Moon,
  Pencil,
  RefreshCw,
  Route,
  Settings2,
  ShieldCheck,
  Star,
  SunMedium,
  Ticket,
  User,
  X,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { LoadingSpinner } from '../components/common/LoadingStates'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { ReviewForm } from '../components/reviews/ReviewForm'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

const DEFAULT_PROFILE_FORM = { fullName: '', email: '' }
const DEFAULT_PASSWORD_FORM = { newPassword: '', confirmPassword: '' }
const DEFAULT_PREFERENCE_FORM = {
  languageCode: 'BS',
  themeMode: 'LIGHT',
  notificationChannel: 'PUSH',
  highContrastEnabled: false,
  largeTextEnabled: false,
  screenReaderEnabled: false,
}
const DEFAULT_LOYALTY_FORM = {
  couponType: 'DISCOUNT',
  rideCode: '',
  // redeem form fields
  points: '',
  rewardType: 'DISCOUNT',
}

const TICKET_STATUS_STYLES = {
  ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  USED: 'bg-surface-alt text-muted',
  EXPIRED: 'bg-surface-alt text-muted',
  CANCELLED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}

function formatTime(value) {
  if (!value) return '--:--'
  return String(value).length > 5 ? String(value).slice(0, 5) : String(value)
}

function formatDays(daysValue, labels) {
  if (!daysValue) return ''
  return String(daysValue)
    .split(',')
    .map((item) => labels[item.trim()] || item.trim())
    .filter(Boolean)
    .join(' · ')
}

function toPageItems(result) {
  if (!result) return []
  if (Array.isArray(result.content)) return result.content
  if (Array.isArray(result)) return result
  return []
}

function resolveCurrentTheme(themeMode, preferredTheme) {
  const systemTheme = typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  if (themeMode === 'LIGHT') return 'light'
  if (themeMode === 'DARK') return 'dark'
  if (themeMode === 'SYSTEM') return systemTheme
  return preferredTheme
}

function mapLanguageCode(languageCode) {
  const normalized = String(languageCode || 'EN').toUpperCase()
  if (normalized === 'BS') return 'bs'
  if (normalized === 'SR') return 'sr'
  if (normalized === 'HR') return 'hr'
  return 'en'
}
function toLanguagePreference(languageCode) {
  const normalized = String(languageCode || '').toLowerCase()
  if (normalized === 'bosnian' || normalized === 'bs') return 'bosnian'
  if (normalized === 'croatian' || normalized === 'hr') return 'croatian'
  if (normalized === 'serbian' || normalized === 'sr') return 'serbian'
  return 'english'
}
function fromLanguagePreference(languagePreference) {
  const normalized = String(languagePreference || '').toLowerCase()
  if (normalized === 'bosnian') return 'BS'
  if (normalized === 'croatian') return 'HR'
  if (normalized === 'serbian') return 'SR'
  return 'EN'
}
function toThemePreference(themeMode) {
  const normalized = String(themeMode || '').toLowerCase()
  if (normalized === 'dark') return 'dark'
  if (normalized === 'system') return 'system'
  return 'light'
}
function fromThemePreference(themePreference) {
  const normalized = String(themePreference || '').toLowerCase()
  if (normalized === 'dark') return 'DARK'
  if (normalized === 'system') return 'SYSTEM'
  return 'LIGHT'
}
function toNotificationPreference(channel) {
  const normalized = String(channel || '').toLowerCase()
  if (normalized === 'email') return 'email'
  if (normalized === 'sms' || normalized === 'text message') return 'sms'
  return 'push notifications'
}
function fromNotificationPreference(channel) {
  const normalized = String(channel || '').toLowerCase()
  if (normalized === 'email') return 'EMAIL'
  if (normalized === 'sms') return 'SMS'
  return 'PUSH'
}

function getLineLabel(line, fallbackCode) {
  if (!line) return fallbackCode
  return `${line.code} · ${line.name}`
}

export function ProfilePage() {
  const {
    isAuthenticated,
    session,
    login,
    logout,
    theme,
    setTheme,
    setLanguage,
    textSize,
    toggleTextSize,
    highContrast,
    toggleHighContrast,
    tripHistory,
  } = useAppContext()
  const { t } = useTranslation('profile')

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [msg, setMsg] = useState(null)

  const [profileData, setProfileData] = useState(null)
  const [subscriptions, setSubscriptions] = useState([])
  const [tickets, setTickets] = useState([])
  const [reviews, setReviews] = useState([])
  const [travelHistory, setTravelHistory] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [availableLines, setAvailableLines] = useState([])
  const [loyaltyBalance, setLoyaltyBalance] = useState(0)
  const [loyaltyTransactions, setLoyaltyTransactions] = useState([])
  const [loyaltyCoupons, setLoyaltyCoupons] = useState([])
  const [generatedCoupon, setGeneratedCoupon] = useState(null)

  const [profileForm, setProfileForm] = useState(DEFAULT_PROFILE_FORM)
  const [passwordForm, setPasswordForm] = useState(DEFAULT_PASSWORD_FORM)
  const [preferenceForm, setPreferenceForm] = useState(DEFAULT_PREFERENCE_FORM)
  const [loyaltyForm, setLoyaltyForm] = useState(DEFAULT_LOYALTY_FORM)

  const [savingProfile, setSavingProfile] = useState(false)
  const [savingPassword, setSavingPassword] = useState(false)
  const [savingPreferences, setSavingPreferences] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [reviewTarget, setReviewTarget] = useState(null)

  const dayLabels = useMemo(
    () => ({
      MON: t('day_mon'),
      TUE: t('day_tue'),
      WED: t('day_wed'),
      THU: t('day_thu'),
      FRI: t('day_fri'),
      SAT: t('day_sat'),
      SUN: t('day_sun'),
    }),
    [t],
  )

  const languageOptions = useMemo(
    () => [
      { value: 'bosnian', label: 'Bosnian' },
      { value: 'croatian', label: 'Croatian' },
      { value: 'serbian', label: 'Serbian' },
      { value: 'english', label: 'English' },
    ],
    [t],
  )

  const themeOptions = useMemo(
    () => [
      { value: 'light', label: 'Light mode' },
      { value: 'dark', label: 'Dark mode' },
      { value: 'system', label: 'System default' },
    ],
    [t],
  )

  const notificationOptions = useMemo(
    () => [
      { value: 'push notifications', label: 'Push notifications' },
      { value: 'email', label: 'Email' },
      { value: 'sms', label: 'SMS' },
    ],
    [t],
  )

  const loyaltyRewardOptions = useMemo(
    () => [
      {
        value: 'DISCOUNT',
        label: 'Discount coupon',
        description: 'Turns your points into a discount coupon for the next ticket.',
      },
      {
        value: 'FREE_RIDE',
        label: 'Free ride coupon',
        description: 'Available only at the highest tier and tied to a specific ride.',
      },
    ],
    [],
  )

  const loyaltyTierLabel = useMemo(() => {
    const tier = String(profileData?.loyaltyTier || 'BRONZE').toUpperCase()
    return tier.charAt(0) + tier.slice(1).toLowerCase()
  }, [profileData?.loyaltyTier])

  const loyaltyCouponSelection = useMemo(() => {
    const tier = String(profileData?.loyaltyTier || 'BRONZE').toUpperCase()
    const freeRideEligible = Boolean(profileData?.loyaltyFreeRideEligible)
    return loyaltyRewardOptions.filter((option) => option.value === 'DISCOUNT' || (option.value === 'FREE_RIDE' && freeRideEligible))
  }, [profileData?.loyaltyFreeRideEligible, profileData?.loyaltyTier, loyaltyRewardOptions])

  const suggestionLineMap = useMemo(() => {
    const map = new Map()
    availableLines.forEach((line) => {
      map.set(String(line.code).trim().toLowerCase(), line)
    })
    return map
  }, [availableLines])

  const loadReviews = useCallback(async (userId) => {
    const result = await transitApi.getUserReviews(userId)
    setReviews(toPageItems(result))
  }, [])

  const loadProfileBundle = useCallback(async () => {
    if (!session?.userId) return

    setLoading(true)
    setError(null)

    try {
      const [summaryResult, subscriptionsResult, linesResult] = await Promise.all([
        transitApi.getUserSummary(session.userId),
        transitApi.getAllUserSubscriptions(session.userId),
        transitApi.getLines({ activeOnly: true }),
      ])

      setProfileData(summaryResult?.profile || null)
      setTravelHistory(Array.isArray(summaryResult?.travelHistory) ? summaryResult.travelHistory : [])
      setTickets(Array.isArray(summaryResult?.ticketPurchases) ? summaryResult.ticketPurchases : [])
      setSuggestions(Array.isArray(summaryResult?.personalizedLineSuggestions) ? summaryResult.personalizedLineSuggestions : [])
      setLoyaltyBalance(Number(summaryResult?.profile?.loyaltyPointsBalance || 0))
      setLoyaltyTransactions(Array.isArray(summaryResult?.loyaltyTransactions) ? summaryResult.loyaltyTransactions : [])
      setLoyaltyCoupons(Array.isArray(summaryResult?.loyaltyCoupons) ? summaryResult.loyaltyCoupons : [])
      setSubscriptions(Array.isArray(subscriptionsResult) ? subscriptionsResult : [])
      setAvailableLines(Array.isArray(linesResult) ? linesResult : [])

      const profile = summaryResult?.profile
      const preference = profile?.preference
      setProfileForm({
        fullName: profile?.fullName || session.fullName || '',
        email: profile?.email || session.email || '',
      })
      setPreferenceForm({
        languageCode: toLanguagePreference(preference?.languageCode || DEFAULT_PREFERENCE_FORM.languageCode),
        themeMode: toThemePreference(preference?.themeMode || DEFAULT_PREFERENCE_FORM.themeMode),
        notificationChannel: toNotificationPreference(preference?.notificationChannel || DEFAULT_PREFERENCE_FORM.notificationChannel),
        highContrastEnabled: Boolean(preference?.highContrastEnabled),
        largeTextEnabled: Boolean(preference?.largeTextEnabled),
        screenReaderEnabled: Boolean(preference?.screenReaderEnabled),
      })

      await loadReviews(session.userId)
    } catch (fetchError) {
      setError(fetchError.message || t('load_failed'))
    } finally {
      setLoading(false)
    }
  }, [loadReviews, session?.userId, session?.fullName, session?.email, t])

  useEffect(() => {
    loadProfileBundle()
  }, [loadProfileBundle, refreshKey])
  }, [session?.userId])

  useEffect(() => {
    fetchSubscriptions()
  }, [fetchSubscriptions])
  const [recentTickets, setRecentTickets] = useState([])
  const [ticketsLoading, setTicketsLoading] = useState(true)
  const [ticketsError, setTicketsError] = useState(null)

  useEffect(() => {
    if (!isAuthenticated || !session?.userId) return

    let active = true
    const loadRecentTickets = async () => {
      setTicketsLoading(true)
      setTicketsError(null)
      try {
        const data = await gatewayClient.getWallet(session.userId, '?size=3&sort=purchaseDate,desc')
        if (!active) return
        const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
        setRecentTickets(list)
      } catch (err) {
        if (active) {
          setRecentTickets([])
          setTicketsError(err.message || t('tickets_load_failed'))
        }
      } finally {
        if (active) setTicketsLoading(false)
      }
    }

    loadRecentTickets()

    return () => {
      active = false
    }
  }, [isAuthenticated, session?.userId, t])

  if (!isAuthenticated) return <Navigate to="/auth" replace />

  const profileName = profileData?.fullName || session.fullName || session.email
  const profileEmail = profileData?.email || session.email
  const preferenceThemePreview = resolveCurrentTheme(preferenceForm.themeMode, theme)

  async function handleProfileSave(event) {
    event.preventDefault()
    setSavingProfile(true)
    setMsg(null)

    try {
      const updated = await transitApi.updateUserProfile(session.userId, {
        fullName: profileForm.fullName,
        email: profileForm.email,
      })

      setProfileData(updated)
      setProfileForm({ fullName: updated.fullName, email: updated.email })
      login({ ...session, fullName: updated.fullName, email: updated.email })
      setMsg({ type: 'success', text: t('profile_updated') })
    } catch (saveError) {
      setMsg({ type: 'error', text: saveError.message || t('profile_update_failed') })
    } finally {
      setSavingProfile(false)
    }
  }

  async function handlePasswordSave(event) {
    event.preventDefault()
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setMsg({ type: 'error', text: t('password_mismatch') })
      return
    }

    setSavingPassword(true)
    setMsg(null)

    try {
      await transitApi.updateUserPassword(session.userId, { newPassword: passwordForm.newPassword })
      setPasswordForm(DEFAULT_PASSWORD_FORM)
      setMsg({ type: 'success', text: t('password_updated') })
    } catch (saveError) {
      setMsg({ type: 'error', text: saveError.message || t('password_update_failed') })
    } finally {
      setSavingPassword(false)
    }
  }

  async function handleLoyaltyCouponGenerate(event) {
    event.preventDefault()

    if (loyaltyForm.couponType === 'FREE_RIDE' && !loyaltyForm.rideCode) {
      setMsg({ type: 'error', text: 'Choose a ride for the free ride coupon.' })
      return
    }

    if (loyaltyForm.couponType === 'FREE_RIDE' && !profileData?.loyaltyFreeRideEligible) {
      setMsg({ type: 'error', text: 'Free ride coupons are only available at the highest tier.' })
      return
    }

    if (loyaltyBalance <= 0) {
      setMsg({ type: 'error', text: 'You do not have enough points to generate a coupon.' })
      return
    }

    setMsg(null)

    try {
      const generated = await transitApi.generateUserLoyaltyCoupon(session.userId, {
        couponType: loyaltyForm.couponType,
        rideCode: loyaltyForm.couponType === 'FREE_RIDE' ? loyaltyForm.rideCode : null,
      })
      setLoyaltyForm(DEFAULT_LOYALTY_FORM)
      setGeneratedCoupon(generated)
      setRefreshKey((current) => current + 1)
      setMsg({ type: 'success', text: 'Your coupon was generated successfully.' })
    } catch (couponError) {
      setMsg({ type: 'error', text: couponError.message || 'Coupon generation failed.' })
    }
  }

  async function handleLoyaltyRedeem(event) {
    event.preventDefault()
    const points = Number(loyaltyForm.points || 0)
    if (!points || points <= 0) {
      setMsg({ type: 'error', text: 'Enter a positive number of points to redeem.' })
      return
    }
    if (points > loyaltyBalance) {
      setMsg({ type: 'error', text: 'You do not have enough points.' })
      return
    }

    setMsg(null)
    try {
      await transitApi.redeemUserLoyaltyPoints(session.userId, { points, rewardType: loyaltyForm.rewardType })
      setLoyaltyForm((current) => ({ ...current, points: '', rewardType: 'DISCOUNT' }))
      setRefreshKey((k) => k + 1)
      setMsg({ type: 'success', text: 'Points redeemed successfully.' })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Failed to redeem points.' })
    }
  }

  async function handlePreferencesSave(event) {
    event.preventDefault()
    setSavingPreferences(true)
    setMsg(null)

    try {
      const payload = {
        ...preferenceForm,
        languageCode: fromLanguagePreference(preferenceForm.languageCode),
        themeMode: fromThemePreference(preferenceForm.themeMode),
        notificationChannel: fromNotificationPreference(preferenceForm.notificationChannel),
      }
      const updated = await transitApi.updateUserPreferences(session.userId, payload)
      setPreferenceForm({
        languageCode: toLanguagePreference(updated.languageCode),
        themeMode: toThemePreference(updated.themeMode),
        notificationChannel: toNotificationPreference(updated.notificationChannel),
        highContrastEnabled: updated.highContrastEnabled,
        largeTextEnabled: updated.largeTextEnabled,
        screenReaderEnabled: updated.screenReaderEnabled,
      })

      setLanguage(mapLanguageCode(updated.languageCode))
      setTheme(resolveCurrentTheme(updated.themeMode, theme))
      if (Boolean(updated.highContrastEnabled) !== highContrast) toggleHighContrast()
      if (Boolean(updated.largeTextEnabled) !== (textSize === 'large')) toggleTextSize()
      setProfileData((current) => (current ? { ...current, preference: updated } : current))
      setMsg({ type: 'success', text: t('preferences_updated') })
    } catch (saveError) {
      setMsg({ type: 'error', text: saveError.message || t('preferences_update_failed') })
    } finally {
      setSavingPreferences(false)
    }
  }

  return (
    <div className="space-y-4">
      {msg?.type === 'success' && <SuccessAlert message={msg.text} onDismiss={() => setMsg(null)} />}
      {msg?.type === 'error' && <ErrorAlert error={msg.text} onDismiss={() => setMsg(null)} />}
      {error && <ErrorAlert error={error} onDismiss={() => setError(null)} />}

      <PanelCard tone="soft">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-muted">{t('profile_title')}</p>
            <h2 className="mt-1 text-xl font-semibold text-ink">{profileName}</h2>
            <p className="mt-1 text-sm text-muted">{profileEmail}</p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => setRefreshKey((current) => current + 1)}
              className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
            >
              <RefreshCw size={14} aria-hidden="true" />
              {t('refresh')}
            </button>
            <Link
              to="/lines"
              className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-2 text-sm font-medium text-white transition hover:bg-accent/90"
            >
              <Route size={14} aria-hidden="true" />
              {t('browse_lines')}
            </Link>
            <button
              type="button"
              onClick={logout}
              className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
            >
              {t('logout')}
            </button>
          </div>
        </div>
      </PanelCard>

      {loading ? (
        <PanelCard>
          <LoadingSpinner label={t('loading_profile')} />
        </PanelCard>
      ) : (
        <>
          <div className="grid gap-4 lg:grid-cols-2">
            <PanelCard tone="default">
              <div className="flex items-center gap-2">
                <User size={18} className="text-accent" aria-hidden="true" />
                <h3 className="text-base font-semibold text-ink">{t('personal_info')}</h3>
              </div>
              <form className="mt-4 space-y-3" onSubmit={handleProfileSave}>
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">{t('full_name')}</span>
                  <input
                    value={profileForm.fullName}
                    onChange={(event) => setProfileForm((current) => ({ ...current, fullName: event.target.value }))}
                    className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                    required
                  />
                </label>
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">{t('email')}</span>
                  <input
                    type="email"
                    value={profileForm.email}
                    onChange={(event) => setProfileForm((current) => ({ ...current, email: event.target.value }))}
                    className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                    required
                  />
                </label>
                <button
                  type="submit"
                  disabled={savingProfile}
                  className="inline-flex items-center gap-2 rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-70"
                >
                  {savingProfile ? <Loader2 size={14} className="animate-spin" aria-hidden="true" /> : <Pencil size={14} aria-hidden="true" />}
                  {t('save_profile')}
                </button>
              </form>
            </PanelCard>

            <PanelCard tone="default">
              <div className="flex items-center gap-2">
                <LockKeyhole size={18} className="text-accent" aria-hidden="true" />
                <h3 className="text-base font-semibold text-ink">{t('change_password')}</h3>
              </div>
              <form className="mt-4 space-y-3" onSubmit={handlePasswordSave}>
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">{t('new_password')}</span>
                  <input
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(event) => setPasswordForm((current) => ({ ...current, newPassword: event.target.value }))}
                    className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                    minLength={8}
                    required
                  />
                </label>
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">{t('confirm_password')}</span>
                  <input
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(event) => setPasswordForm((current) => ({ ...current, confirmPassword: event.target.value }))}
                    className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                    minLength={8}
                    required
                  />
                </label>
                <button
                  type="submit"
                  disabled={savingPassword || !passwordForm.newPassword || passwordForm.newPassword !== passwordForm.confirmPassword}
                  className="inline-flex items-center gap-2 rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-70"
                >
                  {savingPassword ? <Loader2 size={14} className="animate-spin" aria-hidden="true" /> : <ShieldCheck size={14} aria-hidden="true" />}
                  {t('save_password')}
                </button>
              </form>
            </PanelCard>
          </div>
                          <h4 className="text-sm font-semibold text-ink">Generate coupon</h4>
                          <p className="mt-1 text-xs text-muted">Manage coupons from the Loyalty section below.</p>

          <PanelCard tone="default">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <History size={18} className="text-accent" aria-hidden="true" />
                <h3 className="text-base font-semibold text-ink">{t('usage_history')}</h3>
              </div>
              <span className="text-xs text-muted">{t('usage_history_hint')}</span>
            </div>

            <div className="mt-4 grid gap-4 xl:grid-cols-2">
              <div className="space-y-3">
                <h4 className="text-sm font-semibold text-ink">{t('travel_history')}</h4>
                {travelHistory.length === 0 ? (
                  <p className="text-sm text-muted">{t('no_travel_history')}</p>
                ) : (
                  <div className="space-y-2">
                    {travelHistory.map((item) => (
                      <div key={item.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="rounded-full bg-accent/10 px-2 py-0.5 text-xs font-semibold text-accent">{item.lineCode}</span>
                          <span className="text-xs text-muted">{formatDateTime(item.traveledAt)}</span>
                        </div>
                        <p className="mt-1 text-ink">{item.fromStop} → {item.toStop}</p>
                        <p className="text-xs text-muted">{t('travel_duration', { minutes: item.durationMinutes })}</p>
                      </div>
                    ))}
                  </div>
                )}

                <h4 className="pt-2 text-sm font-semibold text-ink">{t('planner_history')}</h4>
                {tripHistory.length === 0 ? (
                  <p className="text-sm text-muted">{t('no_planner_history')}</p>
                ) : (
                  <div className="space-y-2">
                    {tripHistory.map((item) => (
                      <div key={item.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                        <p className="font-medium text-ink">{item.fromStop} → {item.toStop}</p>
                        <p className="text-xs text-muted">{item.lineCode} · {item.durationMinutes} min · {formatDateTime(item.traveledAt)}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="space-y-3">
                <h4 className="text-sm font-semibold text-ink">{t('ticket_history')}</h4>
                {tickets.length === 0 ? (
                  <p className="text-sm text-muted">{t('no_tickets')}</p>
                ) : (
                  <div className="space-y-2">
                    {tickets.map((ticket) => (
                      <div key={ticket.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <p className="font-medium text-ink capitalize">{String(ticket.type || '').toLowerCase()}</p>
                            <p className="text-xs text-muted">{formatDate(ticket.purchaseDate)}{ticket.validUntil ? ` · ${t('valid_until', { date: formatDate(ticket.validUntil) })}` : ''}</p>
                            {ticket.amount != null ? <p className="text-xs text-muted">{ticket.amount} KM</p> : null}
                          </div>
                          <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${TICKET_STATUS_STYLES[ticket.status] || 'bg-surface-alt text-muted'}`}>
                            {ticket.status}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                <h4 className="pt-2 text-sm font-semibold text-ink">{t('review_history')}</h4>
                {reviews.length === 0 ? (
                  <p className="text-sm text-muted">{t('no_reviews')}</p>
                ) : (
                  <div className="space-y-2">
                    {reviews.map((review) => {
                      const reviewLine = availableLines.find((line) => String(line.id) === String(review.lineId))
                      return (
                        <div key={review.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                          <div className="flex items-center justify-between gap-2">
                            <p className="font-medium text-ink">{getLineLabel(reviewLine, `Line ${review.lineId}`)}</p>
                            <span className="inline-flex items-center gap-1 text-xs font-semibold text-amber-500">
                              <Star size={12} fill="currentColor" aria-hidden="true" />
                              {review.rating}
                            </span>
                          </div>
                          {review.reviewText ? <p className="mt-1 text-sm text-muted">{review.reviewText}</p> : null}
                          <p className="mt-1 text-xs text-muted">{formatDateTime(review.createdAt)} · {review.moderationStatus}</p>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            </div>
          </PanelCard>

          <PanelCard tone="default">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <Coins size={18} className="text-amber-500" aria-hidden="true" />
                <div>
                  <h3 className="text-base font-semibold text-ink">Loyalty programme</h3>
                  <p className="text-sm text-muted">Points are earned from ticket purchases and validated rides, then redeemed for discounts or free tickets.</p>
                </div>
              </div>
              <div className="rounded-lg border border-border bg-surface-soft px-4 py-3 text-right">
                <p className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">Current balance</p>
                <p className="text-2xl font-semibold text-ink">{loyaltyBalance}</p>
                <p className="text-xs text-muted">{loyaltyTransactions.length} total transactions</p>
              </div>
            </div>

            <div className="mt-4 grid gap-4 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
              <div className="rounded-lg border border-border bg-surface-soft p-4">
                <h4 className="text-sm font-semibold text-ink">Redeem points</h4>
                <form className="mt-3 space-y-3" onSubmit={handleLoyaltyRedeem}>
                  <label className="block">
                    <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">Points to redeem</span>
                    <input
                      type="number"
                      min="1"
                      step="1"
                      value={loyaltyForm.points}
                      onChange={(event) => setLoyaltyForm((current) => ({ ...current, points: event.target.value }))}
                      className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                    />
                  </label>

                  <label className="block">
                    <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">Reward type</span>
                    <select
                      value={loyaltyForm.rewardType}
                      onChange={(event) => setLoyaltyForm((current) => ({ ...current, rewardType: event.target.value }))}
                      className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                    >
                      {loyaltyRewardOptions.map((option) => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                      ))}
                    </select>
                  </label>

                  <div className="rounded-lg border border-border bg-surface px-3 py-2 text-xs text-muted">
                    {loyaltyRewardOptions.find((option) => option.value === loyaltyForm.rewardType)?.description}
                  </div>

                  <button
                    type="submit"
                    disabled={!loyaltyForm.points || Number(loyaltyForm.points) > loyaltyBalance}
                    className="inline-flex items-center gap-2 rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-70"
                  >
                    Redeem loyalty points
                  </button>
                </form>
              </div>

              <div className="rounded-lg border border-border bg-surface-soft p-4">
                <h4 className="text-sm font-semibold text-ink">Point history</h4>
                {loyaltyTransactions.length === 0 ? (
                  <p className="mt-3 text-sm text-muted">No loyalty activity yet.</p>
                ) : (
                  <div className="mt-3 max-h-96 space-y-2 overflow-y-auto pr-1">
                    {loyaltyTransactions.map((transaction) => {
                      const movement = transaction.transactionType === 'REDEEM'
                        ? -1 * (transaction.pointsSpent || transaction.points || 0)
                        : transaction.pointsEarned || transaction.points || 0

                      return (
                        <div key={transaction.id} className="rounded-lg border border-border bg-surface px-3 py-2 text-sm">
                          <div className="flex items-start justify-between gap-3">
                            <div className="min-w-0">
                              <p className="font-medium text-ink">{transaction.description}</p>
                              <p className="text-xs text-muted">
                                {transaction.referenceType} · {formatDateTime(transaction.createdAt)}
                              </p>
                            </div>
                            <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold ${movement >= 0 ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'}`}>
                              {movement >= 0 ? '+' : ''}{movement}
                            </span>
                          </div>
                          {transaction.expiryDate ? <p className="mt-1 text-xs text-muted">Expires {formatDate(transaction.expiryDate)}</p> : null}
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            </div>
          </PanelCard>

          <div className="grid gap-4 xl:grid-cols-2">
            <PanelCard tone="default">
              <div className="flex items-center gap-2">
                <Ticket size={18} className="text-accent" aria-hidden="true" />
                <h3 className="text-base font-semibold text-ink">{t('ticket_history_short')}</h3>
              </div>
              <p className="mt-1 text-sm text-muted">{t('ticket_history_hint')}</p>
              <div className="mt-4 space-y-2">
                {tickets.length === 0 ? (
                  <p className="text-sm text-muted">{t('no_tickets')}</p>
                ) : (
                  tickets.map((ticket) => (
                    <div key={ticket.id} className="flex items-center justify-between rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                      <div>
                        <p className="font-medium text-ink">{ticket.type}</p>
                        <p className="text-xs text-muted">{formatDate(ticket.purchaseDate)}</p>
                      </div>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${TICKET_STATUS_STYLES[ticket.status] || 'bg-surface-alt text-muted'}`}>
                        {ticket.status}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </PanelCard>

            <PanelCard tone="default">
              <div className="flex items-center gap-2">
                <Route size={18} className="text-accent" aria-hidden="true" />
                <h3 className="text-base font-semibold text-ink">{t('personalized_suggestions')}</h3>
              </div>
              <p className="mt-1 text-sm text-muted">{t('personalized_suggestions_hint')}</p>
              <div className="mt-4 space-y-2">
                {suggestions.length === 0 ? (
                  <p className="text-sm text-muted">{t('no_suggestions')}</p>
                ) : (
                  suggestions.map((code) => {
                    const line = suggestionLineMap.get(String(code).trim().toLowerCase())
                    return (
                      <div key={code} className="flex items-center justify-between gap-3 rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                        <div className="min-w-0 flex-1">
                          {line ? <LineBadge line={line} /> : <p className="font-medium text-ink">{code}</p>}
                          <p className="mt-1 text-xs text-muted">{t('suggestion_reason')}</p>
                        </div>
                        <Link to="/lines" className="shrink-0 text-xs font-medium text-accent underline-offset-2 hover:underline">
                          {t('browse_lines')}
                        </Link>
                      </div>
                    )
                  })
                )}
              </div>
            </PanelCard>
          </div>

          <PanelCard tone="default">
            <div className="flex items-center gap-2">
              <CalendarDays size={18} className="text-accent" aria-hidden="true" />
              <h3 className="text-base font-semibold text-ink">{t('subscriptions')}</h3>
            </div>
            <div className="mt-4 space-y-2 text-sm">
              {subscriptions.length === 0 ? (
                <p className="text-muted">{t('no_subs')}</p>
              ) : (
                subscriptions.map((sub) => (
                  <div key={sub.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2">
                    <div className="flex items-center justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <p className="font-medium text-ink">{sub.lineName || `Line ${sub.lineId}`}</p>
                        <p className="text-xs text-muted">{sub.lineCode || `#${sub.lineId}`}</p>
                      </div>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${sub.isActive ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400'}`}>
                        {sub.isActive ? t('active') : t('inactive')}
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </PanelCard>
        </>
      )}

      <PanelCard tone="default">
        <div className="flex items-center gap-2">
          <Star size={16} className="text-amber-400" fill="currentColor" aria-hidden="true" />
          <h3 className="text-base font-semibold text-ink">{t('rate_title')}</h3>
        <div className="flex items-center justify-between">
          <h3 className="text-base font-semibold text-ink">{t('wallet')}</h3>
          <Link to="/tickets" className="text-xs text-accent underline-offset-2 hover:underline">
            {t('view_all_tickets')}
          </Link>
        </div>

        {ticketsLoading ? (
          <p className="mt-3 text-sm text-muted">{t('loading')}</p>
        ) : ticketsError ? (
          <ErrorAlert error={ticketsError} onDismiss={() => setTicketsError(null)} />
        ) : recentTickets.length === 0 ? (
          <p className="mt-3 text-sm text-muted">
            {t('no_tickets')}{' '}
            <Link to="/tickets" className="text-accent underline-offset-2 hover:underline">
              {t('buy_first_ticket')}
            </Link>
          </p>
        ) : (
          <div className="mt-3 space-y-2">
            {recentTickets.map((ticket) => (
              <div key={ticket.id} className="flex items-center justify-between rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                <div>
                  <span className="font-medium text-ink capitalize">{ticket.type?.toLowerCase()}</span>
                  {ticket.validUntil && (
                    <span className="ml-2 text-xs text-muted">{t('valid_until', { date: formatDate(ticket.validUntil) })}</span>
                  )}
                </div>
                <span className={`rounded px-2 py-0.5 text-xs font-semibold ${TICKET_STATUS_STYLES[ticket.status] || ''}`}>
                  {ticket.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </PanelCard>

      {/* ── Rate a line ─────────────────────────────────────────────────── */}
      <PanelCard tone="default">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Star size={16} className="text-amber-400" fill="currentColor" aria-hidden="true" />
            <h3 className="text-base font-semibold text-ink">{t('rate_title')}</h3>
          </div>
          <Link to="/lines" className="text-xs text-accent underline-offset-2 hover:underline">
            {t('rate_browse_lines')}
          </Link>
        </div>
        <p className="mt-1 text-sm text-muted">{t('rate_subtitle')}</p>

        <div className="mt-3">
          {subscriptions.length === 0 ? (
            <p className="text-sm text-muted">
              {t('rate_no_subs')}{' '}
              <Link to="/lines" className="text-accent underline-offset-2 hover:underline">
                {t('rate_browse_lines')}
              </Link>
            </p>
          ) : (
            <div className="space-y-2">
              {subscriptions.map((sub) => (
                <div key={sub.id} className="flex items-center justify-between rounded-lg border border-border bg-surface-soft px-3 py-2">
                  <div className="flex min-w-0 items-center gap-2">
                    <LineBadge line={{ code: sub.lineCode || String(sub.lineId), vehicleTypeName: 'bus', name: sub.lineName || `Line ${sub.lineId}` }} />
                    <span className="truncate text-sm text-ink">{sub.lineName || `Line ${sub.lineId}`}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setReviewTarget(sub)}
                    className="ml-2 inline-flex shrink-0 items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt"
                  >
                    <Star size={11} className="text-amber-400" fill="currentColor" aria-hidden="true" />
                    {t('rate_write_review')}
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </PanelCard>

      {reviewTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          onClick={() => setReviewTarget(null)}
        >
          <div
            className="w-full max-w-md rounded-panel border border-border bg-surface p-6 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-base font-semibold text-ink">
                {t('rate_modal_title', { line: reviewTarget.lineName || `Line ${reviewTarget.lineId}` })}
              </h2>
              <button
                type="button"
                onClick={() => setReviewTarget(null)}
                className="text-muted transition hover:text-ink"
                aria-label={t('rate_modal_close')}
              >
                <X size={16} />
              </button>
            </div>
            <ReviewForm
              lineId={reviewTarget.lineId}
              reviewerUserId={session.userId}
              onSuccess={() => {
                setReviewTarget(null)
                loadReviews(session.userId)
              }}
            />
          </div>
        </div>
      )}
    </div>
  )
}