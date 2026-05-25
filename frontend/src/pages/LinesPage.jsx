import { AlertCircle, RefreshCw, Heart, Route, Loader2, LogIn, Star } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { LineBadge } from '../components/common/LineBadge'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { EmptyState } from '../components/common/LoadingStates'
import { PanelCard } from '../components/common/PanelCard'
import { LineDetailLayout } from '../components/lines/LineDetailLayout'
import { SubscriptionModal } from '../components/common/SubscriptionModal'
import { useAppContext } from '../context/AppContext'
import { transitApi } from '../services/transitApi'
import { ReviewForm } from '../components/reviews/ReviewForm'
import { ReviewsList } from '../components/reviews/ReviewsList'
import { StarRating } from '../components/reviews/StarRating'

const typeFilterKeys = ['all', 'tram', 'bus', 'trolleybus', 'minibus']

export function LinesPage() {
  const navigate = useNavigate()
  const { t } = useTranslation('lines')
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

  const [reviewSummary, setReviewSummary] = useState(null)
  const [reviewsRefreshKey, setReviewsRefreshKey] = useState(0)

  useEffect(() => {
    if (!selectedLineId) return
    setReviewSummary(null)

    let active = true
    const loadSummary = async () => {
      try {
        const summary = await transitApi.getReviewSummary(selectedLineId)
        if (active) setReviewSummary(summary)
      } catch {
        if (active) setReviewSummary(null)
      }
    }

    loadSummary()

    return () => {
      active = false
    }
  }, [selectedLineId, reviewsRefreshKey])

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
        if (active) setLines(response)
      } catch (err) {
        if (active) {
          setError(err.message || t('failed'))
          setLines([])
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchLines()
    return () => { active = false }
  }, [query, type, linesRetryKey, t])

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
          if (!foundDirectionId) foundDirectionId = directionResponse[0].id
          setSelectedDirectionId(foundDirectionId)
        } else {
          setSelectedDirectionId(null)
          setStops([])
          setPolyline([])
        }
      } catch (err) {
        if (active) {
          setDetailError(err.message || t('failed'))
          setSelectedLine(null)
          setDirections([])
          setSelectedDirectionId(null)
          setStops([])
          setPolyline([])
        }
      } finally {
        if (active) setDetailLoading(false)
      }
    }

    fetchLineDetail()
    return () => { active = false }
  }, [selectedLineId, t])

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
          setDirectionError(err.message || t('failed'))
          setStops([])
          setPolyline([])
        }
      }
    }

    fetchDirectionData()
    return () => { active = false }
  }, [selectedDirectionId, t])

  const countLabel = useMemo(() => {
    return lines.length === 1 ? t('line_count_one', { count: 1 }) : t('line_count_other', { count: lines.length })
  }, [lines.length, t])

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
    if (!selectedLine || !session?.userId) return
    await subscribeToLine(
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
          <h2 className="text-xl font-semibold text-ink">{t('title')}</h2>
          <p className="mt-1 text-sm text-muted">{t('subtitle')}</p>

          <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
            <label htmlFor="line-search" className="sr-only">{t('search_aria')}</label>
            <input
              id="line-search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={t('search_placeholder')}
              className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
            />
            <label htmlFor="line-type-filter" className="sr-only">{t('filter_type_aria')}</label>
            <select
              id="line-type-filter"
              value={type}
              onChange={(event) => setType(event.target.value)}
              className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
            >
              {typeFilterKeys.map((option) => (
                <option key={option} value={option}>
                  {option === 'all' ? t('all_types') : t(`type_${option}`)}
                </option>
              ))}
            </select>
          </div>

          <p className="mt-3 text-xs font-medium uppercase tracking-[0.12em] text-muted">{countLabel}</p>
        </PanelCard>
      ) : null}

      {!detailMode && error && <ErrorAlert error={error} onDismiss={() => setError(null)} />}
      {detailMode && detailError && <ErrorAlert error={detailError} onDismiss={() => setDetailError(null)} />}
      {detailMode && directionError && <ErrorAlert error={directionError} onDismiss={() => setDirectionError(null)} />}

      {!detailMode ? (
        <div className="flex flex-col gap-4 sm:flex-row">
          <PanelCard className="min-h-[520px] sm:w-1/3 sm:shrink-0">
            <h3 className="mb-3 text-base font-semibold text-ink">{t('matching_lines')}</h3>
            {loading ? <p className="text-sm text-muted">{t('loading')}</p> : null}
            {!loading && error ? (
              <EmptyState
                icon={AlertCircle}
                title={t('failed')}
                description={error}
                action={
                  <button
                    type="button"
                    onClick={handleRetry}
                    className="inline-flex items-center gap-2 rounded-lg border border-accent bg-accent px-3 py-2 text-sm font-medium text-white transition hover:bg-accent/90"
                  >
                    <RefreshCw size={14} aria-hidden="true" />
                    {t('failed')}
                  </button>
                }
              />
            ) : null}
            {!loading && !error && lines.length === 0 ? (
              <p className="text-sm text-muted">{t('no_match')}</p>
            ) : null}

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
            <h3 className="mb-3 text-base font-semibold text-ink">{t('route_map')}</h3>
            <div className="flex h-[460px] items-center justify-center rounded-lg border border-dashed border-border bg-surface-soft px-4 text-center">
              <div>
                <Route className="mx-auto mb-2 text-muted" size={22} aria-hidden="true" />
                <p className="text-sm text-muted">{t('select_line_hint')}</p>
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
          backLabel={t('back_to_search')}
          onStopClick={(stopId) => navigate(`/stops/${stopId}`)}
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
                <Loader2 size={14} className="animate-spin" aria-hidden="true" />
              ) : (
                <Heart size={14} fill={subscribed ? 'currentColor' : 'none'} aria-hidden="true" />
              )}
              {subscribing ? t('unsubscribing') : subscribed ? t('subscribed') : t('subscribe')}
            </button>
          )}
        />
      )}

      {/* Reviews section — shown when a line is selected */}
      {detailMode && (
        <PanelCard tone="default">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-ink">{t('review_section_title')}</h3>
              {reviewSummary && reviewSummary.reviewCount > 0 ? (
                <div className="mt-1 flex items-center gap-2">
                  <StarRating value={reviewSummary.averageRating} readOnly size={16} />
                  <span className="text-sm font-medium text-ink">
                    {Number(reviewSummary.averageRating).toFixed(1)}
                  </span>
                  <span className="text-sm text-muted">
                    ({reviewSummary.reviewCount === 1
                      ? t('review_count_one', { count: reviewSummary.reviewCount })
                      : t('review_count_other', { count: reviewSummary.reviewCount })})
                  </span>
                </div>
              ) : (
                <p className="mt-1 text-sm text-muted">{t('review_no_rating_yet')}</p>
              )}
            </div>
          </div>

          <div className="mt-4">
            <ReviewsList lineId={selectedLineId} refreshKey={reviewsRefreshKey} />
          </div>

          <div className="mt-5 border-t border-border pt-4">
            {isAuthenticated && session?.userId ? (
              <>
                <h4 className="mb-3 text-sm font-semibold text-ink">{t('review_write_title')}</h4>
                <ReviewForm
                  lineId={selectedLineId}
                  reviewerUserId={session.userId}
                  onSuccess={() => setReviewsRefreshKey((k) => k + 1)}
                />
              </>
            ) : (
              <div className="flex items-center gap-3">
                <Star size={18} className="text-amber-400" fill="currentColor" aria-hidden="true" />
                <p className="text-sm text-muted">
                  {t('review_login_prompt')}{' '}
                  <button
                    type="button"
                    onClick={() => navigate('/auth')}
                    className="text-accent underline-offset-2 hover:underline"
                  >
                    {t('go_to_login')}
                  </button>
                </p>
              </div>
            )}
          </div>
        </PanelCard>
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
              <LogIn size={18} className="text-accent" aria-hidden="true" />
              <h2 className="text-base font-semibold text-ink">{t('login_required')}</h2>
            </div>
            <p className="mb-5 mt-2 text-sm text-muted">{t('login_required_msg')}</p>
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
