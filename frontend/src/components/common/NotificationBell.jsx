import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell, BellDot, CheckCheck, ExternalLink, Loader2 } from 'lucide-react'
import { useAppContext } from '../../context/AppContext'

const TYPE_COLORS = {
  DELAY: 'bg-yellow-500',
  ROUTE_CHANGE: 'bg-orange-500',
  DISRUPTION: 'bg-red-500',
  UPCOMING_DEPARTURE: 'bg-green-500',
  GENERAL: 'bg-blue-500',
  TIMETABLE_CHANGE: 'bg-purple-500',
}

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}d ago`
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

export function NotificationBell() {
  const {
    isAuthenticated, unreadCount, recentNotifications,
    fetchRecentNotifications, markAsRead, markAllAsRead, requestNotificationPermission,
  } = useAppContext()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [notifEnabled, setNotifEnabled] = useState(
    () => 'Notification' in window && Notification.permission === 'granted',
  )
  const ref = useRef(null)

  const handleToggle = useCallback(async () => {
    if (!open) {
      setLoading(true)
      await fetchRecentNotifications()
      setLoading(false)
    }
    setOpen((v) => !v)
  }, [open, fetchRecentNotifications])

  useEffect(() => {
    function handleClick(e) {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  async function toggleNotifications() {
    const result = await requestNotificationPermission()
    setNotifEnabled(result === 'granted')
  }

  if (!isAuthenticated) return null

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={handleToggle}
        className="relative rounded-panel border border-border p-2 text-muted transition hover:bg-surface-alt hover:text-ink"
        aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
      >
        {unreadCount > 0 ? <BellDot size={17} /> : <Bell size={17} />}
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold leading-none text-white">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-2 w-80 rounded-panel border border-border bg-surface shadow-xl">
          <div className="flex items-center justify-between border-b border-border px-4 py-3">
            <h3 className="text-sm font-semibold text-ink">Notifications</h3>
            <div className="flex items-center gap-2">
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={markAllAsRead}
                  className="inline-flex items-center gap-1 text-xs text-muted hover:text-ink"
                >
                  <CheckCheck size={13} />
                  Mark all read
                </button>
              )}
            </div>
          </div>

          <div className="max-h-80 overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center py-8 text-muted">
                <Loader2 size={16} className="animate-spin" />
              </div>
            ) : recentNotifications.length === 0 ? (
              <p className="py-8 text-center text-sm text-muted">No new notifications.</p>
            ) : (
              <div className="divide-y divide-border">
                {recentNotifications.slice(0, 8).map((n) => (
                  <button
                    key={n.id}
                    type="button"
                    onClick={() => { markAsRead(n.id); setOpen(false); navigate(`/notifications`) }}
                    className="flex w-full gap-3 px-4 py-3 text-left transition hover:bg-surface-alt"
                  >
                    <span className={`mt-1 h-2 w-2 flex-shrink-0 rounded-full ${TYPE_COLORS[n.type] || 'bg-gray-400'}`} />
                    <div className="min-w-0 flex-1">
                      <p className={`truncate text-sm ${n.isRead ? 'text-muted' : 'font-semibold text-ink'}`}>
                        {n.title}
                      </p>
                      <p className="mt-0.5 line-clamp-2 text-xs text-muted">{n.content}</p>
                      <p className="mt-1 text-[11px] text-muted">{timeAgo(n.sentAt)}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="flex items-center justify-between border-t border-border px-4 py-2.5">
            <button
              type="button"
              onClick={toggleNotifications}
              className={`text-xs ${notifEnabled ? 'text-green-600' : 'text-muted'} hover:text-ink`}
            >
              {notifEnabled ? 'Desktop on' : 'Enable desktop'}
            </button>
            <button
              type="button"
              onClick={() => { setOpen(false); navigate('/notifications') }}
              className="inline-flex items-center gap-1 text-xs font-medium text-accent hover:underline"
            >
              View all
              <ExternalLink size={11} />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
