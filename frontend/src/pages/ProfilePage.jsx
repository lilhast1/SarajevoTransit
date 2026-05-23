import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, Navigate } from 'react-router-dom'
import { Loader2, AlertCircle, RefreshCw, Trash2, Pencil, Clock, CalendarDays } from 'lucide-react'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { SubscriptionModal } from '../components/common/SubscriptionModal'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

const DAY_LABELS = { MON: 'Mon', TUE: 'Tue', WED: 'Wed', THU: 'Thu', FRI: 'Fri', SAT: 'Sat', SUN: 'Sun' }

function formatTime(time) {
  if (!time) return '--:--'
  return time.length > 5 ? time.slice(0, 5) : time
}

function formatDays(daysStr) {
  if (!daysStr) return ''
  return daysStr.split(',').map((d) => DAY_LABELS[d.trim()] || d.trim()).filter(Boolean).join(' · ')
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
  const navigate = useNavigate()

  const [subscriptions, setSubscriptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [processing, setProcessing] = useState(false)
  const [msg, setMsg] = useState(null)

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

  useEffect(() => {
    if (!isAuthenticated || !session?.userId) return
    gatewayClient
      .getWallet(session.userId, '?size=3&sort=purchaseDate,desc')
      .then((data) => {
        const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
        setRecentTickets(list)
      })
      .catch(() => {})
      .finally(() => setTicketsLoading(false))
  }, [isAuthenticated, session?.userId])

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
      setMsg({ type: 'success', text: 'Subscription updated successfully.' })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Failed to update.' })
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
      setMsg({ type: 'success', text: sub.isActive ? 'Subscription deactivated.' : 'Subscription reactivated.' })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Operation failed.' })
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
      setMsg({ type: 'success', text: 'Subscription deleted.' })
    } catch (err) {
      setMsg({ type: 'error', text: err.message || 'Failed to delete subscription.' })
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
              Account
            </button>
            <button
              type="button"
              onClick={logout}
              className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
            >
              Logout
            </button>
          </div>
        </div>
      </PanelCard>

      <div className="grid gap-4 lg:grid-cols-2">
        <PanelCard tone="default">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-semibold text-ink">Subscriptions</h3>
            <button
              type="button"
              onClick={fetchSubscriptions}
              disabled={loading}
              className="rounded-lg border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink disabled:opacity-50"
              aria-label="Refresh subscriptions"
            >
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            </button>
          </div>

          <div className="mt-3 space-y-3 text-sm">
            {loading && subscriptions.length === 0 ? (
              <div className="flex items-center justify-center gap-2 py-8 text-muted">
                <Loader2 size={16} className="animate-spin" />
                Loading subscriptions...
              </div>
            ) : error ? (
              <div className="flex flex-col items-center gap-3 py-6">
                <AlertCircle size={24} className="text-red-500" />
                <p className="text-sm text-muted">{error}</p>
                <button
                  type="button"
                  onClick={fetchSubscriptions}
                  className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-1.5 text-xs font-medium text-white"
                >
                  <RefreshCw size={12} />
                  Retry
                </button>
              </div>
            ) : subscriptions.length === 0 ? (
              <p className="py-4 text-center text-muted">No subscriptions yet. Browse lines and subscribe to receive notifications.</p>
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
                            <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-semibold text-green-700 dark:bg-green-900/30 dark:text-green-400">Active</span>
                          ) : (
                            <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-semibold text-gray-500 dark:bg-gray-800 dark:text-gray-400">Inactive</span>
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
                            {formatDays(sub.daysOfWeek)}
                          </span>
                        </div>

                        <p className="mt-1 text-[11px] text-muted">Created {formatDate(sub.createdAt)}</p>
                      </div>

                      <div className="flex flex-col gap-1">
                        <button
                          type="button"
                          onClick={() => setEditTarget(sub)}
                          className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt"
                          aria-label="Edit subscription"
                        >
                          <Pencil size={12} />
                          <span className="text-ink">Edit</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => handleToggleActive(sub)}
                          disabled={processing}
                          className="rounded-md border border-border px-2 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt disabled:opacity-50"
                          aria-label={sub.isActive ? 'Deactivate' : 'Activate'}
                        >
                          {sub.isActive ? (
                            <span className="text-gray-500">Deactivate</span>
                          ) : (
                            <span className="text-green-600">Activate</span>
                          )}
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteTarget(sub)}
                          className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs font-medium text-red-600 transition hover:bg-surface-alt"
                          aria-label="Delete subscription"
                        >
                          <Trash2 size={12} />
                          <span className="text-red-600">Delete</span>
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
          <h3 className="text-base font-semibold text-ink">Trip history</h3>
          <div className="mt-3 space-y-2 text-sm">
            <p className="text-muted">No trips yet. Plan a route to populate history.</p>
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
              <Trash2 size={18} className="text-red-500" />
              <h2 className="text-base font-semibold text-ink">Delete subscription</h2>
            </div>
            <p className="mb-5 mt-2 text-sm text-muted">
              Are you sure you want to permanently delete the subscription for <span className="font-medium text-ink">{deleteTarget.lineName || `Line ${deleteTarget.lineId}`}</span>? This action cannot be undone.
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
                    <Loader2 size={14} className="animate-spin" />
                    Deleting...
                  </span>
                ) : 'Delete'}
              </button>
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                disabled={processing}
                className="flex-1 rounded-panel border border-border py-2 text-sm font-medium text-ink hover:bg-surface-alt disabled:opacity-50"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      <PanelCard tone="default">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-semibold text-ink">Wallet</h3>
          <Link to="/tickets" className="text-xs text-accent underline-offset-2 hover:underline">
            View all tickets →
          </Link>
        </div>

        {ticketsLoading ? (
          <p className="mt-3 text-sm text-muted">Loading…</p>
        ) : recentTickets.length === 0 ? (
          <p className="mt-3 text-sm text-muted">
            No tickets yet.{' '}
            <Link to="/tickets" className="text-accent underline-offset-2 hover:underline">
              Buy your first ticket
            </Link>
          </p>
        ) : (
          <div className="mt-3 space-y-2">
            {recentTickets.map((t) => (
              <div key={t.id} className="flex items-center justify-between rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                <div>
                  <span className="font-medium text-ink capitalize">{t.type?.toLowerCase()}</span>
                  {t.validUntil && (
                    <span className="ml-2 text-xs text-muted">valid until {formatDate(t.validUntil)}</span>
                  )}
                </div>
                <span className={`rounded px-2 py-0.5 text-xs font-semibold ${TICKET_STATUS_STYLES[t.status] || ''}`}>
                  {t.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </PanelCard>
    </div>
  )
}
