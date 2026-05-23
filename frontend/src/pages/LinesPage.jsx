import { AlertCircle, RefreshCw, Heart, Route, Loader2, LogIn } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LineBadge } from '../components/common/LineBadge'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { EmptyState } from '../components/common/LoadingStates'
import { PanelCard } from '../components/common/PanelCard'
import { LineDetailLayout } from '../components/lines/LineDetailLayout'
import { SubscriptionModal } from '../components/common/SubscriptionModal'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

const typeFilters = ['all', 'tram', 'bus', 'trolleybus', 'minibus']

export function LinesPage() {
  const navigate = useNavigate()
  const { isAuthenticated, isLineSubscribed, subscribeToLine, unsubscribeFromLine } = useAppContext()

  const [lines, setLines] = useState([])
  const [query, setQuery] = useState('')
  const [type, setType] = useState('all')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [linesRetryKey, setLinesRetryKey] = useState(0)

  const [selectedLineId, setSelectedLineId] = useState(null)
  const [selectedLine, setSelectedLine] = useState(null)
  const [directions, setDirections] = useState([])
  const [selectedDirectionId, setSelectedDirectionId] = useState(null)
  const [stops, setStops] = useState([])
  const [polyline, setPolyline] = useState([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState(null)
  const [directionError, setDirectionError] = useState(null)

  const [subscriptionModalOpen, setSubscriptionModalOpen] = useState(false)
  const [loginPromptOpen, setLoginPromptOpen] = useState(false)
  const [subscribing, setSubscribing] = useState(false)
  const [subscriptionMsg, setSubscriptionMsg] = useState(null)
  const { session } = useAppContext()

  const detailMode = selectedLineId !== null

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)

    const fetchLines = async () => {
      try {
        const response = await transitApi.getLines({
          search: query,
          vehicleType: type === 'all' ? '' : type,
          activeOnly: true,
        })
        if (active) {
          setLines(response)
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Failed to load lines')
          setLines([])
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    fetchLines()

    return () => {
      active = false
    }
  }, [query, type, linesRetryKey])

  useEffect(() => {
    if (!selectedLineId) return
    let active = true
    setDetailLoading(true)
    setDetailError(null)
    setDirectionError(null)

    const fetchLineDetail = async () => {
      try {
        const [lineResponse, directionResponse] = await Promise.all([
          transitApi.getLineById(selectedLineId),
          transitApi.getDirectionsByLine(selectedLineId),
        ])

        if (!active) return
        setSelectedLine(lineResponse)
        setDirections(directionResponse)

        if (directionResponse.length > 0) {
          let foundDirectionId = null

          for (const direction of directionResponse) {
            const dPolyline = await transitApi.getDirectionPolyline(direction.id)
            if (!active) return

            if (dPolyline && dPolyline.length > 1) {
              foundDirectionId = direction.id
              break
            }
          }

          if (!foundDirectionId) {
            foundDirectionId = directionResponse[0].id
          }

          setSelectedDirectionId(foundDirectionId)
        } else {
          setSelectedDirectionId(null)
          setStops([])
          setPolyline([])
        }
      } catch (err) {
        if (active) {
          setDetailError(err.message || 'Failed to load line details')
          setSelectedLine(null)
          setDirections([])
          setSelectedDirectionId(null)
          setStops([])
          setPolyline([])
        }
      } finally {
        if (active) {
          setDetailLoading(false)
        }
      }
    }

    fetchLineDetail()

    return () => {
      active = false
    }
  }, [selectedLineId])

  useEffect(() => {
    if (!selectedDirectionId) return
    let active = true
    setDirectionError(null)

    const fetchDirectionData = async () => {
      try {
        const [directionStops, directionPolyline] = await Promise.all([
          transitApi.getDirectionStations(selectedDirectionId),
          transitApi.getDirectionPolyline(selectedDirectionId),
        ])
        if (!active) return
        setStops(directionStops)
        setPolyline(directionPolyline)
      } catch (err) {
        if (active) {
          setDirectionError(err.message || 'Failed to load direction data')
          setStops([])
          setPolyline([])
        }
      }
    }

    fetchDirectionData()

    return () => {
      active = false
    }
  }, [selectedDirectionId])

  const countLabel = useMemo(() => `${lines.length} line${lines.length === 1 ? '' : 's'}`, [lines])

  const subscribed = useMemo(
    () => selectedLineId != null && isLineSubscribed(selectedLineId),
    [isLineSubscribed, selectedLineId],
  )

  const openLineDetail = (lineId) => {
    setSelectedLineId(lineId)
    setSelectedLine(null)
    setDirections([])
    setSelectedDirectionId(null)
    setStops([])
    setPolyline([])
  }

  const backToSearch = () => {
    setSelectedLineId(null)
    setSelectedLine(null)
    setDirections([])
    setSelectedDirectionId(null)
    setStops([])
    setPolyline([])
  }

  const handleRetry = () => {
    setError(null)
    setLinesRetryKey((current) => current + 1)
  }

  async function handleHeartClick() {
    setSubscriptionMsg(null)
    if (!isAuthenticated) {
      setLoginPromptOpen(true)
      return
    }
    if (subscribed) {
      setSubscribing(true)
      try {
        await unsubscribeFromLine(selectedLineId)
        setSubscriptionMsg({ type: 'success', text: 'Unsubscribed from notifications for this line.' })
      } catch (err) {
        setSubscriptionMsg({ type: 'error', text: err.message || 'Failed to unsubscribe.' })
      } finally {
        setSubscribing(false)
      }
    } else {
      setSubscriptionModalOpen(true)
    }
  }

  async function handleSubscriptionSubmit(data) {
    if (!selectedLine || !session?.userId) return
    await subscribeToLine(
      session.userId,
      selectedLineId,
      selectedLine.code,
      selectedLine.name,
      data.startInterval,
      data.endInterval,
      data.daysOfWeek,
    )
  }

  return (
    <div className="space-y-4">
      {subscriptionMsg && subscriptionMsg.type === 'success' && (
        <SuccessAlert message={subscriptionMsg.text} onDismiss={() => setSubscriptionMsg(null)} />
      )}
      {subscriptionMsg && subscriptionMsg.type === 'error' && (
        <ErrorAlert error={subscriptionMsg.text} onDismiss={() => setSubscriptionMsg(null)} />
      )}

      {!detailMode ? (
        <PanelCard tone="soft">
          <h2 className="text-xl font-semibold text-ink">Line Search</h2>
          <p className="mt-1 text-sm text-muted">Browse all active public transport lines in Sarajevo.</p>

          <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
            <label htmlFor="line-search" className="sr-only">
              Search lines
            </label>
            <input
              id="line-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by line number or name"
              className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
            />
            <label htmlFor="line-type-filter" className="sr-only">
              Filter line type
            </label>
            <select
              id="line-type-filter"
              value={type}
              onChange={(event) => setType(event.target.value)}
              className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
            >
              {typeFilters.map((option) => (
                <option key={option} value={option}>
                  {option === 'all' ? 'All types' : option}
                </option>
              ))}
            </select>
          </div>

          <p className="mt-3 text-xs font-medium uppercase tracking-[0.12em] text-muted">{countLabel}</p>
        </PanelCard>
      ) : null}

      {!detailMode && error && (
        <ErrorAlert
          error={error}
          onDismiss={() => setError(null)}
        />
      )}

      {detailMode && detailError && (
        <ErrorAlert
          error={detailError}
          onDismiss={() => setDetailError(null)}
        />
      )}

      {detailMode && directionError && (
        <ErrorAlert
          error={directionError}
          onDismiss={() => setDirectionError(null)}
        />
      )}

      {!detailMode ? (
        <div className="flex flex-col gap-4 sm:flex-row">
          <PanelCard className="min-h-[520px] sm:w-1/3 sm:shrink-0">
            <h3 className="mb-3 text-base font-semibold text-ink">Matching lines</h3>
            {loading ? <p className="text-sm text-muted">Loading lines...</p> : null}
            {!loading && error ? (
              <EmptyState
                icon={AlertCircle}
                title="Failed to load lines"
                description={error}
                action={
                  <button
                    type="button"
                    onClick={handleRetry}
                    className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-2 text-sm font-medium text-white transition hover:bg-accent/90"
                  >
                    <RefreshCw size={14} />
                    Try again
                  </button>
                }
              />
            ) : null}
            {!loading && !error && lines.length === 0 ? <p className="text-sm text-muted">No lines match your search.</p> : null}

            <div className="grid gap-2">
              {!error && lines.map((line) => (
                <button
                  key={line.id}
                  type="button"
                  onClick={() => openLineDetail(line.id)}
                  className="rounded-panel border border-border bg-surface-soft px-3 py-3 text-left transition hover:bg-surface-alt"
                >
                  <LineBadge line={line} />
                  <p className="mt-2 text-sm font-semibold text-ink">{line.name}</p>
                </button>
              ))}
            </div>
          </PanelCard>

          <PanelCard className="min-h-[520px] flex-1">
            <h3 className="mb-3 text-base font-semibold text-ink">Route map</h3>
            <div className="flex h-[460px] items-center justify-center rounded-lg border border-dashed border-border bg-surface-soft px-4 text-center">
              <div>
                <Route className="mx-auto mb-2 text-muted" size={22} />
                <p className="text-sm text-muted">Select a line to preview its route map and stations.</p>
              </div>
            </div>
          </PanelCard>
        </div>
      ) : (
        <LineDetailLayout
          line={selectedLine}
          directions={directions}
          selectedDirectionId={selectedDirectionId}
          onSelectDirection={setSelectedDirectionId}
          stops={stops}
          polyline={polyline}
          detailLoading={detailLoading}
          onBack={backToSearch}
          backLabel="Back to search"
          onStopClick={(stopId) => navigate(`/stops/${stopId}`)}
          subtitle="Direction-aware stop list and route preview."
          directionAction={(
            <button
              type="button"
              onClick={handleHeartClick}
              disabled={subscribing}
              className={`inline-flex items-center gap-2 rounded-panel border px-3 py-2 text-sm font-medium transition disabled:opacity-60 ${
                subscribed
                  ? 'border-accent bg-accent text-white'
                  : 'border-border text-ink hover:bg-surface-alt'
              }`}
            >
              {subscribing ? (
                <Loader2 size={14} className="animate-spin" />
              ) : (
                <Heart size={14} fill={subscribed ? 'currentColor' : 'none'} />
              )}
              {subscribing ? 'Unsubscribing...' : subscribed ? 'Subscribed' : 'Subscribe'}
            </button>
          )}
        />
      )}

      <SubscriptionModal
        isOpen={subscriptionModalOpen}
        onClose={() => setSubscriptionModalOpen(false)}
        onSubmit={handleSubscriptionSubmit}
        lineName={selectedLine?.name}
      />

      {loginPromptOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          onClick={() => setLoginPromptOpen(false)}
        >
          <div
            className="w-full max-w-sm rounded-panel border border-border bg-surface p-6 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-2">
              <LogIn size={18} className="text-accent" />
              <h2 className="text-base font-semibold text-ink">Login required</h2>
            </div>
            <p className="mb-5 mt-2 text-sm text-muted">
              Please log in to subscribe to line notifications and receive updates.
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => navigate('/auth')}
                className="flex-1 rounded-panel border border-accent bg-accent py-2 text-sm font-medium text-white"
              >
                Go to Login
              </button>
              <button
                type="button"
                onClick={() => setLoginPromptOpen(false)}
                className="flex-1 rounded-panel border border-border py-2 text-sm font-medium text-ink hover:bg-surface-alt"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
