import { useCallback } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { RefreshCw, Route } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { LoadingSpinner } from '../components/common/LoadingStates'
import { PanelCard } from '../components/common/PanelCard'
import { useAppContext } from '../context/AppContext'
import { ProfileAccount } from './profile/ProfileAccount'
import { ProfileActivity } from './profile/ProfileActivity'
import { ProfileSubscriptions } from './profile/ProfileSubscriptions'
import { ProfileTabs } from './profile/ProfileTabs'
import { ProfileWallet } from './profile/ProfileWallet'
import { useProfileData } from './profile/useProfileData'

const VALID_TABS = new Set(['account', 'subscriptions', 'activity', 'wallet'])

export function ProfilePage() {
  const {
    isAuthenticated,
    session,
    login,
    logout,
    tripHistory,
    refreshSubscriptions,
    savePreferences,
  } = useAppContext()
  const { t } = useTranslation('profile')
  const [searchParams, setSearchParams] = useSearchParams()
  const tabParam = searchParams.get('tab')
  const activeTab = VALID_TABS.has(tabParam) ? tabParam : 'account'

  const data = useProfileData({
    session,
    refreshSubscriptions,
    savePreferences,
    t,
  })

  const {
    loading,
    error,
    msg,
    setMsg,
    refresh,
    profileData,
    subscriptions,
    tickets,
    reviews,
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
  } = data

  const setActiveTab = useCallback(
    (nextTab) => {
      setSearchParams(
        (current) => {
          const params = new URLSearchParams(current)
          if (nextTab === 'account') params.delete('tab')
          else params.set('tab', nextTab)
          return params
        },
        { replace: true },
      )
    },
    [setSearchParams],
  )

  if (!isAuthenticated) return <Navigate to="/auth" replace />

  const profileName = profileData?.fullName || session.fullName || session.email
  const profileEmail = profileData?.email || session.email

  async function handleProfileSaveWithSession(event) {
    try {
      const updated = await handleProfileSave(event)
      if (updated) {
        login({ ...session, fullName: updated.fullName, email: updated.email })
      }
    } catch {
      // error already surfaced via msg
    }
  }

  return (
    <div className="space-y-4">
      {msg?.type === 'success' && <SuccessAlert message={msg.text} onDismiss={() => setMsg(null)} />}
      {msg?.type === 'error' && <ErrorAlert error={msg.text} onDismiss={() => setMsg(null)} />}
      {error && <ErrorAlert error={error} onDismiss={() => setMsg(null)} />}

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
              onClick={refresh}
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

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      <div
        role="tabpanel"
        id={`profile-panel-${activeTab}`}
        aria-labelledby={`profile-tab-${activeTab}`}
        tabIndex={0}
      >
        {loading ? (
          <PanelCard>
            <LoadingSpinner label={t('loading_profile')} />
          </PanelCard>
        ) : activeTab === 'account' ? (
          <ProfileAccount
            profileData={profileData}
            profileForm={profileForm}
            setProfileForm={setProfileForm}
            passwordForm={passwordForm}
            setPasswordForm={setPasswordForm}
            preferenceForm={preferenceForm}
            setPreferenceForm={setPreferenceForm}
            preferenceThemePreview={preferenceThemePreview}
            savingProfile={savingProfile}
            savingPassword={savingPassword}
            savingPreferences={savingPreferences}
            handleProfileSave={handleProfileSaveWithSession}
            handlePasswordSave={handlePasswordSave}
            handlePreferencesSave={handlePreferencesSave}
          />
        ) : activeTab === 'subscriptions' ? (
          <ProfileSubscriptions
            subscriptions={subscriptions}
            suggestions={suggestions}
            suggestionLineMap={suggestionLineMap}
            session={session}
            loading={loading}
            processing={processing}
            refresh={refresh}
            loadReviews={loadReviews}
            handleEditSubscription={handleEditSubscription}
            handleToggleSubscriptionActive={handleToggleSubscriptionActive}
            handleDeleteSubscription={handleDeleteSubscription}
          />
        ) : activeTab === 'activity' ? (
          <ProfileActivity
            travelHistory={travelHistory}
            tripHistory={tripHistory}
            tickets={tickets}
            reviews={reviews}
            availableLines={availableLines}
          />
        ) : (
          <ProfileWallet
            recentTickets={recentTickets}
            recentTicketsLoading={recentTicketsLoading}
            recentTicketsError={recentTicketsError}
            loyaltyBalance={loyaltyBalance}
            loyaltyTransactions={loyaltyTransactions}
            loyaltyCoupons={loyaltyCoupons}
            generatedCoupon={generatedCoupon}
            setGeneratedCoupon={setGeneratedCoupon}
            profileData={profileData}
            couponForm={couponForm}
            setCouponForm={setCouponForm}
            handleLoyaltyCouponGenerate={handleLoyaltyCouponGenerate}
          />
        )}
      </div>
    </div>
  )
}
