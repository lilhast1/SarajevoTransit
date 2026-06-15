import { useEffect, useState, useCallback } from 'react'
import { Plus, Trash2, RefreshCw } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { AdminPagePanel, SELECT_CLS } from '../../components/common/AdminPagePanel'
import { ErrorAlert, SuccessAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'

function todayISO() {
  return new Date().toISOString().slice(0, 10)
}

const EMPTY_FORM = {
  lineId: '',
  timetableId: '',
  serviceDate: todayISO(),
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
  const [success, setSuccess]       = useState(null)

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
    setSuccess(null)
    try {
      await gatewayClient.setTripDelay({
        timetableId: Number(form.timetableId),
        serviceDate: form.serviceDate.replace(/-/g, ''),
        delaySeconds: Math.round(Number(form.delayMinutes) * 60),
        reason: form.reason || null,
      })
      setSuccess(t('success_added'))
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

  const inputCls = `${SELECT_CLS} w-full`
  const labelCls = 'block text-xs font-semibold text-muted mb-1'

  return (
    <AdminPagePanel>
      <AdminPagePanel.Header
        title={t('title')}
        subtitle={t('subtitle')}
      />

      <ErrorAlert error={error} onDismiss={() => setError(null)} />
      <SuccessAlert message={success} onDismiss={() => setSuccess(null)} />

      <div className="rounded-panel border border-border bg-surface shadow-panel overflow-hidden">
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <h3 className="text-base font-semibold text-ink">{t('active_delays')}</h3>
          <button
            onClick={loadDelays}
            className="flex items-center gap-1.5 rounded-panel border border-border px-2.5 py-1.5 text-xs text-muted transition hover:bg-surface-alt"
          >
            <RefreshCw size={12} className={loading ? 'animate-spin' : ''} />
            {t('refresh')}
          </button>
        </div>

        {loading && <p className="p-4 text-sm text-muted">{t('loading')}</p>}
        {!loading && delays.length === 0 && (
          <p className="p-4 text-sm text-muted">{t('no_delays')}</p>
        )}
        {!loading && delays.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  {[t('col_line'), t('col_departure'), t('col_service_date'), t('col_delay'), t('col_reason'), ''].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-muted">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {delays.map((d) => (
                  <tr key={`${d.timetableId}-${d.serviceDate}`} className="border-b border-border last:border-0 transition-colors hover:bg-accent-muted/40">
                    <td className="px-4 py-3 font-medium text-ink">{d.lineCode ?? '—'}</td>
                    <td className="px-4 py-3 text-muted">{d.lineName ?? '—'}</td>
                    <td className="px-4 py-3 text-muted">
                      {d.serviceDate
                        ? `${d.serviceDate.slice(6)}-${d.serviceDate.slice(4, 6)}-${d.serviceDate.slice(0, 4)}`
                        : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`font-semibold ${Number(d.delaySeconds) > 0 ? 'text-danger' : 'text-success'}`}>
                        {d.delaySeconds != null ? `${Math.round(d.delaySeconds / 60)} ${t('delay_unit')}` : '—'}
                      </span>
                    </td>
                    <td className="max-w-[200px] truncate px-4 py-3 text-muted">{d.reason ?? '—'}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleClear(d.timetableId, d.serviceDate)}
                        className="flex items-center gap-1 rounded-panel border border-danger-soft px-2.5 py-1 text-xs font-medium text-danger transition hover:bg-danger-soft/20"
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
      </div>

      <div className="rounded-panel border border-border bg-surface shadow-panel p-4">
        <h3 className="mb-4 text-base font-semibold text-ink">{t('set_delay_title')}</h3>
        <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <label className={labelCls}>{t('label_line')}</label>
            <select className={inputCls} value={form.lineId} onChange={(e) => handleLineChange(e.target.value)}>
              <option value="">{t('select_line')}</option>
              {lines.map((l) => <option key={l.id} value={l.id}>{l.code} — {l.name}</option>)}
            </select>
          </div>
          <div>
            <label className={labelCls}>{t('label_timetable')}</label>
            <select className={inputCls} value={form.timetableId}
              onChange={(e) => setForm((f) => ({ ...f, timetableId: e.target.value }))} disabled={!form.lineId}>
              <option value="">{t('select_timetable')}</option>
              {timetables.map((tt) => (
                <option key={tt.id} value={tt.id}>{tt.departureTime ?? tt.name ?? `#${tt.id}`}</option>
              ))}
            </select>
            {form.lineId && timetables.length === 0 && (
              <p className="mt-1 text-xs text-muted">{t('no_timetables')}</p>
            )}
          </div>
          <div>
            <label className={labelCls}>{t('label_service_date')}</label>
            <input type="date" className={inputCls} value={form.serviceDate}
              onChange={(e) => setForm((f) => ({ ...f, serviceDate: e.target.value }))} />
          </div>
          <div>
            <label className={labelCls}>{t('label_delay_minutes')}</label>
            <input type="number" min="-120" max="300" placeholder="5" className={inputCls}
              value={form.delayMinutes} onChange={(e) => setForm((f) => ({ ...f, delayMinutes: e.target.value }))} />
            <p className="mt-1 text-xs text-muted">{t('delay_hint')}</p>
          </div>
          <div>
            <label className={labelCls}>{t('label_reason')}</label>
            <input type="text" maxLength={300} placeholder={t('reason_placeholder')} className={inputCls}
              value={form.reason} onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} />
          </div>
          <div className="flex items-end">
            <button type="submit" disabled={saving}
              className="flex items-center gap-2 rounded-panel bg-accent px-4 py-2 text-sm font-medium text-white transition hover:bg-accent-strong disabled:opacity-50">
              <Plus size={15} />
              {saving ? t('saving') : t('btn_set_delay')}
            </button>
          </div>
        </form>
      </div>
    </AdminPagePanel>
  )
}

export default AdminDelaysPage
