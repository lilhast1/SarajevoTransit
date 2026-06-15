import { AlertCircle, CheckCircle, CreditCard, RefreshCw, Ticket, Trash2, X } from 'lucide-react'
import { QRCodeSVG } from 'qrcode.react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { useAppContext } from '../context/AppContext'
import { gatewayClient } from '../services/gatewayClient'

const TICKET_STATUS_STYLES = {
  ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  USED: 'bg-surface-alt text-muted',
  EXPIRED: 'bg-surface-alt text-muted',
  CANCELLED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

const STRIPE_TEST_CARDS = [
  { number: '4242424242424242', token: 'pm_card_visa', cardType: 'VISA' },
  { number: '5555555555554444', token: 'pm_card_mastercard', cardType: 'MASTERCARD' },
  { number: '378282246310005',  token: 'pm_card_amex', cardType: 'AMEX' },
  { number: '4000000000000002', token: 'pm_card_visa_chargeDeclined', cardType: 'VISA' },
]

function detectCardType(digits) {
  if (/^4/.test(digits)) return 'VISA'
  if (/^5[1-5]/.test(digits)) return 'MASTERCARD'
  if (/^3[47]/.test(digits)) return 'AMEX'
  return 'VISA'
}

function resolveStripeToken(digits) {
  const match = STRIPE_TEST_CARDS.find((c) => c.number === digits)
  return match ? match.token : 'pm_card_visa'
}

function formatCardNumber(value) {
  const digits = value.replace(/\D/g, '').slice(0, 16)
  return digits.replace(/(.{4})/g, '$1 ').trim()
}

function formatExpiry(value) {
  const digits = value.replace(/\D/g, '').slice(0, 4)
  if (digits.length >= 3) return digits.slice(0, 2) + ' / ' + digits.slice(2)
  if (digits.length === 2) return digits + ' / '
  return digits
}

function formatDatetime(iso) {
  if (!iso) return '?'
  return new Date(iso).toLocaleString([], {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function TicketRow({ ticket }) {
  const { t } = useTranslation('tickets')
  const TICKET_TYPES_LOCAL = [
    { type: 'SINGLE', label: t('type_single') },
    { type: 'DAILY', label: t('type_daily') },
    { type: 'WEEKLY', label: t('type_weekly') },
    { type: 'MONTHLY', label: t('type_monthly') },
  ]
  const statusStyle = TICKET_STATUS_STYLES[ticket.status] || 'bg-surface-alt text-muted'
  const typeInfo = TICKET_TYPES_LOCAL.find((ti) => ti.type === ticket.type)

  return (
    <div className="rounded-panel border border-border px-4 py-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex items-center gap-2">
          <Ticket size={15} className="shrink-0 text-muted" />
          <span className="text-sm font-medium text-ink">
            {typeInfo?.label ?? ticket.type}
          </span>
          <span className={`rounded px-2 py-0.5 text-xs font-semibold ${statusStyle}`}>
            {ticket.status}
          </span>
        </div>
        <span className="text-sm font-semibold text-ink">{ticket.amount} BAM</span>
      </div>

      <div className="mt-1 flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-muted">
        <span>{t('purchased', { date: formatDatetime(ticket.purchaseDate) })}</span>
        {ticket.validUntil && <span>{t('valid_until', { date: formatDatetime(ticket.validUntil) })}</span>}
      </div>

      {ticket.status === 'ACTIVE' && ticket.qrCodeData && (
        <div className="mt-3 flex flex-col items-center gap-2 rounded border border-emerald-200 bg-emerald-50 p-3 dark:border-emerald-900 dark:bg-emerald-950/20">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-emerald-700 dark:text-emerald-400">
            Scan to board
          </p>
          <div className="rounded bg-white p-2">
            <QRCodeSVG value={ticket.qrCodeData} size={148} level="M" />
          </div>
          <code className="break-all text-center text-[11px] text-emerald-800 dark:text-emerald-300">
            {ticket.qrCodeData}
          </code>
        </div>
      )}
    </div>
  )
}

function PaymentMethodCard({ method, selected, onSelect, onRemove, removing }) {
  const { t } = useTranslation('tickets')
  return (
    <button
      type="button"
      onClick={() => onSelect(method.id)}
      className={`flex w-full items-center justify-between gap-3 rounded-panel border px-3 py-2.5 text-left transition ${
        selected
          ? 'border-accent bg-accent/5'
          : 'border-border hover:bg-surface-alt'
      }`}
    >
      <div className="flex items-center gap-2">
        <div className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2 ${
          selected ? 'border-accent bg-accent' : 'border-border'
        }`}>
          {selected && <CheckCircle size={10} className="text-white" />}
        </div>
        <CreditCard size={14} className="text-muted" />
        <span className="text-sm text-ink">???? {method.lastFour}</span>
        <span className="text-xs text-muted">{method.cardType}</span>
        {method.isDefault && (
          <span className="rounded bg-surface-alt px-1.5 py-0.5 text-[10px] font-semibold text-muted">
            {t('default_label')}
          </span>
        )}
      </div>
      <button
        type="button"
        onClick={(e) => { e.stopPropagation(); onRemove(method.id) }}
        disabled={removing}
        className="rounded p-1 text-muted transition hover:bg-red-50 hover:text-red-600 disabled:opacity-40 dark:hover:bg-red-950/30"
        aria-label={t('remove_card')}
      >
        <Trash2 size={13} aria-hidden="true" />
      </button>
    </button>
  )
}

function AddCardForm({ onAdded, userId }) {
  const { t } = useTranslation('tickets')
  const [cardNumber, setCardNumber] = useState('')
  const [expiry, setExpiry] = useState('')
  const [cvv, setCvv] = useState('')
  const [isDefault, setIsDefault] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  const digits = cardNumber.replace(/\D/g, '')
  const cardType = detectCardType(digits)
  const isAmex = cardType === 'AMEX'
  const expectedLength = isAmex ? 15 : 16
  const cvvLength = isAmex ? 4 : 3

  function validate() {
    const errors = {}
    if (digits.length !== expectedLength) {
      errors.cardNumber = `Card number must be ${expectedLength} digits`
    }
    const expiryDigits = expiry.replace(/\D/g, '')
    if (expiryDigits.length !== 4) {
      errors.expiry = 'Enter expiry as MM / YY'
    } else {
      const mm = parseInt(expiryDigits.slice(0, 2), 10)
      const yy = parseInt(expiryDigits.slice(2), 10)
      const now = new Date()
      const fullYear = 2000 + yy
      if (mm < 1 || mm > 12) errors.expiry = 'Invalid month'
      else if (fullYear < now.getFullYear() || (fullYear === now.getFullYear() && mm < now.getMonth() + 1))
        errors.expiry = 'Card has expired'
    }
    if (cvv.length !== cvvLength) {
      errors.cvv = `CVV must be ${cvvLength} digits`
    }
    return errors
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errors = validate()
    if (Object.keys(errors).length > 0) { setFieldErrors(errors); return }
    setFieldErrors({})
    setSubmitting(true)
    setError(null)
    try {
      const newMethod = await gatewayClient.addPaymentMethod({
        userId,
        provider: 'STRIPE',
        gatewayToken: resolveStripeToken(digits),
        lastFour: digits.slice(-4),
        cardType,
        isDefault,
      })
      setCardNumber(''); setExpiry(''); setCvv(''); setIsDefault(false)
      onAdded(newMethod)
    } catch (err) {
      setError(err.message || 'Failed to add card')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-3 flex flex-col gap-3 rounded-panel border border-dashed border-border p-4">
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold text-muted">Add payment card</p>
        {digits.length > 0 && (
          <span className="text-xs font-semibold text-accent">{cardType}</span>
        )}
      </div>

      <div className="flex flex-col gap-1">
        <div className="relative">
          <CreditCard size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
          <input
            type="text"
            inputMode="numeric"
            placeholder="Card number"
            value={cardNumber}
            onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
            className={`w-full rounded-panel border bg-surface py-2 pl-9 pr-3 text-sm text-ink placeholder:text-muted focus:outline-none ${
              fieldErrors.cardNumber ? 'border-red-400 focus:border-red-400' : 'border-border focus:border-accent'
            }`}
          />
        </div>
        {fieldErrors.cardNumber && <p className="text-[11px] text-red-500">{fieldErrors.cardNumber}</p>}
      </div>

      <div className="flex gap-2">
        <div className="flex flex-1 flex-col gap-1">
          <input
            type="text"
            inputMode="numeric"
            placeholder="MM / YY"
            value={expiry}
            onChange={(e) => setExpiry(formatExpiry(e.target.value))}
            className={`w-full rounded-panel border bg-surface px-3 py-2 text-sm text-ink placeholder:text-muted focus:outline-none ${
              fieldErrors.expiry ? 'border-red-400 focus:border-red-400' : 'border-border focus:border-accent'
            }`}
          />
          {fieldErrors.expiry && <p className="text-[11px] text-red-500">{fieldErrors.expiry}</p>}
        </div>
        <div className="flex w-24 flex-col gap-1">
          <input
            type="text"
            inputMode="numeric"
            placeholder="CVV"
            maxLength={cvvLength}
            value={cvv}
            onChange={(e) => setCvv(e.target.value.replace(/\D/g, '').slice(0, cvvLength))}
            className={`w-full rounded-panel border bg-surface px-3 py-2 text-sm text-ink placeholder:text-muted focus:outline-none ${
              fieldErrors.cvv ? 'border-red-400 focus:border-red-400' : 'border-border focus:border-accent'
            }`}
          />
          {fieldErrors.cvv && <p className="text-[11px] text-red-500">{fieldErrors.cvv}</p>}
        </div>
      </div>

      <label className="flex items-center gap-2 text-xs text-muted">
        <input
          type="checkbox"
          checked={isDefault}
          onChange={(e) => setIsDefault(e.target.checked)}
          className="accent-accent"
        />
        {t('set_default')}
      </label>

      {error && <ErrorAlert error={error} onDismiss={() => setError(null)} />}

      <button
        type="submit"
        disabled={submitting}
        className="rounded-panel bg-accent px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-60"
      >
        {submitting ? t('adding') : t('add_card')}
      </button>
    </form>
  )
}

export function TicketsPage() {
  const { isAuthenticated, session } = useAppContext()
  const { t } = useTranslation('tickets')
  const TICKET_TYPES = [
    { type: 'SINGLE', label: t('type_single'), price: t('price_single'), duration: t('validity_single') },
    { type: 'DAILY', label: t('type_daily'), price: t('price_daily'), duration: t('validity_daily') },
    { type: 'WEEKLY', label: t('type_weekly'), price: t('price_weekly'), duration: t('validity_weekly') },
    { type: 'MONTHLY', label: t('type_monthly'), price: t('price_monthly'), duration: t('validity_monthly') },
  ]

  const [selectedType, setSelectedType] = useState(null)
  const [selectedMethodId, setSelectedMethodId] = useState(null)
  const [showAddCard, setShowAddCard] = useState(false)
  const [purchasing, setPurchasing] = useState(false)
  const [purchaseError, setPurchaseError] = useState(null)
  const [purchaseSuccess, setPurchaseSuccess] = useState(null)
  const [couponCode, setCouponCode] = useState('')

  const [methods, setMethods] = useState([])
  const [methodsLoading, setMethodsLoading] = useState(true)
  const [methodsError, setMethodsError] = useState(null)
  const [removingId, setRemovingId] = useState(null)
  const [userCoupons, setUserCoupons] = useState([])

  const [tickets, setTickets] = useState([])
  const [ticketsLoading, setTicketsLoading] = useState(true)
  const [ticketsError, setTicketsError] = useState(null)

  useEffect(() => {
    if (!isAuthenticated || !session?.userId) return
    loadMethods()
    loadTickets()
    loadCoupons()
  }, [isAuthenticated, session?.userId])

  async function loadCoupons() {
    try {
      const data = await gatewayClient.getUserLoyaltyCoupons(session.userId)
      const list = Array.isArray(data) ? data : []
      setUserCoupons(list.filter((c) => c.active === true))
    } catch {
      setUserCoupons([])
    }
  }

  async function loadMethods() {
    setMethodsLoading(true)
    setMethodsError(null)
    try {
      const data = await gatewayClient.getPaymentMethods(session.userId)
      const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
      setMethods(list)
      if (list.length > 0 && !selectedMethodId) {
        const def = list.find((m) => m.isDefault) || list[0]
        setSelectedMethodId(def.id)
      }
      if (list.length === 0) setShowAddCard(true)
    } catch (err) {
      setMethods([])
      setSelectedMethodId(null)
      setMethodsError(err.message || t('payment_methods_load_failed'))
    } finally {
      setMethodsLoading(false)
    }
  }

  async function loadTickets() {
    setTicketsLoading(true)
    setTicketsError(null)
    try {
      const data = await gatewayClient.getWallet(session.userId, '?size=20&sort=purchaseDate,desc')
      const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
      setTickets(list)
    } catch (err) {
      setTicketsError(err.message || 'Failed to load tickets')
    } finally {
      setTicketsLoading(false)
    }
  }

  async function handlePurchase() {
    if (!selectedType || !selectedMethodId) return
    setPurchasing(true)
    setPurchaseError(null)
    setPurchaseSuccess(null)
    try {
      const payload = {
        userId: session.userId,
        ticketType: selectedType,
        paymentMethodId: selectedMethodId,
      }
      if (couponCode && couponCode.trim().length > 0) {
        const code = couponCode.trim()
        const selectedCoupon = userCoupons.find((c) => c.couponCode === code)
        if (selectedCoupon?.couponType === 'FREE_RIDE' && selectedType !== 'SINGLE') {
          throw new Error('Free ride coupons can only be used with single ride tickets')
        }
        payload.couponCode = code
        if (selectedCoupon?.couponType === 'FREE_RIDE' && selectedCoupon?.rideCode) {
          payload.rideCode = selectedCoupon.rideCode
        }
      }

      await gatewayClient.purchaseTicket(payload)
      setPurchaseSuccess(t('ticket_purchased'))
      setSelectedType(null)
      setCouponCode('')
      await loadTickets()
      await loadCoupons()
    } catch (err) {
      setPurchaseError(err.message || 'Purchase failed')
    } finally {
      setPurchasing(false)
    }
  }

  async function handleRemoveMethod(methodId) {
    setRemovingId(methodId)
    setMethodsError(null)
    try {
      await gatewayClient.removePaymentMethod(methodId)
      setMethods((prev) => prev.filter((m) => m.id !== methodId))
      if (selectedMethodId === methodId) {
        const remaining = methods.filter((m) => m.id !== methodId)
        setSelectedMethodId(remaining.length > 0 ? remaining[0].id : null)
      }
    } catch (err) {
      setMethodsError(err.message || t('remove_card_failed'))
    } finally {
      setRemovingId(null)
    }
  }

  function handleMethodAdded(newMethod) {
    setMethods((prev) => [...prev, newMethod])
    setSelectedMethodId(newMethod.id)
    setShowAddCard(false)
  }

  if (!isAuthenticated) {
    return (
      <div className="flex items-center gap-2 rounded-panel border border-border px-4 py-10 text-muted">
        <AlertCircle size={16} />
        <span className="text-sm">{t('login_required')}</span>
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h2 className="flex items-center gap-2 text-lg font-semibold text-ink">
          <Ticket size={18} aria-hidden="true" />
          {t('title')}
        </h2>
        <p className="text-xs text-muted">{session?.email}</p>
      </div>

      {/* ?? Buy a ticket ?? */}
      <section className="rounded-panel border border-border p-4">
        <h3 className="mb-3 text-sm font-semibold text-ink">{t('buy_title')}</h3>

        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {TICKET_TYPES.map((t) => (
            <button
              key={t.type}
              type="button"
              onClick={() => setSelectedType(t.type)}
              className={`flex flex-col rounded-panel border px-3 py-3 text-left transition ${
                selectedType === t.type
                  ? 'border-accent bg-accent/5'
                  : 'border-border hover:bg-surface-alt'
              }`}
            >
              <span className="text-sm font-semibold text-ink">{t.label}</span>
              <span className="mt-0.5 text-base font-bold text-accent">{t.price}</span>
              <span className="mt-0.5 text-[11px] text-muted">{t.duration}</span>
            </button>
          ))}
        </div>

        {selectedType && (
          <div className="mt-4 flex flex-col gap-2">
            <p className="text-xs font-semibold text-muted">{t('pay_with')}</p>

            {methodsLoading ? (
              <p className="text-sm text-muted">{t('loading_payment')}</p>
            ) : (
              <>
                {methodsError && <ErrorAlert error={methodsError} onDismiss={() => setMethodsError(null)} />}

                {methods.map((m) => (
                  <PaymentMethodCard
                    key={m.id}
                    method={m}
                    selected={selectedMethodId === m.id}
                    onSelect={setSelectedMethodId}
                    onRemove={handleRemoveMethod}
                    removing={removingId === m.id}
                  />
                ))}

                {methods.length > 0 && (
                  <button
                    type="button"
                    onClick={() => setShowAddCard((v) => !v)}
                    className="self-start text-xs text-accent underline-offset-2 hover:underline"
                  >
                    {showAddCard ? t('close') : t('add_card_link')}
                  </button>
                )}

                {showAddCard && (
                  <AddCardForm userId={session.userId} onAdded={handleMethodAdded} />
                )}
              </>
            )}

            {purchaseError && <ErrorAlert error={purchaseError} onDismiss={() => setPurchaseError(null)} />}
            {purchaseSuccess && <SuccessAlert message={purchaseSuccess} onDismiss={() => setPurchaseSuccess(null)} />}

            <div className="flex flex-col gap-2">
              <label className="text-xs text-muted">Have a coupon? (optional)</label>
              {userCoupons.length === 0 ? (
                <input
                  type="text"
                  placeholder="Paste a coupon code (optional)"
                  value={couponCode}
                  onChange={(e) => setCouponCode(e.target.value)}
                  className="w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
                />
              ) : (
                <select
                  value={couponCode}
                  onChange={(e) => setCouponCode(e.target.value)}
                  className="w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent"
                >
                  <option value="">No coupon</option>
                  {userCoupons
                    .filter((c) => c.couponType !== 'FREE_RIDE' || selectedType === 'SINGLE')
                    .map((c) => (
                    <option key={c.couponCode} value={c.couponCode}>
                      {c.couponCode}{c.couponType === 'DISCOUNT' && c.discountPercent ? ` ? ${c.discountPercent}% off` : c.couponType === 'FREE_RIDE' ? ` ? Free ride (${c.rideCode})` : ''}
                    </option>
                  ))}
                </select>
              )}
            </div>

            <div className="flex items-center justify-between pt-1">
              <button
                type="button"
                onClick={() => { setSelectedType(null); setPurchaseError(null); setPurchaseSuccess(null) }}
                className="text-xs text-muted hover:text-ink"
              >
                <X size={13} className="inline mr-1" />
                Cancel
              </button>
              <button
                type="button"
                onClick={handlePurchase}
                disabled={purchasing || !selectedMethodId}
                className="rounded-panel bg-accent px-5 py-1.5 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-60"
              >
                {purchasing ? t('processing') : t('buy_btn', { price: TICKET_TYPES.find((tt) => tt.type === selectedType)?.price })}
              </button>
            </div>
          </div>
        )}
      </section>

      {/* ?? My Tickets ?? */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-ink">{t('my_tickets')}</h3>
          <button
            type="button"
            onClick={loadTickets}
            disabled={ticketsLoading}
            className="rounded-panel border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink"
            aria-label={t('refresh_tickets')}
          >
            <RefreshCw size={13} className={ticketsLoading ? 'animate-spin' : ''} />
          </button>
        </div>

        {ticketsError && <ErrorAlert error={ticketsError} />}

        {!ticketsLoading && tickets.length === 0 && !ticketsError && (
          <p className="text-sm text-muted">{t('no_tickets')}</p>
        )}

        {tickets.map((t) => <TicketRow key={t.id} ticket={t} />)}
      </section>

      {/* ?? Payment Methods ?? */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-ink">{t('payment_methods')}</h3>
        </div>

        {methodsError && <ErrorAlert error={methodsError} onDismiss={() => setMethodsError(null)} />}

        {!methodsLoading && methods.length === 0 && !showAddCard && (
          <p className="text-sm text-muted">{t('no_payment_methods')}</p>
        )}

        {methods.map((m) => (
          <div key={m.id} className="flex items-center gap-2 rounded-panel border border-border px-3 py-2.5">
            <CreditCard size={14} className="shrink-0 text-muted" />
            <span className="flex-1 text-sm text-ink">???? {m.lastFour}</span>
            <span className="text-xs text-muted">{m.cardType}</span>
            {m.isDefault && (
              <span className="rounded bg-surface-alt px-1.5 py-0.5 text-[10px] font-semibold text-muted">
                {t('default_label')}
              </span>
            )}
            <button
              type="button"
              onClick={() => handleRemoveMethod(m.id)}
              disabled={removingId === m.id}
              className="rounded p-1 text-muted transition hover:bg-red-50 hover:text-red-600 disabled:opacity-40 dark:hover:bg-red-950/30"
              aria-label={t('remove_card')}
            >
              <Trash2 size={13} aria-hidden="true" />
            </button>
          </div>
        ))}

        {!showAddCard && (
          <button
            type="button"
            onClick={() => setShowAddCard(true)}
            className="self-start text-xs text-accent underline-offset-2 hover:underline"
          >
            {t('add_card_btn')}
          </button>
        )}

        {showAddCard && !selectedType && (
          <AddCardForm userId={session.userId} onAdded={handleMethodAdded} />
        )}
      </section>
    </div>
  )
}
