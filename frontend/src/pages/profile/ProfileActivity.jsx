import { History, Star } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../../components/common/PanelCard'
import { formatDate, formatDateTime, getLineLabel, TICKET_STATUS_STYLES } from './_shared'

export function ProfileActivity({ travelHistory, tripHistory, tickets, reviews, availableLines }) {
  const { t } = useTranslation('profile')

  return (
    <div className="space-y-4">
      <PanelCard tone="default">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <History size={18} className="text-accent" aria-hidden="true" />
            <h3 className="text-base font-semibold text-ink">{t('usage_history')}</h3>
          </div>
          <span className="hidden text-xs text-muted sm:inline">{t('usage_history_hint')}</span>
        </div>

        <div className="mt-4 grid gap-4 xl:grid-cols-2">
          <section className="space-y-3">
            <h4 className="text-sm font-semibold text-ink">{t('travel_history')}</h4>
            {travelHistory.length === 0 ? (
              <p className="text-sm text-muted">{t('no_travel_history')}</p>
            ) : (
              <div className="space-y-2">
                {travelHistory.map((item) => (
                  <article key={item.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-full bg-accent/10 px-2 py-0.5 text-xs font-semibold text-accent">{item.lineCode}</span>
                      <span className="text-xs text-muted">{formatDateTime(item.traveledAt)}</span>
                    </div>
                    <p className="mt-1 text-ink">{item.fromStop} → {item.toStop}</p>
                    <p className="text-xs text-muted">{t('travel_duration', { minutes: item.durationMinutes })}</p>
                  </article>
                ))}
              </div>
            )}

            <h4 className="pt-2 text-sm font-semibold text-ink">{t('planner_history')}</h4>
            {tripHistory.length === 0 ? (
              <p className="text-sm text-muted">{t('no_planner_history')}</p>
            ) : (
              <div className="space-y-2">
                {tripHistory.map((item) => (
                  <article key={item.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                    <p className="font-medium text-ink">{item.fromStop} → {item.toStop}</p>
                    <p className="text-xs text-muted">{item.lineCode} · {item.durationMinutes} min · {formatDateTime(item.traveledAt)}</p>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="space-y-3">
            <h4 className="text-sm font-semibold text-ink">{t('ticket_history')}</h4>
            {tickets.length === 0 ? (
              <p className="text-sm text-muted">{t('no_tickets')}</p>
            ) : (
              <div className="space-y-2">
                {tickets.map((ticket) => (
                  <article key={ticket.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="font-medium text-ink capitalize">{String(ticket.type || '').toLowerCase()}</p>
                        <p className="text-xs text-muted">
                          {formatDate(ticket.purchaseDate)}
                          {ticket.validUntil ? ` · ${t('valid_until', { date: formatDate(ticket.validUntil) })}` : ''}
                        </p>
                        {ticket.amount != null ? <p className="text-xs text-muted">{ticket.amount} KM</p> : null}
                      </div>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${TICKET_STATUS_STYLES[ticket.status] || 'bg-surface-alt text-muted'}`}>
                        {ticket.status}
                      </span>
                    </div>
                  </article>
                ))}
              </div>
            )}

            <h4 className="pt-2 text-sm font-semibold text-ink">{t('review_history')}</h4>
            {reviews.length === 0 ? (
              <p className="text-sm text-muted">{t('no_reviews')}</p>
            ) : (
              <div className="space-y-2">
                {reviews.map((review) => {
                  const reviewLine = availableLines.find((line) => String(line.id) === String(review.lineId))
                  return (
                    <article key={review.id} className="rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm">
                      <div className="flex items-center justify-between gap-2">
                        <p className="font-medium text-ink">{getLineLabel(reviewLine, `Line ${review.lineId}`)}</p>
                        <span className="inline-flex items-center gap-1 text-xs font-semibold text-amber-500">
                          <Star size={12} fill="currentColor" aria-hidden="true" />
                          {review.rating}
                        </span>
                      </div>
                      {review.reviewText ? <p className="mt-1 text-sm text-muted">{review.reviewText}</p> : null}
                      <p className="mt-1 text-xs text-muted">{formatDateTime(review.createdAt)} · {review.moderationStatus}</p>
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        </div>
      </PanelCard>
    </div>
  )
}
