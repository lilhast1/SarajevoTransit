export function mapLanguageCode(languageCode) {
  const normalized = String(languageCode || 'EN').toUpperCase()
  if (normalized === 'BS') return 'bs'
  if (normalized === 'SR') return 'sr'
  if (normalized === 'HR') return 'hr'
  return 'en'
}

export function toApiLanguageCode(i18nCode) {
  const normalized = String(i18nCode || '').toLowerCase()
  if (normalized === 'bs') return 'BS'
  if (normalized === 'sr') return 'SR'
  if (normalized === 'hr') return 'HR'
  return 'EN'
}

export function toLanguagePreference(languageCode) {
  const normalized = String(languageCode || '').toLowerCase()
  if (normalized === 'bosnian' || normalized === 'bs') return 'bosnian'
  if (normalized === 'croatian' || normalized === 'hr') return 'croatian'
  if (normalized === 'serbian' || normalized === 'sr') return 'serbian'
  return 'english'
}

export function fromLanguagePreference(languagePreference) {
  const normalized = String(languagePreference || '').toLowerCase()
  if (normalized === 'bosnian') return 'BS'
  if (normalized === 'croatian') return 'HR'
  if (normalized === 'serbian') return 'SR'
  return 'EN'
}

export function toThemePreference(themeMode) {
  const normalized = String(themeMode || '').toLowerCase()
  if (normalized === 'dark') return 'dark'
  if (normalized === 'system') return 'system'
  return 'light'
}

export function toApiThemeMode(themeMode) {
  const normalized = String(themeMode || '').toLowerCase()
  if (normalized === 'dark') return 'DARK'
  return 'LIGHT'
}

export function fromThemePreference(themePreference) {
  const normalized = String(themePreference || '').toLowerCase()
  if (normalized === 'dark') return 'DARK'
  if (normalized === 'system') return 'SYSTEM'
  return 'LIGHT'
}

export function toNotificationPreference(channel) {
  const normalized = String(channel || '').toLowerCase()
  if (normalized === 'email') return 'email'
  if (normalized === 'sms' || normalized === 'text message') return 'sms'
  return 'push notifications'
}

export function fromNotificationPreference(channel) {
  const normalized = String(channel || '').toLowerCase()
  if (normalized === 'email') return 'EMAIL'
  if (normalized === 'sms') return 'SMS'
  return 'PUSH'
}

export function resolveCurrentTheme(themeMode, preferredTheme) {
  const systemTheme =
    typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light'
  if (themeMode === 'LIGHT') return 'light'
  if (themeMode === 'DARK') return 'dark'
  if (themeMode === 'SYSTEM') return systemTheme
  return preferredTheme
}

export function arePreferencesEqual(a, b) {
  if (!a || !b) return false
  return (
    a.languageCode === b.languageCode &&
    a.themeMode === b.themeMode &&
    a.notificationChannel === b.notificationChannel &&
    Boolean(a.highContrastEnabled) === Boolean(b.highContrastEnabled) &&
    Boolean(a.largeTextEnabled) === Boolean(b.largeTextEnabled) &&
    Boolean(a.screenReaderEnabled) === Boolean(b.screenReaderEnabled)
  )
}
