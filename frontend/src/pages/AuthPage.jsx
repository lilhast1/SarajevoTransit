import { useState } from 'react'
import { Navigate } from 'react-router-dom'
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
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState(initialState)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

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
      if (!validateEmail(form.email)) {
        setError('Please enter a valid email address')
        return
      }
      const payload =
        mode === 'login'
          ? await transitApi.login({ email: form.email, password: form.password })
          : await transitApi.register({
              fullName: form.fullName,
              email: form.email,
              password: form.password,
            })
      login(payload)
    } catch (submitError) {
      setError(submitError.message || 'Authentication failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <PanelCard tone="soft">
        <h2 className="text-xl font-semibold text-ink">Login / Register</h2>
        <p className="mt-1 text-sm text-muted">
          Optional authentication. App remains fully available in guest mode.
        </p>

        <div className="mt-4 inline-flex rounded-lg border border-border bg-surface-soft p-1">
          {['login', 'register'].map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => setMode(option)}
              className={`rounded-md px-3 py-1 text-sm font-medium transition ${
                mode === option ? 'bg-accent text-white' : 'text-muted hover:text-ink'
              }`}
            >
              {option[0].toUpperCase() + option.slice(1)}
            </button>
          ))}
        </div>

        <form onSubmit={onSubmit} className="mt-4 space-y-3">
          {mode === 'register' ? (
            <div>
              <label htmlFor="auth-full-name" className="mb-1 block text-xs font-semibold uppercase tracking-[0.1em] text-muted">
                Full name
              </label>
              <input
                id="auth-full-name"
                name="fullName"
                value={form.fullName}
                onChange={onChange}
                placeholder="Full name"
                className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
                required
              />
            </div>
          ) : null}

          <div>
            <label htmlFor="auth-email" className="mb-1 block text-xs font-semibold uppercase tracking-[0.1em] text-muted">
              Email
            </label>
            <input
              id="auth-email"
              name="email"
              type="email"
              value={form.email}
              onChange={onChange}
              placeholder="Email"
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
              required
            />
          </div>

          <div>
            <label htmlFor="auth-password" className="mb-1 block text-xs font-semibold uppercase tracking-[0.1em] text-muted">
              Password
            </label>
            <input
              id="auth-password"
              name="password"
              type="password"
              value={form.password}
              onChange={onChange}
              placeholder="Password"
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink"
              required
            />
          </div>

          {error ? <p className="text-sm text-accent">{error}</p> : null}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-70"
          >
            {loading ? 'Please wait...' : mode === 'login' ? 'Login' : 'Create account'}
          </button>
        </form>
      </PanelCard>
    </div>
  )
}
