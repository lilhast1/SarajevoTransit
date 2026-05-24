import { Heart, AlertCircle, Loader2, LogIn } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LineDetailLayout } from '../components/lines/LineDetailLayout'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { EmptyState } from '../components/common/LoadingStates'
import { SubscriptionModal } from '../components/common/SubscriptionModal'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'

export function LineDetailPage() {
  const { t } = useTranslation('lines')
  const { id } = useParams()
  const navigate = useNavigate()
  const lineId = Number(id)
  const { isAuthenticated, isLineSubscribed, subscribeToLine, unsubscribeFromLine } = useAppContext()

  const [line, setLine] = useState(null)
  const [directions, setDirections] = useState([])
  const [selectedDirectionId, setSelectedDirectionId] = useState(null)
  const [stops, setStops] = useState([])
  const [polyline, setPolyline] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [subscriptionModalOpen, setSubscriptionModalOpen] = useState(false)
  const [loginPromptOpen, setLoginPromptOpen] = useState(false)
  const [subscribing, setSubscribing] = useState(false)
  const [subscriptionMsg, setSubscriptionMsg] = useState(null)
  const { session } = useAppContext()

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)

    const loadLineData = async () => {
      try {
        const [lineResponse, directionResponse] = await Promise.all([
          transitApi.getLineById(lineId),
          transitApi.getDirectionsByLine(lineId),
        ])

        if (!active) return

        setLine(lineResponse)
        setDirections(directionResponse)

        const selected = directionResponse[0]?.id || null
        setSelectedDirectionId(selected)

        if (selected) {
          const [directionStops, directionPolyline] = await Promise.all([
            transitApi.getDirectionStations(selected),
            transitApi.getDirectionPolyline(selected),
          ])
          if (active) {
            setStops(directionStops)
            setPolyline(directionPolyline)
          }
        }
      } catch (err) {
        if (active) {
          setError(err.message || 'Failed to load line details')
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    loadLineData()

    return () => {
      active = false
    }
  }, [lineId])

  useEffect(() => {
    if (!selectedDirectionId) return
    let active = true

    const loadDirectionData = async () => {
      try {
        const [directionStops, directionPolyline] = await Promise.all([
          transitApi.getDirectionStations(selectedDirectionId),
          transitApi.getDirectionPolyline(selectedDirectionId),
        ])
        if (active) {
          setStops(directionStops)
          setPolyline(directionPolyline)
        }
      } catch (err) {
        console.error('Failed to load direction data:', err)
      }
    }

    loadDirectionData()

    return () => {
      active = false
    }
  }, [selectedDirectionId])

  const subscribed = useMemo(() => isLineSubscribed(lineId), [isLineSubscribed, lineId])

  const handleStopClick = (stopId) => {
    navigate(`/stops/${stopId}`)
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
        await unsubscribeFromLine(lineId)
        setSubscriptionMsg({ type: 'success', text: t('unsubscribed_success') })
      } catch (err) {
        setSubscriptionMsg({ type: 'error', text: err.message || t('unsubscribed_failed') })
      } finally {
        setSubscribing(false)
      }
    } else {
      setSubscriptionModalOpen(true)
    }
  }

  async function handleSubscriptionSubmit(data) {
    if (!line || !session?.userId) return
    await subscribeToLine(
      session.userId,
      lineId,
      line.code,
      line.name,
      data.startInterval,
      data.endInterval,
      data.daysOfWeek,
    )
  }

  if (error) {
    return (
      <div className="space-y-4">
        <ErrorAlert
          error={error}
          onDismiss={() => setError(null)}
        />
        <Link to="/lines" className="inline-flex items-center gap-2 text-sm font-medium text-accent hover:underline">
          ← {t('back_to_lines')}
        </Link>
      </div>
    )
  }

  if (!loading && !line) {
    return (
      <EmptyState
        icon={AlertCircle}
        title={t('line_not_found')}
        description={t('line_not_found_msg')}
        action={
          <Link
            to="/lines"
            className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-2 text-sm font-medium text-white transition hover:bg-accent/90"
          >
            {t('back_to_lines')}
          </Link>
        }
      />
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

      <LineDetailLayout
        line={line}
        directions={directions}
        selectedDirectionId={selectedDirectionId}
        onSelectDirection={setSelectedDirectionId}
        stops={stops}
        polyline={polyline}
        detailLoading={loading}
        onBack={() => navigate('/lines')}
        backLabel={t('back_to_lines')}
        onStopClick={handleStopClick}
        subtitle={t('direction_hint')}
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
              <Loader2 size={16} className="animate-spin" aria-hidden="true" />
            ) : (
              <Heart size={16} fill={subscribed ? 'currentColor' : 'none'} aria-hidden="true" />
            )}
            {subscribing ? t('unsubscribing') : subscribed ? t('subscribed') : t('subscribe')}
          </button>
        )}
      />

      <SubscriptionModal
        isOpen={subscriptionModalOpen}
        onClose={() => setSubscriptionModalOpen(false)}
        onSubmit={handleSubscriptionSubmit}
        lineName={line?.name}
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
              <LogIn size={18} className="text-accent" aria-hidden="true" />
              <h2 className="text-base font-semibold text-ink">{t('login_required')}</h2>
            </div>
            <p className="mb-5 mt-2 text-sm text-muted">
              {t('login_required_msg')}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => navigate('/auth')}
                className="flex-1 rounded-panel border border-accent bg-accent py-2 text-sm font-medium text-white"
              >
                {t('go_to_login')}
              </button>
              <button
                type="button"
                onClick={() => setLoginPromptOpen(false)}
                className="flex-1 rounded-panel border border-border py-2 text-sm font-medium text-ink hover:bg-surface-alt"
              >
                {t('cancel')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
