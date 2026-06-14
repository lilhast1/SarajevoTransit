import { useCallback, useEffect, useMemo, useState } from 'react'
import { gatewayClient } from '../../services/gatewayClient'
import { transitApi } from '../../services/transitApi'
import {
  DEFAULT_COUPON_FORM,
  DEFAULT_PASSWORD_FORM,
  DEFAULT_PREFERENCE_FORM,
  DEFAULT_PROFILE_FORM,
  fromLanguagePreference,
  fromNotificationPreference,
  fromThemePreference,
  normalizeTimeInput,
  toLanguagePreference,
  toNotificationPreference,
  toPageItems,
  toThemePreference,
} from './_shared'

export function useProfileData({ session, refreshSubscriptions, savePreferences, t }) {
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
  const [couponForm, setCouponForm] = useState(DEFAULT_COUPON_FORM)

  const [savingProfile, setSavingProfile] = useState(false)
  const [savingPassword, setSavingPassword] = useState(false)
  const [savingPreferences, setSavingPreferences] = useState(false)
  const [processing, setProcessing] = useState(false)

  const [refreshKey, setRefreshKey] = useState(0)
  const refresh = useCallback(() => setRefreshKey((current) => current + 1), [])

  const [recentTickets, setRecentTickets] = useState([])
  const [recentTicketsLoading, setRecentTicketsLoading] = useState(true)
  const [recentTicketsError, setRecentTicketsError] = useState(null)

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
  }, [loadReviews, session, t])

  useEffect(() => {
    loadProfileBundle()
  }, [loadProfileBundle, refreshKey, session?.userId])

  useEffect(() => {
    if (!session?.userId) return

    let active = true
    const loadRecentTickets = async () => {
      setRecentTicketsLoading(true)
      setRecentTicketsError(null)
      try {
        const data = await gatewayClient.getWallet(session.userId, '?size=3&sort=purchaseDate,desc')
        if (!active) return
        const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
        setRecentTickets(list)
      } catch (err) {
        if (active) {
          setRecentTickets([])
          setRecentTicketsError(err.message || t('tickets_load_failed'))
        }
      } finally {
        if (active) setRecentTicketsLoading(false)
      }
    }

    loadRecentTickets()
    return () => {
      active = false
    }
  }, [session?.userId, t])

  const suggestionLineMap = useMemo(() => {
    const map = new Map()
    availableLines.forEach((line) => {
      map.set(String(line.code).trim().toLowerCase(), line)
    })
    return map
  }, [availableLines])

  const preferenceThemePreview = useMemo(() => {
    const mode = preferenceForm.themeMode
    if (mode === 'dark') return 'dark'
    if (mode === 'system') {
      return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light'
    }
    return 'light'
  }, [preferenceForm.themeMode])

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
      setMsg({ type: 'success', text: t('profile_updated') })
      return updated
    } catch (saveError) {
      setMsg({ type: 'error', text: saveError.message || t('profile_update_failed') })
      throw saveError
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
      const updated = await savePreferences(payload)
      setPreferenceForm({
        languageCode: toLanguagePreference(updated.languageCode),
        themeMode: toThemePreference(updated.themeMode),
        notificationChannel: toNotificationPreference(updated.notificationChannel),
        highContrastEnabled: updated.highContrastEnabled,
        largeTextEnabled: updated.largeTextEnabled,
        screenReaderEnabled: updated.screenReaderEnabled,
      })
      setProfileData((current) => (current ? { ...current, preference: updated } : current))
      setMsg({ type: 'success', text: t('preferences_updated') })
    } catch (saveError) {
      setMsg({ type: 'error', text: saveError.message || t('preferences_update_failed') })
    } finally {
      setSavingPreferences(false)
    }
  }

  async function handleLoyaltyCouponGenerate(event) {
    event.preventDefault()
    setMsg(null)

    if (couponForm.couponType === 'FREE_RIDE' && !couponForm.rideCode) {
      setMsg({ type: 'error', text: 'Choose a ride for the free ride coupon.' })
      return
    }
    if (couponForm.couponType === 'FREE_RIDE' && !profileData?.loyaltyFreeRideEligible) {
      setMsg({ type: 'error', text: 'Free ride coupons are only available at the highest tier.' })
      return
    }
    if (loyaltyBalance <= 0) {
      setMsg({ type: 'error', text: 'You do not have enough points to generate a coupon.' })
      return
    }

    try {
      const generated = await transitApi.generateUserLoyaltyCoupon(session.userId, {
        couponType: couponForm.couponType,
        rideCode: couponForm.couponType === 'FREE_RIDE' ? couponForm.rideCode : null,
      })
      setCouponForm(DEFAULT_COUPON_FORM)
      setGeneratedCoupon(generated)
      setRefreshKey((current) => current + 1)
      setMsg({ type: 'success', text: 'Your coupon was generated successfully.' })
    } catch (couponError) {
      setMsg({ type: 'error', text: couponError.message || 'Coupon generation failed.' })
    }
  }

  async function handleEditSubscription(subId, data) {
    setProcessing(true)
    setMsg(null)
    try {
      await transitApi.updateSubscription(subId, {
        startInterval: normalizeTimeInput(data.startInterval),
        endInterval: normalizeTimeInput(data.endInterval),
        daysOfWeek: data.daysOfWeek,
      })
      setRefreshKey((current) => current + 1)
      refreshSubscriptions()
      setMsg({ type: 'success', text: t('sub_updated_ok') })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || t('sub_update_failed') })
    } finally {
      setProcessing(false)
    }
  }

  async function handleToggleSubscriptionActive(sub) {
    setProcessing(true)
    setMsg(null)
    try {
      if (sub.isActive) {
        await transitApi.unsubscribeFromLine(sub.id)
      } else {
        await transitApi.reactivateSubscription(sub.id)
      }
      setRefreshKey((current) => current + 1)
      refreshSubscriptions()
      setMsg({ type: 'success', text: sub.isActive ? t('sub_deactivated') : t('sub_reactivated') })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || t('op_failed') })
    } finally {
      setProcessing(false)
    }
  }

  async function handleDeleteSubscription(subId) {
    setProcessing(true)
    setMsg(null)
    try {
      await transitApi.deleteSubscription(subId)
      setRefreshKey((current) => current + 1)
      refreshSubscriptions()
      setMsg({ type: 'success', text: t('sub_deleted') })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || t('sub_delete_failed') })
    } finally {
      setProcessing(false)
    }
  }

  return {
    loading,
    error,
    msg,
    setMsg,
    refresh,
    profileData,
    setProfileData,
    subscriptions,
    setSubscriptions,
    tickets,
    reviews,
    setReviews,
    travelHistory,
    suggestions,
    availableLines,
    suggestionLineMap,
    loyaltyBalance,
    loyaltyTransactions,
    loyaltyCoupons,
    generatedCoupon,
    setGeneratedCoupon,
    recentTickets,
    recentTicketsLoading,
    recentTicketsError,
    profileForm,
    setProfileForm,
    passwordForm,
    setPasswordForm,
    preferenceForm,
    setPreferenceForm,
    couponForm,
    setCouponForm,
    savingProfile,
    savingPassword,
    savingPreferences,
    processing,
    preferenceThemePreview,
    loadReviews,
    handleProfileSave,
    handlePasswordSave,
    handlePreferencesSave,
    handleLoyaltyCouponGenerate,
    handleEditSubscription,
    handleToggleSubscriptionActive,
    handleDeleteSubscription,
  }
}
