import { AlertCircle, CheckCircle, CreditCard, RefreshCw, Ticket, Trash2, X } from 'lucide-react'
import { QRCodeSVG } from 'qrcode.react'
import { useEffect, useState } from 'react'
import { ErrorAlert, SuccessAlert } from '../components/common/Alerts'
import { useAppContext } from '../context/AppContext'
import { gatewayClient } from '../services/gatewayClient'

const TICKET_TYPES = [
  { type: 'SINGLE', label: 'Single', price: '1.80 BAM', duration: 'Valid 1 hour' },
  { type: 'DAILY', label: 'Daily', price: '5.00 BAM', duration: 'Valid 24 hours' },
  { type: 'WEEKLY', label: 'Weekly', price: '20.00 BAM', duration: 'Valid 7 days' },
  { type: 'MONTHLY', label: 'Monthly', price: '50.00 BAM', duration: 'Valid 30 days' },
]

const TICKET_STATUS_STYLES = {
  ACTIVE: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
  PENDING: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
  USED: 'bg-surface-alt text-muted',
  EXPIRED: 'bg-surface-alt text-muted',
  CANCELLED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
}

// Stripe shared test PaymentMethod tokens — each charges (or declines) in Stripe test mode.
const STRIPE_TEST_CARDS = [
  { token: 'pm_card_visa', label: 'Visa — success', lastFour: '4242', cardType: 'VISA' },
  { token: 'pm_card_mastercard', label: 'Mastercard — success', lastFour: '4444', cardType: 'MASTERCARD' },
  { token: 'pm_card_amex', label: 'Amex — success', lastFour: '8431', cardType: 'AMEX' },
  { token: 'pm_card_visa_chargeDeclined', label: 'Visa — declined', lastFour: '0002', cardType: 'VISA' },
]

function formatDatetime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString([], {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function TicketRow({ ticket }) {
  const statusStyle = TICKET_STATUS_STYLES[ticket.status] || 'bg-surface-alt text-muted'
  const typeInfo = TICKET_TYPES.find((t) => t.type === ticket.type)

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
        <span>Purchased: {formatDatetime(ticket.purchaseDate)}</span>
        {ticket.validUntil && <span>Valid until: {formatDatetime(ticket.validUntil)}</span>}
      </div>

      {ticket.status === 'ACTIVE' && ticket.qrCodeData && (
        <div className="mt-3 flex flex-col items-center gap-2 rounded border border-emerald-200 bg-emerald-50 p-3 dark:border-emerald-900 dark:bg-emerald-950/20">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-emerald-700 dark:text-emerald-400">
            Scan to board
          </p>
          {/* QR needs a light background to remain scannable in dark mode */}
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
            Default
          </span>
        )}
      </div>
      <button
        type="button"
        onClick={(e) => { e.stopPropagation(); onRemove(method.id) }}
        disabled={removing}
        className="rounded p-1 text-muted transition hover:bg-red-50 hover:text-red-600 disabled:opacity-40 dark:hover:bg-red-950/30"
        aria-label="Remove card"
      >
        <Trash2 size={13} />
      </button>
    </button>
  )
}

function AddCardForm({ onAdded, userId }) {
  const [selectedToken, setSelectedToken] = useState(STRIPE_TEST_CARDS[0].token)
  const [isDefault, setIsDefault] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    const card = STRIPE_TEST_CARDS.find((c) => c.token === selectedToken)
    if (!card) return
    setSubmitting(true)
    setError(null)
    try {
      const newMethod = await gatewayClient.addPaymentMethod({
        userId,
        provider: 'STRIPE',
        gatewayToken: card.token,
        lastFour: card.lastFour,
        cardType: card.cardType,
        isDefault,
      })
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
      <p className="text-xs font-semibold text-muted">Add Stripe Test Card</p>
      <select
        value={selectedToken}
        onChange={(e) => setSelectedToken(e.target.value)}
        className="w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none"
      >
        {STRIPE_TEST_CARDS.map((c) => (
          <option key={c.token} value={c.token}>
            {c.label} •••• {c.lastFour}
          </option>
        ))}
      </select>
      <label className="flex items-center gap-2 text-xs text-muted">
        <input
          type="checkbox"
          checked={isDefault}
          onChange={(e) => setIsDefault(e.target.checked)}
          className="accent-accent"
        />
        Set as default
      </label>
      {error && <ErrorAlert error={error} onDismiss={() => setError(null)} />}
      <button
        type="submit"
        disabled={submitting}
        className="self-end rounded-panel bg-accent px-4 py-1.5 text-xs font-medium text-white transition hover:opacity-90 disabled:opacity-60"
      >
        {submitting ? 'Adding…' : 'Add Card'}
      </button>
    </form>
  )
}

export function TicketsPage() {
  const { isAuthenticated, session } = useAppContext()

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
      setPurchaseSuccess('Ticket purchased! It may take a moment to activate.')
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
        <span className="text-sm">Sign in to access your tickets and wallet.</span>
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <h2 className="flex items-center gap-2 text-lg font-semibold text-ink">
          <Ticket size={18} />
          Tickets
        </h2>
        <p className="text-xs text-muted">{session?.email}</p>
      </div>

      {/* ── Buy a ticket ── */}
      <section className="rounded-panel border border-border p-4">
        <h3 className="mb-3 text-sm font-semibold text-ink">Buy a Ticket</h3>

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
            <p className="text-xs font-semibold text-muted">Pay with</p>

            {methodsLoading ? (
              <p className="text-sm text-muted">Loading payment methods…</p>
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
                    {showAddCard ? 'Cancel' : '+ Add another card'}
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
                {purchasing ? 'Processing…' : `Buy — ${TICKET_TYPES.find((t) => t.type === selectedType)?.price}`}
              </button>
            </div>
          </div>
        )}
      </section>

      {/* ── My Tickets ── */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-ink">My Tickets</h3>
          <button
            type="button"
            onClick={loadTickets}
            disabled={ticketsLoading}
            className="rounded-panel border border-border p-1.5 text-muted transition hover:bg-surface-alt hover:text-ink"
            aria-label="Refresh tickets"
          >
            <RefreshCw size={13} className={ticketsLoading ? 'animate-spin' : ''} />
          </button>
        </div>

        {ticketsError && <ErrorAlert error={ticketsError} />}

        {!ticketsLoading && tickets.length === 0 && !ticketsError && (
          <p className="text-sm text-muted">No tickets yet. Buy your first ticket above.</p>
        )}

        {tickets.map((t) => <TicketRow key={t.id} ticket={t} />)}
      </section>

      {/* ── Payment Methods ── */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-ink">Payment Methods</h3>
        </div>

        {!methodsLoading && methods.length === 0 && !showAddCard && (
          <p className="text-sm text-muted">No payment methods saved.</p>
        )}

        {methods.map((m) => (
          <div key={m.id} className="flex items-center gap-2 rounded-panel border border-border px-3 py-2.5">
            <CreditCard size={14} className="shrink-0 text-muted" />
            <span className="flex-1 text-sm text-ink">•••• {m.lastFour}</span>
            <span className="text-xs text-muted">{m.cardType}</span>
            {m.isDefault && (
              <span className="rounded bg-surface-alt px-1.5 py-0.5 text-[10px] font-semibold text-muted">
                Default
              </span>
            )}
            <button
              type="button"
              onClick={() => handleRemoveMethod(m.id)}
              disabled={removingId === m.id}
              className="rounded p-1 text-muted transition hover:bg-red-50 hover:text-red-600 disabled:opacity-40 dark:hover:bg-red-950/30"
              aria-label="Remove card"
            >
              <Trash2 size={13} />
            </button>
          </div>
        ))}

        {!showAddCard && (
          <button
            type="button"
            onClick={() => setShowAddCard(true)}
            className="self-start text-xs text-accent underline-offset-2 hover:underline"
          >
            + Add card
          </button>
        )}

        {showAddCard && !selectedType && (
          <AddCardForm userId={session.userId} onAdded={handleMethodAdded} />
        )}
      </section>
    </div>
  )
}
