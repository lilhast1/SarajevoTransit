import { useEffect, useState, useCallback } from 'react'
import { Plus, Trash2, RefreshCw } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../../components/common/PanelCard'
import { ErrorAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'

function todayISO() {
  return new Date().toISOString().slice(0, 10) // YYYY-MM-DD for <input type="date">
}

const EMPTY_FORM = {
  lineId: '',
  timetableId: '',
  serviceDate: todayISO(), // stored as YYYY-MM-DD, converted to YYYYMMDD on submit
  delayMinutes: '',
  reason: '',
}

export function AdminDelaysPage() {
  const { t } = useTranslation('admin-delays')
  const [delays, setDelays]         = useState([])
  const [lines, setLines]           = useState([])
  const [timetables, setTimetables] = useState([])
  const [form, setForm]             = useState(EMPTY_FORM)
  const [loading, setLoading]       = useState(false)
  const [saving, setSaving]         = useState(false)
  const [error, setError]           = useState(null)

  const loadDelays = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await gatewayClient.getTripDelays()
      setDelays(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadDelays()
    gatewayClient.getLines().then((res) =>
      setLines(Array.isArray(res) ? res : (res?.content ?? []))
    ).catch(() => {})
  }, [loadDelays])

  async function handleLineChange(lineId) {
    setForm((f) => ({ ...f, lineId, timetableId: '' }))
    setTimetables([])
    if (!lineId) return
    try {
      const res = await gatewayClient.getTimetables(`?lineId=${lineId}&activeOnly=true`)
      setTimetables(Array.isArray(res) ? res : (res?.content ?? []))
    } catch (_) {}
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.timetableId || !form.serviceDate || !form.delayMinutes) {
      setError(t('error_required'))
      return
    }
    setSaving(true)
    setError(null)
    try {
      await gatewayClient.setTripDelay({
        timetableId: Number(form.timetableId),
        serviceDate: form.serviceDate.replace(/-/g, ''), // YYYY-MM-DD → YYYYMMDD
        delaySeconds: Math.round(Number(form.delayMinutes) * 60),
        reason: form.reason || null,
      })
      setForm(EMPTY_FORM)
      setTimetables([])
      await loadDelays()
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleClear(timetableId, serviceDate) {
    if (!window.confirm(t('clear_confirm'))) return
    try {
      await gatewayClient.clearTripDelay(timetableId, serviceDate)
      await loadDelays()
    } catch (e) {
      setError(e.message)
    }
  }

  const inputCls = 'w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink focus:outline-none focus:ring-1 focus:ring-accent'
  const labelCls = 'block text-xs font-semibold text-muted mb-1'

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-ink">{t('title')}</h2>
        <p className="mt-0.5 text-sm text-muted">{t('subtitle')}</p>
      </div>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      {/* Active delays table */}
      <PanelCard>
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-ink">{t('active_delays')}</h3>
          <button
            onClick={loadDelays}
            className="flex items-center gap-1.5 rounded-panel border border-border px-2.5 py-1.5 text-xs text-muted hover:bg-surface-alt transition"
          >
            <RefreshCw size={12} className={loading ? 'animate-spin' : ''} />
            {t('refresh')}
          </button>
        </div>

        {loading && <p className="text-sm text-muted">{t('loading')}</p>}
        {!loading && delays.length === 0 && (
          <p className="text-sm text-muted">{t('no_delays')}</p>
        )}
        {!loading && delays.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  {[t('col_line'), t('col_departure'), t('col_service_date'), t('col_delay'), t('col_reason'), ''].map((h) => (
                    <th key={h} className="pb-1.5 pr-4 text-left text-xs font-semibold text-muted last:pr-0">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {delays.map((d) => (
                  <tr key={`${d.timetableId}-${d.serviceDate}`} className="border-b border-border/50 last:border-0">
                    <td className="py-1.5 pr-4 font-medium text-ink">{d.lineCode ?? '—'}</td>
                    <td className="py-1.5 pr-4 text-muted">{d.lineName ?? '—'}</td>
                    <td className="py-1.5 pr-4 text-muted">
                      {d.serviceDate
                        ? `${d.serviceDate.slice(6)}-${d.serviceDate.slice(4, 6)}-${d.serviceDate.slice(0, 4)}`
                        : '—'}
                    </td>
                    <td className="py-1.5 pr-4">
                      <span className={`font-semibold ${Number(d.delaySeconds) > 0 ? 'text-red-500' : 'text-green-500'}`}>
                        {d.delaySeconds != null ? `${Math.round(d.delaySeconds / 60)} ${t('delay_unit')}` : '—'}
                      </span>
                    </td>
                    <td className="py-1.5 pr-4 text-muted max-w-[200px] truncate">{d.reason ?? '—'}</td>
                    <td className="py-1.5">
                      <button
                        onClick={() => handleClear(d.timetableId, d.serviceDate)}
                        className="flex items-center gap-1 rounded border border-border px-2 py-1 text-xs text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition"
                      >
                        <Trash2 size={11} /> {t('clear')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </PanelCard>

      {/* Set delay form */}
      <PanelCard>
        <h3 className="mb-4 text-sm font-semibold text-ink">{t('set_delay_title')}</h3>
        <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">

          <div>
            <label className={labelCls}>{t('label_line')}</label>
            <select
              className={inputCls}
              value={form.lineId}
              onChange={(e) => handleLineChange(e.target.value)}
            >
              <option value="">{t('select_line')}</option>
              {lines.map((l) => (
                <option key={l.id} value={l.id}>{l.code} — {l.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className={labelCls}>{t('label_timetable')}</label>
            <select
              className={inputCls}
              value={form.timetableId}
              onChange={(e) => setForm((f) => ({ ...f, timetableId: e.target.value }))}
              disabled={!form.lineId}
            >
              <option value="">{t('select_timetable')}</option>
              {timetables.map((tt) => (
                <option key={tt.id} value={tt.id}>
                  {tt.departureTime ?? tt.name ?? `#${tt.id}`}
                </option>
              ))}
            </select>
            {form.lineId && timetables.length === 0 && (
              <p className="mt-1 text-xs text-muted">{t('no_timetables')}</p>
            )}
          </div>

          <div>
            <label className={labelCls}>{t('label_service_date')}</label>
            <input
              type="date"
              className={inputCls}
              value={form.serviceDate}
              onChange={(e) => setForm((f) => ({ ...f, serviceDate: e.target.value }))}
            />
          </div>

          <div>
            <label className={labelCls}>{t('label_delay_minutes')}</label>
            <input
              type="number"
              min="-120"
              max="300"
              placeholder="5"
              className={inputCls}
              value={form.delayMinutes}
              onChange={(e) => setForm((f) => ({ ...f, delayMinutes: e.target.value }))}
            />
            <p className="mt-1 text-xs text-muted">{t('delay_hint')}</p>
          </div>

          <div>
            <label className={labelCls}>{t('label_reason')}</label>
            <input
              type="text"
              maxLength={300}
              placeholder={t('reason_placeholder')}
              className={inputCls}
              value={form.reason}
              onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))}
            />
          </div>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={saving}
              className="flex items-center gap-2 rounded-panel bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent/90 transition disabled:opacity-50"
            >
              <Plus size={15} />
              {saving ? t('saving') : t('btn_set_delay')}
            </button>
          </div>
        </form>
      </PanelCard>
    </div>
  )
}

export default AdminDelaysPage
