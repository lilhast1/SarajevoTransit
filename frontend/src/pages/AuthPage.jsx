import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../components/common/PanelCard'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

const initialState = {
  fullName: '',
  email: '',
  password: '',
}

function validateEmail(email) {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return re.test(String(email).toLowerCase())
}

export function AuthPage() {
  const { isAuthenticated, login } = useAppContext()
  const { t } = useTranslation('auth')
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState(initialState)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const normalizedEmail = form.email.trim()
  const emailValid = validateEmail(normalizedEmail)
  const showInlineEmailError = normalizedEmail.length > 0 && !emailValid

  if (isAuthenticated) return <Navigate to="/profile" replace />

  const onChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const onSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      if (!emailValid) {
        setError(t('invalid_email'))
        return
      }
      const payload =
        mode === 'login'
          ? await transitApi.login({ email: normalizedEmail, password: form.password })
          : await transitApi.register({
              fullName: form.fullName,
              email: normalizedEmail,
              password: form.password,
            })
      login(payload)
    } catch (submitError) {
      setError(submitError.message || t('auth_failed'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <PanelCard tone="soft">
        <h2 className="text-xl font-semibold text-ink">{t('title')}</h2>
        <p className="mt-1 text-sm text-muted">{t('subtitle')}</p>

        <div className="mt-4 inline-flex rounded-lg border border-border bg-surface-soft p-1">
          <button
            type="button"
            onClick={() => setMode('login')}
            className={`rounded-md px-3 py-1 text-sm font-medium transition ${
              mode === 'login' ? 'bg-accent text-white' : 'text-muted hover:text-ink'
            }`}
          >
            {t('login_tab')}
          </button>
          <button
            type="button"
            onClick={() => setMode('register')}
            className={`rounded-md px-3 py-1 text-sm font-medium transition ${
              mode === 'register' ? 'bg-accent text-white' : 'text-muted hover:text-ink'
            }`}
          >
            {t('register_tab')}
          </button>
        </div>

        <form onSubmit={onSubmit} className="mt-4 space-y-3">
          {mode === 'register' ? (
            <div>
              <label htmlFor="auth-full-name" className="mb-1 block text-xs font-semibold uppercase tracking-[0.1em] text-muted">
                {t('full_name')}
              </label>
              <input
                id="auth-full-name"
                name="fullName"
                value={form.fullName}
                onChange={onChange}
                placeholder={t('full_name_placeholder')}
                className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
                required
              />
            </div>
          ) : null}

          <div>
            <label htmlFor="auth-email" className="mb-1 block text-xs font-semibold uppercase tracking-[0.1em] text-muted">
              {t('email')}
            </label>
            <input
              id="auth-email"
              name="email"
              type="email"
              value={form.email}
              onChange={onChange}
              placeholder={t('email_placeholder')}
              aria-invalid={showInlineEmailError}
              aria-describedby={showInlineEmailError ? 'auth-email-error' : undefined}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
              required
            />
            {showInlineEmailError ? (
              <p id="auth-email-error" className="mt-1 text-xs text-accent">
                {t('invalid_email')}
              </p>
            ) : null}
          </div>

          <div>
            <label htmlFor="auth-password" className="mb-1 block text-xs font-semibold uppercase tracking-[0.1em] text-muted">
              {t('password')}
            </label>
            <input
              id="auth-password"
              name="password"
              type="password"
              value={form.password}
              onChange={onChange}
              placeholder={t('password_placeholder')}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
              required
            />
          </div>

          {error ? <p role="alert" className="text-sm text-accent">{error}</p> : null}

          <button
            type="submit"
            disabled={loading || !emailValid}
            className="w-full rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-70"
          >
            {loading ? t('please_wait') : mode === 'login' ? t('login_button') : t('register_button')}
          </button>
        </form>
      </PanelCard>
    </div>
  )
}
