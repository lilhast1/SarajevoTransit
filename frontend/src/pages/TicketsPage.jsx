import { AlertCircle, CheckCircle, CreditCard, RefreshCw, Ticket, Trash2, X } from 'lucide-react'
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

const CARD_TYPES = ['VISA', 'MASTERCARD', 'AMEX', 'MAESTRO']

function formatDatetime(iso) {
  if (!iso) return '—'
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
        <div className="mt-2 rounded border border-emerald-200 bg-emerald-50 p-2 dark:border-emerald-900 dark:bg-emerald-950/20">
          <p className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-emerald-700 dark:text-emerald-400">
            {t('ticket_code')}
          </p>
          <code className="break-all text-xs text-emerald-800 dark:text-emerald-300">
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
        <span className="text-sm text-ink">•••• {method.lastFour}</span>
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
  const [lastFour, setLastFour] = useState('')
  const [cardType, setCardType] = useState('VISA')
  const [isDefault, setIsDefault] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    if (lastFour.length !== 4) return
    setSubmitting(true)
    setError(null)
    try {
      const newMethod = await gatewayClient.addPaymentMethod({
        userId,
        provider: 'MOCK',
        gatewayToken: `mock_${Date.now()}`,
        lastFour,
        cardType,
        isDefault,
      })
      setLastFour('')
      setCardType('VISA')
      setIsDefault(false)
      onAdded(newMethod)
    } catch (err) {
      setError(err.message || 'Failed to add card')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-3 flex flex-col gap-2 rounded-panel border border-dashed border-border p-3">
      <p className="text-xs font-semibold text-muted">{t('add_mock_card')}</p>
      <div className="flex gap-2">
        <input
          type="text"
          inputMode="numeric"
          maxLength={4}
          pattern="\d{4}"
          placeholder={t('last_4')}
          value={lastFour}
          onChange={(e) => setLastFour(e.target.value.replace(/\D/g, '').slice(0, 4))}
          required
          className="w-32 rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none"
        />
        <select
          value={cardType}
          onChange={(e) => setCardType(e.target.value)}
          className="flex-1 rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
        >
          {CARD_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
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
        disabled={submitting || lastFour.length !== 4}
        className="self-end rounded-panel bg-accent px-4 py-1.5 text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-60"
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

  const [methods, setMethods] = useState([])
  const [methodsLoading, setMethodsLoading] = useState(true)
  const [removingId, setRemovingId] = useState(null)

  const [tickets, setTickets] = useState([])
  const [ticketsLoading, setTicketsLoading] = useState(true)
  const [ticketsError, setTicketsError] = useState(null)

  useEffect(() => {
    if (!isAuthenticated || !session?.userId) return
    loadMethods()
    loadTickets()
  }, [isAuthenticated, session?.userId])

  async function loadMethods() {
    setMethodsLoading(true)
    try {
      const data = await gatewayClient.getPaymentMethods(session.userId)
      const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : []
      setMethods(list)
      if (list.length > 0 && !selectedMethodId) {
        const def = list.find((m) => m.isDefault) || list[0]
        setSelectedMethodId(def.id)
      }
      if (list.length === 0) setShowAddCard(true)
    } catch {
      // silent — user will see empty list
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
      await gatewayClient.purchaseTicket({
        userId: session.userId,
        ticketType: selectedType,
        paymentMethodId: selectedMethodId,
      })
      setPurchaseSuccess(t('ticket_purchased'))
      setSelectedType(null)
      await loadTickets()
    } catch (err) {
      setPurchaseError(err.message || 'Purchase failed')
    } finally {
      setPurchasing(false)
    }
  }

  async function handleRemoveMethod(methodId) {
    setRemovingId(methodId)
    try {
      await gatewayClient.removePaymentMethod(methodId)
      setMethods((prev) => prev.filter((m) => m.id !== methodId))
      if (selectedMethodId === methodId) {
        const remaining = methods.filter((m) => m.id !== methodId)
        setSelectedMethodId(remaining.length > 0 ? remaining[0].id : null)
      }
    } catch {
      // silent
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

      {/* ── Buy a ticket ── */}
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

      {/* ── My Tickets ── */}
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

      {/* ── Payment Methods ── */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-ink">{t('payment_methods')}</h3>
        </div>

        {!methodsLoading && methods.length === 0 && !showAddCard && (
          <p className="text-sm text-muted">{t('no_payment_methods')}</p>
        )}

        {methods.map((m) => (
          <div key={m.id} className="flex items-center gap-2 rounded-panel border border-border px-3 py-2.5">
            <CreditCard size={14} className="shrink-0 text-muted" />
            <span className="flex-1 text-sm text-ink">•••• {m.lastFour}</span>
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
