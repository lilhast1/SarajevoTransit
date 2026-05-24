import { useEffect, useState } from 'react'
import { Heart, X, Loader2, CheckCircle, AlertCircle, Pencil } from 'lucide-react'
import { useTranslation } from 'react-i18next'

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']

export function SubscriptionModal({ isOpen, onClose, onSubmit, lineName, mode = 'create', initialValues }) {
  const { t } = useTranslation('lines')
  const [startInterval, setStartInterval] = useState('00:00')
  const [endInterval, setEndInterval] = useState('23:59')
  const [daysOfWeek, setDaysOfWeek] = useState(() => new Set(DAYS))
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)

  const isEdit = mode === 'edit'

  const DAY_LABELS = {
    MON: t('days_mon'), TUE: t('days_tue'), WED: t('days_wed'),
    THU: t('days_thu'), FRI: t('days_fri'), SAT: t('days_sat'), SUN: t('days_sun'),
  }

  useEffect(() => {
    if (!isOpen) return
    if (isEdit && initialValues) {
      setStartInterval(initialValues.startInterval || '00:00')
      setEndInterval(initialValues.endInterval || '23:59')
      setDaysOfWeek(new Set((initialValues.daysOfWeek || '').split(',').filter(Boolean)))
    } else {
      setStartInterval('00:00')
      setEndInterval('23:59')
      setDaysOfWeek(new Set(DAYS))
    }
    setResult(null)
  }, [isOpen, isEdit, initialValues])

  if (!isOpen) return null

  function toggleDay(day) {
    setDaysOfWeek((prev) => {
      const next = new Set(prev)
      if (next.has(day)) next.delete(day)
      else next.add(day)
      return next
    })
  }

  async function handleSubmit() {
    if (daysOfWeek.size === 0) return
    setLoading(true)
    setResult(null)
    try {
      await onSubmit({
        startInterval: startInterval + ':00',
        endInterval: endInterval + ':00',
        daysOfWeek: Array.from(daysOfWeek).join(','),
      })
      setResult('success')
    } catch (err) {
      setResult({ type: 'error', message: err.message || 'Operation failed' })
    } finally {
      setLoading(false)
    }
  }

  function handleClose() {
    setResult(null)
    onClose()
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
      onClick={handleClose}
    >
      <div className="w-full max-w-sm rounded-panel border border-border bg-surface p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
        {result === 'success' ? (
          <>
            <div className="flex flex-col items-center gap-3 py-4">
              <CheckCircle size={40} className="text-green-500" aria-hidden="true" />
              <h2 className="text-base font-semibold text-ink">
                {isEdit ? t('sub_updated') : t('sub_subscribed')}
              </h2>
              <p className="text-center text-sm text-muted">
                {isEdit ? t('sub_preferences_saved') : t('sub_will_receive', { line: lineName })}
              </p>
            </div>
            <button
              type="button"
              onClick={handleClose}
              className="mt-4 w-full rounded-panel border border-accent bg-accent py-2 text-sm font-medium text-white"
            >
              {t('sub_done')}
            </button>
          </>
        ) : result && result.type === 'error' ? (
          <>
            <div className="flex items-start gap-3">
              <AlertCircle size={20} className="mt-0.5 flex-shrink-0 text-red-500" aria-hidden="true" />
              <div className="flex-1">
                <h2 className="text-base font-semibold text-ink">
                  {isEdit ? t('sub_update_failed') : t('sub_subscribe_failed')}
                </h2>
                <p className="mt-1 text-sm text-muted">{result.message}</p>
              </div>
              <button type="button" onClick={handleClose} className="text-muted hover:text-ink" aria-label={t('sub_cancel')}>
                <X size={16} aria-hidden="true" />
              </button>
            </div>
            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={handleSubmit}
                className="flex-1 rounded-panel border border-accent bg-accent py-2 text-sm font-medium text-white"
              >
                {t('sub_retry')}
              </button>
              <button
                type="button"
                onClick={handleClose}
                className="flex-1 rounded-panel border border-border py-2 text-sm font-medium text-ink hover:bg-surface-alt"
              >
                {t('sub_cancel')}
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                {isEdit ? (
                  <Pencil size={18} className="text-accent" aria-hidden="true" />
                ) : (
                  <Heart size={18} className="text-accent" fill="currentColor" aria-hidden="true" />
                )}
                <h2 className="text-base font-semibold text-ink">
                  {isEdit ? t('sub_edit_title') : t('sub_subscribe_title', { line: lineName })}
                </h2>
              </div>
              <button type="button" onClick={handleClose} className="text-muted hover:text-ink" aria-label={t('sub_cancel')}>
                <X size={16} aria-hidden="true" />
              </button>
            </div>

            <p className="mb-4 mt-2 text-sm text-muted">
              {isEdit ? t('sub_edit_hint') : t('sub_set_hint')}
            </p>

            <div className="mb-4 grid grid-cols-2 gap-3">
              <label className="space-y-1">
                <span className="text-xs font-medium text-muted">{t('sub_start_time')}</span>
                <input
                  type="time"
                  value={startInterval}
                  onChange={(e) => setStartInterval(e.target.value)}
                  className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                />
              </label>
              <label className="space-y-1">
                <span className="text-xs font-medium text-muted">{t('sub_end_time')}</span>
                <input
                  type="time"
                  value={endInterval}
                  onChange={(e) => setEndInterval(e.target.value)}
                  className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring"
                />
              </label>
            </div>

            <div className="mb-5">
              <p className="mb-2 text-xs font-medium text-muted">{t('sub_days')}</p>
              <div className="flex flex-wrap gap-2">
                {DAYS.map((day) => (
                  <button
                    key={day}
                    type="button"
                    onClick={() => toggleDay(day)}
                    aria-pressed={daysOfWeek.has(day)}
                    className={`rounded-lg border px-3 py-1.5 text-xs font-medium transition ${
                      daysOfWeek.has(day)
                        ? 'border-accent bg-accent text-white'
                        : 'border-border text-muted hover:bg-surface-alt hover:text-ink'
                    }`}
                  >
                    {DAY_LABELS[day]}
                  </button>
                ))}
              </div>
              {daysOfWeek.size === 0 && (
                <p role="alert" className="mt-1 text-xs text-red-500">{t('sub_select_day')}</p>
              )}
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleSubmit}
                disabled={loading || daysOfWeek.size === 0}
                className="flex-1 rounded-panel border border-accent bg-accent py-2 text-sm font-medium text-white disabled:opacity-50"
              >
                {loading ? (
                  <span className="inline-flex items-center gap-2">
                    <Loader2 size={14} className="animate-spin" aria-hidden="true" />
                    {isEdit ? t('sub_saving') : t('sub_subscribing')}
                  </span>
                ) : (
                  isEdit ? t('sub_save_changes') : t('sub_subscribe_btn')
                )}
              </button>
              <button
                type="button"
                onClick={handleClose}
                disabled={loading}
                className="flex-1 rounded-panel border border-border py-2 text-sm font-medium text-ink hover:bg-surface-alt disabled:opacity-50"
              >
                {t('sub_cancel')}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
