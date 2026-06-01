import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Coins, Ticket, Wallet } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { ErrorAlert } from '../../components/common/Alerts'
import { PanelCard } from '../../components/common/PanelCard'
import { formatDate, formatDateTime, TICKET_STATUS_STYLES } from './_shared'

const LOYALTY_REWARD_OPTIONS = [
  {
    value: 'DISCOUNT',
    label: 'Discount coupon',
    description: 'Turns your points into a discount coupon for the next ticket.',
  },
  {
    value: 'FREE_RIDE',
    label: 'Free ride coupon',
    description: 'Available only at the highest tier and tied to a specific ride.',
  },
]

export function ProfileWallet({
  recentTickets,
  recentTicketsLoading,
  recentTicketsError,
  loyaltyBalance,
  loyaltyTransactions,
  loyaltyCoupons,
  generatedCoupon,
  setGeneratedCoupon,
  profileData,
  couponForm,
  setCouponForm,
  redeemForm,
  setRedeemForm,
  handleLoyaltyCouponGenerate,
  handleLoyaltyRedeem,
}) {
  const { t } = useTranslation('profile')

  const loyaltyTier = useMemo(
    () => String(profileData?.loyaltyTier || 'BRONZE').toUpperCase(),
    [profileData?.loyaltyTier],
  )
  const freeRideEligible = Boolean(profileData?.loyaltyFreeRideEligible)
  const availableRewards = useMemo(
    () => LOYALTY_REWARD_OPTIONS.filter((option) => option.value === 'DISCOUNT' || (option.value === 'FREE_RIDE' && freeRideEligible)),
    [freeRideEligible],
  )
  const selectedReward = LOYALTY_REWARD_OPTIONS.find((option) => option.value === redeemForm.rewardType)

  return (
    <div className="space-y-4">
      <PanelCard tone="default">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Wallet size={18} className="text-accent" aria-hidden="true" />
            <h3 className="text-base font-semibold text-ink">{t('wallet')}</h3>
          </div>
          <Link to="/tickets" className="text-xs text-accent underline-offset-2 hover:underline">
            {t('view_all_tickets')}
          </Link>
        </div>

        {recentTicketsLoading ? (
          <p className="mt-3 text-sm text-muted">{t('loading')}</p>
        ) : recentTicketsError ? (
          <div className="mt-3">
            <ErrorAlert error={recentTicketsError} onDismiss={() => {}} />
          </div>
        ) : recentTickets.length === 0 ? (
          <p className="mt-3 text-sm text-muted">
            {t('no_tickets')}{' '}
            <Link to="/tickets" className="text-accent underline-offset-2 hover:underline">
              {t('buy_first_ticket')}
            </Link>
          </p>
        ) : (
          <ul className="mt-3 space-y-2">
            {recentTickets.map((ticket) => (
              <li
                key={ticket.id}
                className="flex items-center justify-between rounded-lg border border-border bg-surface-soft px-3 py-2 text-sm"
              >
                <div>
                  <span className="font-medium text-ink capitalize">{ticket.type?.toLowerCase()}</span>
                  {ticket.validUntil && (
                    <span className="ml-2 text-xs text-muted">{t('valid_until', { date: formatDate(ticket.validUntil) })}</span>
                  )}
                </div>
                <span className={`rounded px-2 py-0.5 text-xs font-semibold ${TICKET_STATUS_STYLES[ticket.status] || ''}`}>
                  {ticket.status}
                </span>
              </li>
            ))}
          </ul>
        )}
      </PanelCard>

      <PanelCard tone="default">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Coins size={18} className="text-amber-500" aria-hidden="true" />
            <div>
              <h3 className="text-base font-semibold text-ink">Loyalty programme</h3>
              <p className="text-sm text-muted">
                Tier: <span className="font-semibold text-ink">{loyaltyTier.charAt(0) + loyaltyTier.slice(1).toLowerCase()}</span> · Points are earned from ticket purchases and validated rides.
              </p>
            </div>
          </div>
          <div className="rounded-lg border border-border bg-surface-soft px-4 py-3 text-right">
            <p className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">Current balance</p>
            <p className="text-2xl font-semibold text-ink">{loyaltyBalance}</p>
            <p className="text-xs text-muted">{loyaltyTransactions.length} total transactions</p>
          </div>
        </div>

        {generatedCoupon && (
          <div className="mt-4 flex items-start gap-3 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm dark:border-amber-700 dark:bg-amber-900/20">
            <Ticket size={16} className="mt-0.5 shrink-0 text-amber-600 dark:text-amber-400" aria-hidden="true" />
            <div className="min-w-0 flex-1">
              <p className="font-semibold text-ink">New coupon generated</p>
              <p className="text-xs text-muted">
                {generatedCoupon.code || generatedCoupon.couponCode} · {generatedCoupon.couponType || generatedCoupon.type}
                {generatedCoupon.expiresAt ? ` · expires ${formatDate(generatedCoupon.expiresAt)}` : ''}
              </p>
            </div>
            <button
              type="button"
              onClick={() => setGeneratedCoupon(null)}
              className="text-xs font-medium text-amber-700 underline-offset-2 hover:underline dark:text-amber-300"
            >
              Dismiss
            </button>
          </div>
        )}

        <div className="mt-4 grid gap-4 xl:grid-cols-2">
          <section className="space-y-3">
            <h4 className="text-sm font-semibold text-ink">Generate coupon</h4>
            <form className="space-y-3" onSubmit={handleLoyaltyCouponGenerate}>
              <label className="block">
                <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">Reward type</span>
                <select
                  value={couponForm.couponType}
                  onChange={(event) => setCouponForm((current) => ({ ...current, couponType: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                >
                  {availableRewards.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>

              {couponForm.couponType === 'FREE_RIDE' && (
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">Ride code</span>
                  <input
                    value={couponForm.rideCode}
                    onChange={(event) => setCouponForm((current) => ({ ...current, rideCode: event.target.value }))}
                    className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                    placeholder="e.g. 11A"
                  />
                </label>
              )}

              <p className="rounded-lg border border-border bg-surface px-3 py-2 text-xs text-muted">
                {availableRewards.find((option) => option.value === couponForm.couponType)?.description}
              </p>

              <button
                type="submit"
                disabled={loyaltyBalance <= 0}
                className="inline-flex items-center gap-2 rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-70"
              >
                Generate coupon
              </button>
            </form>

            <h4 className="pt-2 text-sm font-semibold text-ink">Redeem points</h4>
            <form className="space-y-3" onSubmit={handleLoyaltyRedeem}>
              <label className="block">
                <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">Points to redeem</span>
                <input
                  type="number"
                  min="1"
                  step="1"
                  value={redeemForm.points}
                  onChange={(event) => setRedeemForm((current) => ({ ...current, points: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                />
              </label>

              <label className="block">
                <span className="text-xs font-semibold uppercase tracking-[0.1em] text-muted">Reward type</span>
                <select
                  value={redeemForm.rewardType}
                  onChange={(event) => setRedeemForm((current) => ({ ...current, rewardType: event.target.value }))}
                  className="mt-1 w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                >
                  {availableRewards.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>

              {selectedReward && (
                <p className="rounded-lg border border-border bg-surface px-3 py-2 text-xs text-muted">
                  {selectedReward.description}
                </p>
              )}

              <button
                type="submit"
                disabled={!redeemForm.points || Number(redeemForm.points) > loyaltyBalance}
                className="inline-flex items-center gap-2 rounded-lg bg-accent px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-70"
              >
                Redeem loyalty points
              </button>
            </form>
          </section>

          <section className="space-y-3">
            <h4 className="text-sm font-semibold text-ink">Point history</h4>
            {loyaltyTransactions.length === 0 ? (
              <p className="text-sm text-muted">No loyalty activity yet.</p>
            ) : (
              <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
                {loyaltyTransactions.map((transaction) => {
                  const movement = transaction.transactionType === 'REDEEM'
                    ? -1 * (transaction.pointsSpent || transaction.points || 0)
                    : transaction.pointsEarned || transaction.points || 0
                  return (
                    <article key={transaction.id} className="rounded-lg border border-border bg-surface px-3 py-2 text-sm">
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="font-medium text-ink">{transaction.description}</p>
                          <p className="text-xs text-muted">
                            {transaction.referenceType} · {formatDateTime(transaction.createdAt)}
                          </p>
                        </div>
                        <span
                          className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold ${
                            movement >= 0
                              ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400'
                              : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
                          }`}
                        >
                          {movement >= 0 ? '+' : ''}{movement}
                        </span>
                      </div>
                      {transaction.expiryDate ? (
                        <p className="mt-1 text-xs text-muted">Expires {formatDate(transaction.expiryDate)}</p>
                      ) : null}
                    </article>
                  )
                })}
              </div>
            )}

            {loyaltyCoupons.length > 0 && (
              <>
                <h4 className="pt-2 text-sm font-semibold text-ink">My coupons</h4>
                <ul className="space-y-2">
                  {loyaltyCoupons.map((coupon) => (
                    <li key={coupon.id || coupon.code} className="flex items-center justify-between rounded-lg border border-border bg-surface px-3 py-2 text-sm">
                      <div>
                        <p className="font-medium text-ink">{coupon.code || coupon.couponCode}</p>
                        <p className="text-xs text-muted">
                          {coupon.couponType || coupon.type}
                          {coupon.expiresAt ? ` · expires ${formatDate(coupon.expiresAt)}` : ''}
                        </p>
                      </div>
                      {coupon.status && (
                        <span className="rounded-full bg-surface-alt px-2 py-0.5 text-xs font-semibold text-muted">
                          {coupon.status}
                        </span>
                      )}
                    </li>
                  ))}
                </ul>
              </>
            )}
          </section>
        </div>
      </PanelCard>
    </div>
  )
}
