import { Navigate } from 'react-router-dom'
import { LineBadge } from '../components/common/LineBadge'
import { PanelCard } from '../components/common/PanelCard'
import { useAppContext } from '../context/AppContext'

export function ProfilePage() {
  const { isAuthenticated, session, logout, favorites, tripHistory } = useAppContext()

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
    </div>
  )
}
