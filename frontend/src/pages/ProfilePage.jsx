import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { useAppContext } from '../context/AppContext'
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
  const { isAuthenticated, session, logout, favorites, tripHistory } = useAppContext()
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

  return (
    <div className="space-y-4">
      <PanelCard tone="soft">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-ink">{session.fullName || session.email}</h2>
            <p className="mt-1 text-sm text-muted">{session.email}</p>
          </div>
          <button
            type="button"
            onClick={logout}
            className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-ink transition hover:bg-surface-alt"
          >
            Logout
          </button>
        </div>
      </PanelCard>

      <div className="grid gap-4 lg:grid-cols-2">
        <PanelCard tone="default">
          <h3 className="text-base font-semibold text-ink">Favourites</h3>
          <div className="mt-3 space-y-3 text-sm">
            <div>
              <p className="font-medium text-muted">Lines</p>
              {favorites.lines.length === 0 ? (
                <p className="mt-1 text-muted">No favourite lines yet.</p>
              ) : (
                <div className="mt-2 flex flex-wrap gap-2">
                  {favorites.lines.map((lineId) => (
                    <LineBadge
                      key={lineId}
                      line={{ code: String(lineId), vehicleTypeName: 'line', name: `Line ${lineId}` }}
                    />
                  ))}
                </div>
              )}
            </div>

            <div>
              <p className="font-medium text-muted">Stops</p>
              {favorites.stops.length === 0 ? (
                <p className="mt-1 text-muted">No favourite stops yet.</p>
              ) : (
                <p className="mt-1 text-ink">{favorites.stops.length} saved stops</p>
              )}
            </div>
          </div>
        </PanelCard>

        <PanelCard tone="default">
          <h3 className="text-base font-semibold text-ink">Trip history</h3>
          {tripHistory.length === 0 ? (
            <p className="mt-3 text-sm text-muted">No trips yet. Plan a route to populate history.</p>
          ) : (
            <div className="mt-3 space-y-2">
              {tripHistory.slice(0, 8).map((trip) => (
                <div
                  key={trip.id}
                  className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm"
                >
                  <p className="font-semibold text-ink">
                    {trip.fromStop} → {trip.toStop}
                  </p>
                  <p className="text-xs text-muted">
                    {trip.durationMinutes} min · {new Date(trip.traveledAt).toLocaleString()}
                  </p>
                </div>
              ))}
            </div>
          )}
        </PanelCard>
      </div>

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
