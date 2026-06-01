import { Languages, LockKeyhole, Loader2, Moon, Pencil, Settings2, ShieldCheck, SunMedium, User } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../../components/common/PanelCard'

const LANGUAGE_OPTIONS = [
  { value: 'bosnian', label: 'Bosnian' },
  { value: 'croatian', label: 'Croatian' },
  { value: 'serbian', label: 'Serbian' },
  { value: 'english', label: 'English' },
]

const THEME_OPTIONS = [
  { value: 'light', label: 'Light mode' },
  { value: 'dark', label: 'Dark mode' },
  { value: 'system', label: 'System default' },
]

const NOTIFICATION_OPTIONS = [
  { value: 'push notifications', label: 'Push notifications' },
  { value: 'email', label: 'Email' },
  { value: 'sms', label: 'SMS' },
]

export function ProfileAccount({
  profileData,
  profileForm,
  setProfileForm,
  passwordForm,
  setPasswordForm,
  preferenceForm,
  setPreferenceForm,
  preferenceThemePreview,
  savingProfile,
  savingPassword,
  savingPreferences,
  handleProfileSave,
  handlePasswordSave,
  handlePreferencesSave,
}) {
  const { t } = useTranslation('profile')
  const profileName = profileData?.fullName || ''
  const profileEmail = profileData?.email || ''

  return (
    <div className="space-y-4">
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
            {(profileName || profileEmail) && profileForm.fullName === profileName && profileForm.email === profileEmail ? (
              <p className="text-xs text-muted">No changes to save.</p>
            ) : null}
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

      <PanelCard tone="default">
        <div className="flex items-center gap-2">
          <Settings2 size={18} className="text-accent" aria-hidden="true" />
          <h3 className="text-base font-semibold text-ink">{t('preferences')}</h3>
        </div>
        <p className="mt-1 text-sm text-muted">
          {t('preview_text', { theme: preferenceThemePreview, language: preferenceForm.languageCode })}
        </p>

        <form className="mt-4 space-y-5" onSubmit={handlePreferencesSave}>
          <div className="grid gap-4 sm:grid-cols-3">
            <label className="block">
              <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">{t('language')}</span>
              <div className="relative mt-1">
                <Languages size={14} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" />
                <select
                  value={preferenceForm.languageCode}
                  onChange={(event) => setPreferenceForm((current) => ({ ...current, languageCode: event.target.value }))}
                  className="w-full appearance-none rounded-lg border border-border bg-surface py-2 pl-9 pr-3 text-sm text-ink outline-none ring-accent/30 focus:ring"
                >
                  {LANGUAGE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </div>
            </label>

            <label className="block">
              <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">{t('theme')}</span>
              <div className="relative mt-1">
                {preferenceForm.themeMode === 'dark' ? (
                  <Moon size={14} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" />
                ) : (
                  <SunMedium size={14} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" />
                )}
                <select
                  value={preferenceForm.themeMode}
                  onChange={(event) => setPreferenceForm((current) => ({ ...current, themeMode: event.target.value }))}
                  className="w-full appearance-none rounded-lg border border-border bg-surface py-2 pl-9 pr-3 text-sm text-ink outline-none ring-accent/30 focus:ring"
                >
                  {THEME_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </div>
            </label>

            <label className="block">
              <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">{t('notification_channel')}</span>
              <select
                value={preferenceForm.notificationChannel}
                onChange={(event) => setPreferenceForm((current) => ({ ...current, notificationChannel: event.target.value }))}
                className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
              >
                {NOTIFICATION_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
          </div>

          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">{t('accessibility')}</p>
            <div className="mt-2 grid gap-2 sm:grid-cols-3">
              <label className="flex items-center gap-2 rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm text-ink">
                <input
                  type="checkbox"
                  checked={preferenceForm.highContrastEnabled}
                  onChange={(event) => setPreferenceForm((current) => ({ ...current, highContrastEnabled: event.target.checked }))}
                  className="h-4 w-4 rounded border-border text-accent focus:ring-accent"
                />
                {t('high_contrast')}
              </label>
              <label className="flex items-center gap-2 rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm text-ink">
                <input
                  type="checkbox"
                  checked={preferenceForm.largeTextEnabled}
                  onChange={(event) => setPreferenceForm((current) => ({ ...current, largeTextEnabled: event.target.checked }))}
                  className="h-4 w-4 rounded border-border text-accent focus:ring-accent"
                />
                {t('large_text')}
              </label>
              <label className="flex items-center gap-2 rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm text-ink">
                <input
                  type="checkbox"
                  checked={preferenceForm.screenReaderEnabled}
                  onChange={(event) => setPreferenceForm((current) => ({ ...current, screenReaderEnabled: event.target.checked }))}
                  className="h-4 w-4 rounded border-border text-accent focus:ring-accent"
                />
                {t('screen_reader')}
              </label>
            </div>
          </div>

          <button
            type="submit"
            disabled={savingPreferences}
            className="inline-flex items-center gap-2 rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-70"
          >
            {savingPreferences ? <Loader2 size={14} className="animate-spin" aria-hidden="true" /> : <ShieldCheck size={14} aria-hidden="true" />}
            {t('save_preferences')}
          </button>
        </form>
      </PanelCard>
    </div>
  )
}
