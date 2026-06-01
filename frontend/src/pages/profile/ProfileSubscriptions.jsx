import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, Clock, Loader2, Pencil, RefreshCw, Route, Star, Trash2, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { LineBadge } from '../../components/common/LineBadge'
import { PanelCard } from '../../components/common/PanelCard'
import { SubscriptionModal } from '../../components/common/SubscriptionModal'
import { ReviewForm } from '../../components/reviews/ReviewForm'
import { formatDate, formatDays, formatTime } from './_shared'

export function ProfileSubscriptions({
  subscriptions,
  suggestions,
  suggestionLineMap,
  session,
  loading,
  processing,
  refresh,
  loadReviews,
  handleEditSubscription,
  handleToggleSubscriptionActive,
  handleDeleteSubscription,
}) {
  const { t } = useTranslation(['profile'])
  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [reviewTarget, setReviewTarget] = useState(null)

  const dayLabels = useMemo(
    () => ({
      MON: t('profile:day_mon'),
      TUE: t('profile:day_tue'),
      WED: t('profile:day_wed'),
      THU: t('profile:day_thu'),
      FRI: t('profile:day_fri'),
      SAT: t('profile:day_sat'),
      SUN: t('profile:day_sun'),
    }),
    [t],
  )

  function lineForSubscription(sub) {
    return { code: sub.lineCode || String(sub.lineId), vehicleTypeName: 'bus', name: sub.lineName || `Line ${sub.lineId}` }
  }

  return (
    <div className="space-y-4">
      <PanelCard tone="default">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CalendarDays size={18} className="text-accent" aria-hidden="true" />
            <h3 className="text-base font-semibold text-ink">{t('profile:subscriptions')}</h3>
          </div>
          <button
            type="button"
            onClick={refresh}
            disabled={loading}
            className="rounded-lg border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink disabled:opacity-50"
            aria-label={t('profile:refresh_subs')}
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
        <div className="mt-4 space-y-2 text-sm">
          {subscriptions.length === 0 ? (
            <p className="text-muted">{t('profile:no_subs')}</p>
          ) : (
            subscriptions.map((sub) => (
              <article
                key={sub.id}
                className={`rounded-lg border px-3 py-3 transition ${sub.isActive ? 'border-border bg-surface-soft' : 'border-border/50 bg-surface-soft/60'}`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <LineBadge line={lineForSubscription(sub)} />
                      {sub.isActive ? (
                        <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-semibold text-green-700 dark:bg-green-900/30 dark:text-green-400">
                          {t('profile:active')}
                        </span>
                      ) : (
                        <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-semibold text-gray-500 dark:bg-gray-800 dark:text-gray-400">
                          {t('profile:inactive')}
                        </span>
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
                        {formatDays(sub.daysOfWeek, dayLabels)}
                      </span>
                    </div>

                    <p className="mt-1 text-[11px] text-muted">{t('profile:created', { date: formatDate(sub.createdAt) })}</p>
                  </div>

                  <div className="flex shrink-0 flex-col gap-1">
                    <button
                      type="button"
                      onClick={() => setEditTarget(sub)}
                      className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt"
                      aria-label={t('profile:edit_sub')}
                    >
                      <Pencil size={12} aria-hidden="true" />
                      <span className="text-ink">{t('profile:edit')}</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => handleToggleSubscriptionActive(sub)}
                      disabled={processing}
                      className="rounded-md border border-border px-2 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt disabled:opacity-50"
                      aria-label={sub.isActive ? t('profile:deactivate') : t('profile:activate')}
                    >
                      {sub.isActive ? (
                        <span className="text-gray-500">{t('profile:deactivate')}</span>
                      ) : (
                        <span className="text-green-600">{t('profile:activate')}</span>
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleteTarget(sub)}
                      className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs font-medium text-red-600 transition hover:bg-surface-alt"
                      aria-label={t('profile:delete_sub')}
                    >
                      <Trash2 size={12} aria-hidden="true" />
                      <span className="text-red-600">{t('profile:delete')}</span>
                    </button>
                  </div>
                </div>
              </article>
            ))
          )}
        </div>
      </PanelCard>

      <div className="grid gap-4 xl:grid-cols-2">
        <PanelCard tone="default">
          <div className="flex items-center gap-2">
            <Star size={16} className="text-amber-400" fill="currentColor" aria-hidden="true" />
            <h3 className="text-base font-semibold text-ink">{t('profile:rate_title')}</h3>
          </div>
          <p className="mt-1 text-sm text-muted">{t('profile:rate_subtitle')}</p>

          <div className="mt-3">
            {subscriptions.length === 0 ? (
              <p className="text-sm text-muted">
                {t('profile:rate_no_subs')}{' '}
                <Link to="/lines" className="text-accent underline-offset-2 hover:underline">
                  {t('profile:rate_browse_lines')}
                </Link>
              </p>
            ) : (
              <div className="space-y-2">
                {subscriptions.map((sub) => (
                  <div key={sub.id} className="flex items-center justify-between rounded-lg border border-border bg-surface-soft px-3 py-2">
                    <div className="flex min-w-0 items-center gap-2">
                      <LineBadge line={lineForSubscription(sub)} />
                      <span className="truncate text-sm text-ink">{sub.lineName || `Line ${sub.lineId}`}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setReviewTarget(sub)}
                      className="ml-2 inline-flex shrink-0 items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt"
                    >
                      <Star size={11} className="text-amber-400" fill="currentColor" aria-hidden="true" />
                      {t('profile:rate_write_review')}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </PanelCard>

        <PanelCard tone="default">
          <div className="flex items-center gap-2">
            <Route size={18} className="text-accent" aria-hidden="true" />
            <h3 className="text-base font-semibold text-ink">{t('profile:personalized_suggestions')}</h3>
          </div>
          <p className="mt-1 text-sm text-muted">{t('profile:personalized_suggestions_hint')}</p>
          <div className="mt-4 space-y-2">
            {suggestions.length === 0 ? (
              <p className="text-sm text-muted">{t('profile:no_suggestions')}</p>
            ) : (
              suggestions.map((code) => {
                const line = suggestionLineMap.get(String(code).trim().toLowerCase())
                return (
                  <div key={code} className="flex items-center justify-between gap-3 rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                    <div className="min-w-0 flex-1">
                      {line ? <LineBadge line={line} /> : <p className="font-medium text-ink">{code}</p>}
                      <p className="mt-1 text-xs text-muted">{t('profile:suggestion_reason')}</p>
                    </div>
                    <Link to="/lines" className="shrink-0 text-xs font-medium text-accent underline-offset-2 hover:underline">
                      {t('profile:browse_lines')}
                    </Link>
                  </div>
                )
              })
            )}
          </div>
        </PanelCard>
      </div>

      <SubscriptionModal
        isOpen={editTarget !== null}
        onClose={() => setEditTarget(null)}
        onSubmit={(data) => handleEditSubscription(editTarget.id, data)}
        lineName={editTarget?.lineName}
        mode="edit"
        initialValues={
          editTarget
            ? { startInterval: editTarget.startInterval, endInterval: editTarget.endInterval, daysOfWeek: editTarget.daysOfWeek }
            : null
        }
      />

      {deleteTarget !== null && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          onClick={() => !processing && setDeleteTarget(null)}
          role="dialog"
          aria-modal="true"
        >
          <div
            className="w-full max-w-sm rounded-panel border border-border bg-surface p-6 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex items-center gap-2">
              <Trash2 size={18} className="text-red-500" aria-hidden="true" />
              <h2 className="text-base font-semibold text-ink">{t('profile:delete_sub')}</h2>
            </div>
            <p className="mb-5 mt-2 text-sm text-muted">
              {t('profile:delete_confirm', { line: deleteTarget.lineName || `Line ${deleteTarget.lineId}` })}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={async () => {
                  await handleDeleteSubscription(deleteTarget.id)
                  setDeleteTarget(null)
                }}
                disabled={processing}
                className="flex-1 rounded-panel border border-red-500 bg-red-500 py-2 text-sm font-medium text-white disabled:opacity-50"
              >
                {processing ? (
                  <span className="inline-flex items-center gap-2">
                    <Loader2 size={14} className="animate-spin" aria-hidden="true" />
                    {t('profile:deleting')}
                  </span>
                ) : (
                  t('profile:delete')
                )}
              </button>
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                disabled={processing}
                className="flex-1 rounded-panel border border-border py-2 text-sm font-medium text-ink hover:bg-surface-alt disabled:opacity-50"
              >
                {t('profile:cancel')}
              </button>
            </div>
          </div>
        </div>
      )}

      {reviewTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
          onClick={() => setReviewTarget(null)}
          role="dialog"
          aria-modal="true"
        >
          <div
            className="w-full max-w-md rounded-panel border border-border bg-surface p-6 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-base font-semibold text-ink">
                {t('profile:rate_modal_title', { line: reviewTarget.lineName || `Line ${reviewTarget.lineId}` })}
              </h2>
              <button
                type="button"
                onClick={() => setReviewTarget(null)}
                className="text-muted transition hover:text-ink"
                aria-label={t('profile:rate_modal_close')}
              >
                <X size={16} aria-hidden="true" />
              </button>
            </div>
            <ReviewForm
              lineId={reviewTarget.lineId}
              reviewerUserId={session.userId}
              onSuccess={() => {
                setReviewTarget(null)
                loadReviews(session.userId)
              }}
            />
          </div>
        </div>
      )}
    </div>
  )
}
