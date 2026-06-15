export {
  arePreferencesEqual,
  fromLanguagePreference,
  fromNotificationPreference,
  fromThemePreference,
  mapLanguageCode,
  resolveCurrentTheme,
  toApiLanguageCode,
  toApiThemeMode,
  toLanguagePreference,
  toNotificationPreference,
  toThemePreference,
} from '../../utils/preferenceMappers'

export const DEFAULT_PROFILE_FORM = { fullName: '', email: '' }
export const DEFAULT_PASSWORD_FORM = { newPassword: '', confirmPassword: '' }
export const DEFAULT_PREFERENCE_FORM = {
  languageCode: 'BS',
  themeMode: 'LIGHT',
  notificationChannel: 'PUSH',
  highContrastEnabled: false,
  largeTextEnabled: false,
  screenReaderEnabled: false,
}
export const DEFAULT_COUPON_FORM = { couponType: 'DISCOUNT', rideCode: '' }

export const TICKET_STATUS_STYLES = {
  ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  USED: 'bg-surface-alt text-muted',
  EXPIRED: 'bg-surface-alt text-muted',
  CANCELLED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

export const TABS = [
  { id: 'account', iconKey: 'user' },
  { id: 'subscriptions', iconKey: 'route' },
  { id: 'activity', iconKey: 'history' },
  { id: 'wallet', iconKey: 'wallet' },
]

export function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

export function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
}

export function formatTime(value) {
  if (!value) return '--:--'
  return String(value).length > 5 ? String(value).slice(0, 5) : String(value)
}

export function formatDays(daysValue, labels) {
  if (!daysValue) return ''
  return String(daysValue)
    .split(',')
    .map((item) => labels[item.trim()] || item.trim())
    .filter(Boolean)
    .join(' · ')
}

export function toPageItems(result) {
  if (!result) return []
  if (Array.isArray(result.content)) return result.content
  if (Array.isArray(result)) return result
  return []
}

export function getLineLabel(line, fallbackCode) {
  if (!line) return fallbackCode
  return `${line.code} · ${line.name}`
}

export function normalizeTimeInput(timeStr) {
  if (!timeStr) return null
  const parts = timeStr.split(':')
  const hh = parts[0].padStart(2, '0')
  const mm = (parts[1] || '00').padStart(2, '0')
  return `${hh}:${mm}:00`
}
