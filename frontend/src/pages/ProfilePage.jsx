import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, Navigate } from 'react-router-dom'
import { Loader2, AlertCircle, RefreshCw, Trash2, Pencil, Clock, CalendarDays, Star, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { SubscriptionModal } from '../components/common/SubscriptionModal'
import { ReviewForm } from '../components/reviews/ReviewForm'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

function formatTime(time) {
  if (!time) return '--:--'
  return time.length > 5 ? time.slice(0, 5) : time
}

function formatDays(daysStr, labels) {
  if (!daysStr) return ''
  return daysStr.split(',').map((d) => labels[d.trim()] || d.trim()).filter(Boolean).join(' · ')
}

import { gatewayClient } from '../services/gatewayClient'

const TICKET_STATUS_STYLES = {
  ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  USED: 'bg-surface-alt text-muted',
  EXPIRED: 'bg-surface-alt text-muted',
  CANCELLED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

export function ProfilePage() {
  const { isAuthenticated, session, logout, refreshSubscriptions } = useAppContext()
  const { t } = useTranslation('profile')
  const navigate = useNavigate()

  const DAY_LABELS = {
    MON: t('day_mon'), TUE: t('day_tue'), WED: t('day_wed'),
    THU: t('day_thu'), FRI: t('day_fri'), SAT: t('day_sat'), SUN: t('day_sun'),
  }

  const [subscriptions, setSubscriptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [processing, setProcessing] = useState(false)
  const [msg, setMsg] = useState(null)

  const [reviewTarget, setReviewTarget] = useState(null)

  const fetchSubscriptions = useCallback(async () => {
    if (!session?.userId) return
    setLoading(true)
    setError(null)
    try {
      const data = await transitApi.getAllUserSubscriptions(session.userId)
      setSubscriptions(data || [])
    } catch (err) {
      setError(err.message || 'Failed to load subscriptions')
    } finally {
      setLoading(false)
    }
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

  // ... inside ProfilePage component

  async function handleEditSave(data) {
    if (!editTarget) return
    
    const normalizeTime = (timeStr) => {
      if (!timeStr) return null;
      const parts = timeStr.split(':');
      const hh = parts[0].padStart(2, '0');
      const mm = (parts[1] || '00').padStart(2, '0');
      return `${hh}:${mm}:00`;
    };

    try {
      setProcessing(true); 
      await transitApi.updateSubscription(editTarget.id, {
        startInterval: normalizeTime(data.startInterval),
        endInterval: normalizeTime(data.endInterval),
        daysOfWeek: data.daysOfWeek,
      })
      
      setEditTarget(null); 
      await fetchSubscriptions()
      refreshSubscriptions()
      setMsg({ type: 'success', text: t('sub_updated_ok') })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || t('sub_update_failed') })
    } finally {
      setProcessing(false);
    }
  }

  async function handleToggleActive(sub) {
    setProcessing(true)
    setMsg(null)
    try {
      if (sub.isActive) {
        await transitApi.unsubscribeFromLine(sub.id)
      } else {
        await transitApi.reactivateSubscription(sub.id)
      }
      await fetchSubscriptions()
      refreshSubscriptions()
      setMsg({ type: 'success', text: sub.isActive ? t('sub_deactivated') : t('sub_reactivated') })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || t('op_failed') })
    } finally {
      setProcessing(false)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setProcessing(true)
    setMsg(null)
    try {
      await transitApi.deleteSubscription(deleteTarget.id)
      setDeleteTarget(null)
      await fetchSubscriptions()
      refreshSubscriptions()
      setMsg({ type: 'success', text: t('sub_deleted') })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || t('sub_delete_failed') })
    } finally {
      setProcessing(false)
    }
  }

  return (
    <div className="space-y-4">
      {msg && msg.type === 'success' && (
        <SuccessAlert message={msg.text} onDismiss={() => setMsg(null)} />
      )}
      {msg && msg.type === 'error' && (
        <ErrorAlert error={msg.text} onDismiss={() => setMsg(null)} />
      )}

      <PanelCard tone="soft">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-ink">{session.fullName || session.email}</h2>
            <p className="mt-1 text-sm text-muted">{session.email}</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => navigate('/auth')}
              className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
            >
              {t('account')}
            </button>
            <button
              type="button"
              onClick={logout}
              className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
            >
              {t('logout')}
            </button>
          </div>
        </div>
      </PanelCard>

      <div className="grid gap-4 lg:grid-cols-2">
        <PanelCard tone="default">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-semibold text-ink">{t('subscriptions')}</h3>
            <button
              type="button"
              onClick={fetchSubscriptions}
              disabled={loading}
              className="rounded-lg border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink disabled:opacity-50"
              aria-label={t('refresh_subs')}
            >
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            </button>
          </div>

          <div className="mt-3 space-y-3 text-sm">
            {loading && subscriptions.length === 0 ? (
              <div className="flex items-center justify-center gap-2 py-8 text-muted">
                <Loader2 size={16} className="animate-spin" aria-hidden="true" />
                {t('loading_subs')}
              </div>
            ) : error ? (
              <div className="flex flex-col items-center gap-3 py-6">
                <AlertCircle size={24} className="text-red-500" aria-hidden="true" />
                <p className="text-sm text-muted">{error}</p>
                <button
                  type="button"
                  onClick={fetchSubscriptions}
                  className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-1.5 text-xs font-medium text-white"
                >
                  <RefreshCw size={12} aria-hidden="true" />
                  {t('retry')}
                </button>
              </div>
            ) : subscriptions.length === 0 ? (
              <p className="py-4 text-center text-muted">{t('no_subs')}</p>
            ) : (
              <div className="space-y-2">
                {subscriptions.map((sub) => (
                  <div
                    key={sub.id}
                    className={`rounded-lg border px-3 py-3 transition ${
                      sub.isActive
                        ? 'border-border bg-surface-soft'
                        : 'border-border/50 bg-surface-soft/60'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <LineBadge line={{ code: sub.lineCode || String(sub.lineId), vehicleTypeName: 'bus', name: sub.lineName || `Line ${sub.lineId}` }} />
                          {sub.isActive ? (
                            <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-semibold text-green-700 dark:bg-green-900/30 dark:text-green-400">{t('active')}</span>
                          ) : (
                            <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-semibold text-gray-500 dark:bg-gray-800 dark:text-gray-400">{t('inactive')}</span>
                          )}
                        </div>
                        <p className="mt-1.5 font-semibold text-ink">{sub.lineName || `Line ${sub.lineId}`}</p>

                        <div className="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted">
                          <span className="inline-flex items-center gap-1">
                            <Clock size={12} />
                            {formatTime(sub.startInterval)} – {formatTime(sub.endInterval)}
                          </span>
                          <span className="inline-flex items-center gap-1">
                            <CalendarDays size={12} />
                            {formatDays(sub.daysOfWeek, DAY_LABELS)}
                          </span>
                        </div>

                        <p className="mt-1 text-[11px] text-muted">{t('created', { date: formatDate(sub.createdAt) })}</p>
                      </div>

                      <div className="flex flex-col gap-1">
                        <button
                          type="button"
                          onClick={() => setEditTarget(sub)}
                          className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt"
                          aria-label={t('edit_sub')}
                        >
                          <Pencil size={12} aria-hidden="true" />
                          <span className="text-ink">{t('edit')}</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => handleToggleActive(sub)}
                          disabled={processing}
                          className="rounded-md border border-border px-2 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt disabled:opacity-50"
                          aria-label={sub.isActive ? t('deactivate') : t('activate')}
                        >
                          {sub.isActive ? (
                            <span className="text-gray-500">{t('deactivate')}</span>
                          ) : (
                            <span className="text-green-600">{t('activate')}</span>
                          )}
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteTarget(sub)}
                          className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs font-medium text-red-600 transition hover:bg-surface-alt"
                          aria-label={t('delete_sub')}
                        >
                          <Trash2 size={12} aria-hidden="true" />
                          <span className="text-red-600">{t('delete')}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </PanelCard>

        <PanelCard tone="default">
          <h3 className="text-base font-semibold text-ink">{t('trip_history')}</h3>
          <div className="mt-3 space-y-2 text-sm">
            <p className="text-muted">{t('no_trips')}</p>
          </div>
        </PanelCard>
      </div>

      <SubscriptionModal
        isOpen={editTarget !== null}
        onClose={() => setEditTarget(null)}
        onSubmit={handleEditSave}
        lineName={editTarget?.lineName}
        mode="edit"
        initialValues={editTarget ? { startInterval: editTarget.startInterval, endInterval: editTarget.endInterval, daysOfWeek: editTarget.daysOfWeek } : null}
      />

      {deleteTarget !== null && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          onClick={() => !processing && setDeleteTarget(null)}
        >
          <div
            className="w-full max-w-sm rounded-panel border border-border bg-surface p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-2">
              <Trash2 size={18} className="text-red-500" aria-hidden="true" />
              <h2 className="text-base font-semibold text-ink">{t('delete_sub')}</h2>
            </div>
            <p className="mb-5 mt-2 text-sm text-muted">
              {t('delete_confirm', { line: deleteTarget.lineName || `Line ${deleteTarget.lineId}` })}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleDelete}
                disabled={processing}
                className="flex-1 rounded-panel border border-red-500 bg-red-500 py-2 text-sm font-medium text-white disabled:opacity-50"
              >
                {processing ? (
                  <span className="inline-flex items-center gap-2">
                    <Loader2 size={14} className="animate-spin" aria-hidden="true" />
                    {t('deleting')}
                  </span>
                ) : t('delete')}
              </button>
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                disabled={processing}
                className="flex-1 rounded-panel border border-border py-2 text-sm font-medium text-ink hover:bg-surface-alt disabled:opacity-50"
              >
                {t('cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      <PanelCard tone="default">
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
                  <div className="flex items-center gap-2 min-w-0">
                    <LineBadge line={{ code: sub.lineCode || String(sub.lineId), vehicleTypeName: 'bus', name: sub.lineName || `Line ${sub.lineId}` }} />
                    <span className="truncate text-sm text-ink">{sub.lineName || `Line ${sub.lineId}`}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setReviewTarget(sub)}
                    className="ml-2 flex-shrink-0 inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt"
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

      {/* ── Review modal ─────────────────────────────────────────────────── */}
      {reviewTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          onClick={() => setReviewTarget(null)}
        >
          <div
            className="w-full max-w-md rounded-panel border border-border bg-surface p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
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
              onSuccess={() => setReviewTarget(null)}
            />
          </div>
        </div>
      )}
    </div>
  )
}
