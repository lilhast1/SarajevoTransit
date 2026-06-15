import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import {
  Bell, BellDot, CheckCheck, ChevronLeft, ChevronRight,
  Loader2, AlertCircle, RefreshCw, ArrowLeft, ExternalLink,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../components/common/PanelCard'
import { ErrorAlert } from '../components/common/Alerts'
import { EmptyState } from '../components/common/LoadingStates'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

const TYPE_COLORS = {
  DELAY: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300',
  ROUTE_CHANGE: 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-300',
  DISRUPTION: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
  UPCOMING_DEPARTURE: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  GENERAL: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  TIMETABLE_CHANGE: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300',
  REPORT_STATUS_CHANGE: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
}

function timeAgo(dateStr, t) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return t('just_now')
  if (mins < 60) return t('minutes_ago', { count: mins })
  const hours = Math.floor(mins / 60)
  if (hours < 24) return t('hours_ago', { count: hours })
  const days = Math.floor(hours / 24)
  if (days < 30) return t('days_ago', { count: days })
  return new Date(dateStr).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

export function NotificationsPage() {
  const { isAuthenticated, session, unreadCount, markAllAsRead, markAsRead } = useAppContext()
  const { t } = useTranslation('notifications')
  const navigate = useNavigate()

  const [notifications, setNotifications] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchPage = useCallback(async (p) => {
    if (!session?.userId) return
    setLoading(true)
    setError(null)
    try {
      const result = await transitApi.getUserNotifications(session.userId, p)
      setNotifications(result.content || result || [])
      setTotalPages(result.totalPages ?? 1)
      setTotalElements(result.totalElements ?? 0)
      setPage(result.page ?? p)
    } catch (err) {
      setError(err.message || 'Failed to load notifications')
    } finally {
      setLoading(false)
    }
  }, [session?.userId])

  useEffect(() => {
    fetchPage(0)
  }, [fetchPage])

  if (!isAuthenticated) return <Navigate to="/auth" replace />

  const TYPE_LABEL_KEYS = {
    DELAY: 'type_delay', ROUTE_CHANGE: 'type_route_change', DISRUPTION: 'type_disruption',
    UPCOMING_DEPARTURE: 'type_departure', GENERAL: 'type_general', TIMETABLE_CHANGE: 'type_timetable',
    REPORT_STATUS_CHANGE: 'type_report_status',
  }
  const typeMeta = (type) => ({
    label: TYPE_LABEL_KEYS[type] ? t(TYPE_LABEL_KEYS[type]) : type,
    color: TYPE_COLORS[type] || 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
  })

  return (
    <div className="space-y-4">
      <PanelCard tone="soft">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="rounded-lg border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
              aria-label={t('go_back')}
            >
              <ArrowLeft size={16} aria-hidden="true" />
            </button>
            <div>
              <h2 className="text-xl font-semibold text-ink">{t('bell_title')}</h2>
              <p className="mt-1 text-sm text-muted">
                {totalElements === 1 ? t('count_one') : t('count_other', { count: totalElements })}
                {unreadCount > 0 && ` (${t('unread_count', { count: unreadCount })})`}
              </p>
            </div>
          </div>
          {unreadCount > 0 && (
            <button
              type="button"
              onClick={markAllAsRead}
              className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-2 text-sm font-medium text-white transition hover:bg-accent/90"
            >
              <CheckCheck size={14} aria-hidden="true" />
              {t('mark_all')}
            </button>
          )}
        </div>
      </PanelCard>

      {error && <ErrorAlert error={error} onDismiss={() => setError(null)} />}

      <PanelCard tone="default">
        {loading && notifications.length === 0 ? (
          <div className="flex items-center justify-center py-12 text-muted">
            <Loader2 size={20} className="animate-spin" />
          </div>
        ) : error ? (
          <EmptyState
            icon={AlertCircle}
            title={t('failed_to_load')}
            description={error}
            action={
              <button
                type="button"
                onClick={() => fetchPage(page)}
                className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-2 text-sm font-medium text-white"
              >
                <RefreshCw size={14} aria-hidden="true" />
                {t('retry')}
              </button>
            }
          />
        ) : notifications.length === 0 ? (
          <EmptyState
            icon={Bell}
            title={t('no_notifications')}
            description={t('no_notifications_desc')}
          />
        ) : (
          <div className="divide-y divide-border">
            {notifications.map((n) => (
              <button
                key={n.id}
                type="button"
                onClick={() => {
                  if (!n.isRead) markAsRead(n.id)
                  if (n.type === 'REPORT_STATUS_CHANGE') navigate('/my-reports')
                }}
                className={`flex w-full gap-4 px-4 py-4 text-left transition hover:bg-surface-soft ${
                  !n.isRead ? 'bg-surface-alt/50' : ''
                }`}
              >
                <div className="flex flex-col items-center gap-2 pt-0.5">
                  {n.isRead ? (
                    <Bell size={16} className="text-muted" />
                  ) : (
                    <BellDot size={16} className="text-accent" />
                  )}
                  <span className={`h-2 w-2 rounded-full ${!n.isRead ? 'bg-accent' : 'bg-transparent'}`} />
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${typeMeta(n.type).color}`}>
                      {typeMeta(n.type).label}
                    </span>
                    {n.lineName && (
                      <span className="text-[11px] text-muted">{n.lineName}</span>
                    )}
                    <span className="ml-auto text-[11px] text-muted">{timeAgo(n.sentAt, t)}</span>
                  </div>

                  <p className={`mt-1.5 ${n.isRead ? 'text-ink' : 'font-semibold text-ink'}`}>
                    {n.title}
                  </p>

                  {n.content && (
                    <p className="mt-1 line-clamp-3 text-sm text-muted">{n.content}</p>
                  )}

                  {n.type === 'REPORT_STATUS_CHANGE' && (
                    <span className="mt-1.5 inline-flex items-center gap-1 text-xs font-medium text-accent">
                      {t('view_my_reports')}
                      <ExternalLink size={11} aria-hidden="true" />
                    </span>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}

        {!loading && totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-border px-4 py-3">
            <button
              type="button"
              onClick={() => fetchPage(page - 1)}
              disabled={page <= 0}
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-sm text-ink hover:bg-surface-alt disabled:opacity-40"
            >
              <ChevronLeft size={14} aria-hidden="true" />
              {t('previous')}
            </button>
            <span className="text-xs text-muted">
              {t('page_of', { page: page + 1, total: totalPages })}
            </span>
            <button
              type="button"
              onClick={() => fetchPage(page + 1)}
              disabled={page >= totalPages - 1}
              className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-1.5 text-sm text-ink hover:bg-surface-alt disabled:opacity-40"
            >
              {t('next')}
              <ChevronRight size={14} aria-hidden="true" />
            </button>
          </div>
        )}
      </PanelCard>
    </div>
  )
}
