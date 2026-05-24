import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAppContext } from '../../context/AppContext'

export function SessionExpiryModal() {
  const { sessionModal, refreshSession, logout } = useAppContext()
  const { t } = useTranslation('common')
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)

  if (!sessionModal.open) return null

  async function handleExtend() {
    setLoading(true)
    await refreshSession()
    setLoading(false)
    navigate(0)
  }

  function handleLoginAgain() {
    logout()
    navigate('/auth')
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="session-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
    >
      <div className="w-full max-w-sm rounded-panel border border-border bg-surface p-6 shadow-xl">
        <div className="mb-1 flex items-center gap-2">
          <span className="text-lg" aria-hidden="true">🔒</span>
          <h2 id="session-modal-title" className="text-base font-semibold text-ink">
            {sessionModal.refreshExpired ? t('session_expired_title') : t('session_expiring_title')}
          </h2>
        </div>

        <p className="mb-5 mt-2 text-sm text-muted">
          {sessionModal.refreshExpired ? t('session_expired_msg') : t('session_expiring_msg')}
        </p>

        <div className="flex flex-col gap-2">
          {!sessionModal.refreshExpired && (
            <button
              type="button"
              onClick={handleExtend}
              disabled={loading}
              className="w-full rounded-panel border border-accent bg-accent py-2 text-sm font-medium text-white disabled:opacity-60"
            >
              {loading ? t('extending') : t('extend_session')}
            </button>
          )}
          <button
            type="button"
            onClick={handleLoginAgain}
            className={`w-full rounded-panel border py-2 text-sm font-medium ${
              sessionModal.refreshExpired
                ? 'border-accent bg-accent text-white'
                : 'border-border text-muted hover:bg-surface-alt'
            }`}
          >
            {t('login_again')}
          </button>
        </div>
      </div>
    </div>
  )
}
