import { describe, it, expect, vi, afterEach } from 'vitest'
import {
  mapLanguageCode,
  toApiLanguageCode,
  toLanguagePreference,
  fromLanguagePreference,
  toThemePreference,
  toApiThemeMode,
  fromThemePreference,
  toNotificationPreference,
  fromNotificationPreference,
  arePreferencesEqual,
  resolveCurrentTheme,
} from '../../utils/preferenceMappers'

describe('mapLanguageCode', () => {
  it('maps BS to bs', () => expect(mapLanguageCode('BS')).toBe('bs'))
  it('maps SR to sr', () => expect(mapLanguageCode('SR')).toBe('sr'))
  it('maps HR to hr', () => expect(mapLanguageCode('HR')).toBe('hr'))
  it('maps EN to en', () => expect(mapLanguageCode('EN')).toBe('en'))
  it('maps null to en', () => expect(mapLanguageCode(null)).toBe('en'))
  it('maps unknown to en', () => expect(mapLanguageCode('ZZ')).toBe('en'))
})

describe('toApiLanguageCode', () => {
  it('maps bs to BS', () => expect(toApiLanguageCode('bs')).toBe('BS'))
  it('maps sr to SR', () => expect(toApiLanguageCode('sr')).toBe('SR'))
  it('maps hr to HR', () => expect(toApiLanguageCode('hr')).toBe('HR'))
  it('maps en to EN', () => expect(toApiLanguageCode('en')).toBe('EN'))
  it('maps null to EN', () => expect(toApiLanguageCode(null)).toBe('EN'))
})

describe('toLanguagePreference', () => {
  it('maps bs to bosnian', () => expect(toLanguagePreference('bs')).toBe('bosnian'))
  it('maps hr to croatian', () => expect(toLanguagePreference('hr')).toBe('croatian'))
  it('maps sr to serbian', () => expect(toLanguagePreference('sr')).toBe('serbian'))
  it('maps unknown to english', () => expect(toLanguagePreference('zz')).toBe('english'))
})

describe('fromLanguagePreference', () => {
  it('maps bosnian to BS', () => expect(fromLanguagePreference('bosnian')).toBe('BS'))
  it('maps croatian to HR', () => expect(fromLanguagePreference('croatian')).toBe('HR'))
  it('maps serbian to SR', () => expect(fromLanguagePreference('serbian')).toBe('SR'))
  it('maps unknown to EN', () => expect(fromLanguagePreference('xyz')).toBe('EN'))
})

describe('toThemePreference', () => {
  it('maps dark to dark', () => expect(toThemePreference('dark')).toBe('dark'))
  it('maps system to system', () => expect(toThemePreference('system')).toBe('system'))
  it('maps light to light', () => expect(toThemePreference('light')).toBe('light'))
  it('maps null to light', () => expect(toThemePreference(null)).toBe('light'))
})

describe('toApiThemeMode', () => {
  it('maps dark to DARK', () => expect(toApiThemeMode('dark')).toBe('DARK'))
  it('maps light to LIGHT', () => expect(toApiThemeMode('light')).toBe('LIGHT'))
  it('maps null to LIGHT', () => expect(toApiThemeMode(null)).toBe('LIGHT'))
})

describe('fromThemePreference', () => {
  it('maps dark to DARK', () => expect(fromThemePreference('dark')).toBe('DARK'))
  it('maps system to SYSTEM', () => expect(fromThemePreference('system')).toBe('SYSTEM'))
  it('maps light to LIGHT', () => expect(fromThemePreference('light')).toBe('LIGHT'))
})

describe('toNotificationPreference', () => {
  it('maps email to email', () => expect(toNotificationPreference('email')).toBe('email'))
  it('maps sms to sms', () => expect(toNotificationPreference('sms')).toBe('sms'))
  it('maps unknown to push notifications', () => expect(toNotificationPreference('push')).toBe('push notifications'))
})

describe('fromNotificationPreference', () => {
  it('maps email to EMAIL', () => expect(fromNotificationPreference('email')).toBe('EMAIL'))
  it('maps sms to SMS', () => expect(fromNotificationPreference('sms')).toBe('SMS'))
  it('maps push to PUSH', () => expect(fromNotificationPreference('push')).toBe('PUSH'))
})

describe('arePreferencesEqual', () => {
  const base = {
    languageCode: 'EN',
    themeMode: 'LIGHT',
    notificationChannel: 'EMAIL',
    highContrastEnabled: false,
    largeTextEnabled: false,
    screenReaderEnabled: false,
  }

  it('returns true for identical objects', () => {
    expect(arePreferencesEqual(base, { ...base })).toBe(true)
  })

  it('returns false when languageCode differs', () => {
    expect(arePreferencesEqual(base, { ...base, languageCode: 'BS' })).toBe(false)
  })

  it('returns false when themeMode differs', () => {
    expect(arePreferencesEqual(base, { ...base, themeMode: 'DARK' })).toBe(false)
  })

  it('returns false when either is null', () => {
    expect(arePreferencesEqual(null, base)).toBe(false)
    expect(arePreferencesEqual(base, null)).toBe(false)
  })
})

describe('resolveCurrentTheme', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('returns light for LIGHT', () => {
    expect(resolveCurrentTheme('LIGHT', 'dark')).toBe('light')
  })

  it('returns dark for DARK', () => {
    expect(resolveCurrentTheme('DARK', 'light')).toBe('dark')
  })

  it('returns system theme for SYSTEM when dark', () => {
    vi.stubGlobal('window', {
      matchMedia: () => ({ matches: true }),
    })
    expect(resolveCurrentTheme('SYSTEM', 'light')).toBe('dark')
  })

  it('returns preferredTheme as fallback', () => {
    expect(resolveCurrentTheme(null, 'dark')).toBe('dark')
  })
})
