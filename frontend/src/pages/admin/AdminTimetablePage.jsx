import { useEffect, useState, useCallback, useMemo } from 'react'
import { Plus, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { DataTable } from '../../components/admin/DataTable'
import { AdminPagePanel, SELECT_CLS } from '../../components/common/AdminPagePanel'
import { StatusBadge } from '../../components/common/StatusBadge'
import { ErrorAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'
import { VEHICLE_TYPE_META_BY_ID } from '../../constants/vehicleColors'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)
const DAY_KEYS = { 1: 'day_mon', 2: 'day_tue', 3: 'day_wed', 4: 'day_thu', 5: 'day_fri', 6: 'day_sat', 7: 'day_sun' }
const ALL_DAYS = [1, 2, 3, 4, 5, 6, 7]
const DAY_TYPES = [
  { key: 'weekday', days: [1, 2, 3, 4, 5] },
  { key: 'saturday', days: [6] },
  { key: 'sunday', days: [7] },
]
const HOURS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'))
const MINUTES = ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55']

const EMPTY_FORM = {
  formLineId: '',
  formDirectionId: '',
  hour: '06',
  minute: '00',
  name: '',
  daysOfWeek: [1, 2, 3, 4, 5],
  ridesOnHolidays: false,
  receivesPassengers: true,
  isActive: true,
}

export function AdminTimetablePage() {
  const { t } = useTranslation('admin-timetable')
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  const [lines, setLines] = useState([])
  const [dayType, setDayType] = useState('weekday')
  const [timetables, setTimetables] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')

  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [formDirections, setFormDirections] = useState([])
  const [saving, setSaving] = useState(false)
  const [formDirectionError, setFormDirectionError] = useState(null)

  useEffect(() => {
    let active = true
    const q = vehicleTypeId ? `?vehicleTypeId=${vehicleTypeId}` : ''

    gatewayClient.getLines(q)
      .then((response) => { if (active) setLines(response) })
      .catch(() => { if (active) setLines([]) })

    return () => { active = false }
  }, [vehicleTypeId])

  useEffect(() => {
    if (!form.formLineId) { setFormDirections([]); setForm((f) => ({ ...f, formDirectionId: '' })); return }

    let active = true
    const loadFormDirections = async () => {
      try {
        setFormDirectionError(null)
        const response = await gatewayClient.getDirections(`?lineId=${form.formLineId}&activeOnly=true`)
        if (active) setFormDirections(response)
      } catch (err) {
        if (active) {
          setFormDirections([])
          setFormDirectionError(err.message || t('directions_load_failed'))
        }
      }
    }

    loadFormDirections()
    setForm((f) => ({ ...f, formDirectionId: '' }))

    return () => { active = false }
  }, [form.formLineId, t])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await gatewayClient.getTimetables('?activeOnly=false')
      const dayNums = DAY_TYPES.find((d) => d.key === dayType)?.days ?? []
      const filtered = res.filter((tt) => (tt.daysOfWeek ?? []).some((d) => dayNums.includes(d)))
      setTimetables(filtered.sort((a, b) => String(a.departureTime).localeCompare(String(b.departureTime))))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [dayType])

  useEffect(() => { load() }, [load])

  const filteredTimetables = useMemo(() => {
    const q = search.toLowerCase().trim()
    if (!q) return timetables
    return timetables.filter((tt) =>
      String(tt.departureTime ?? '').toLowerCase().includes(q) ||
      (tt.name ?? '').toLowerCase().includes(q)
    )
  }, [timetables, search])

  function openForm() {
    setForm({ ...EMPTY_FORM })
    setFormOpen(true)
  }

  async function handleDelete(id) {
    if (!window.confirm(t('delete_confirm'))) return
    try {
      await gatewayClient.deleteTimetable(id)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    if (!form.formLineId || !form.formDirectionId) return
    setSaving(true)
    setError(null)
    try {
      await gatewayClient.createTimetable({
        lineId: Number(form.formLineId),
        directionId: Number(form.formDirectionId),
        departureTime: `${form.hour}:${form.minute}:00`,
        name: form.name || null,
        daysOfWeek: form.daysOfWeek,
        ridesOnHolidays: form.ridesOnHolidays,
        receivesPassengers: form.receivesPassengers,
        isActive: form.isActive,
      })
      setFormOpen(false)
      setForm(EMPTY_FORM)
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  function toggleDay(day) {
    setForm((f) => ({
      ...f,
      daysOfWeek: f.daysOfWeek.includes(day)
        ? f.daysOfWeek.filter((d) => d !== day)
        : [...f.daysOfWeek, day],
    }))
  }

  const columns = [
    { key: 'departureTime', label: t('col_departure'), render: (r) => String(r.departureTime ?? '—').slice(0, 5) },
    { key: 'name', label: t('col_name'), render: (r) => r.name ?? '—' },
    {
      key: 'daysOfWeek', label: t('col_days'), render: (r) =>
        (r.daysOfWeek ?? []).map((d) => t(DAY_KEYS[d])).join(', ')
    },
    {
      key: 'isActive', label: t('col_active'), render: (r) => <StatusBadge status={r.isActive ? 'ACTIVE' : 'INACTIVE'} />
    },
    {
      key: 'actions', label: t('col_actions'), render: (r) => (
        <button type="button" onClick={() => handleDelete(r.id)}
          className="rounded-panel border border-danger-soft px-2.5 py-1 text-xs font-medium text-danger transition hover:bg-danger-soft/20">
          {t('delete')}
        </button>
      )
    },
  ]

  const typePillOptions = [{ id: '', label: t('all') }, ...VEHICLE_TYPES]

  return (
    <AdminPagePanel>
      <AdminPagePanel.Header
        title={t('title')}
        subtitle={t('subtitle')}
        actions={
          <button type="button" onClick={openForm}
            className="flex items-center gap-1.5 rounded-panel bg-accent px-3 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-accent-strong">
            <Plus size={15} /> {t('new_entry')}
          </button>
        }
      />

      <AdminPagePanel.Toolbar>
        <input
          type="text"
          placeholder={t('search_placeholder')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none max-w-[240px]"
        />
        <AdminPagePanel.ToolbarDivider />
        <AdminPagePanel.ToolbarGroup label={t('type_label')}>
          <div className="flex flex-wrap gap-1">
            {typePillOptions.map((vt) => (
              <button key={vt.id} type="button" onClick={() => setVehicleTypeId(vt.id)}
                className={`rounded-panel border px-2.5 py-1 text-xs font-medium transition ${
                  vehicleTypeId === vt.id
                    ? 'border-accent bg-accent text-white shadow-sm'
                    : 'border-border text-muted hover:border-accent-subtle hover:text-ink'
                }`}>
                {vt.label}
              </button>
            ))}
          </div>
        </AdminPagePanel.ToolbarGroup>
        <AdminPagePanel.ToolbarDivider />
        <div className="flex items-center gap-1 rounded-panel border border-border bg-surface p-0.5">
          {DAY_TYPES.map((dt) => (
            <button key={dt.key} type="button" onClick={() => setDayType(dt.key)}
              className={`rounded-panel px-2.5 py-1 text-xs font-medium transition ${
                dayType === dt.key ? 'bg-accent text-white shadow-sm' : 'text-muted hover:text-ink'
              }`}>
              {t(dt.key)}
            </button>
          ))}
        </div>
      </AdminPagePanel.Toolbar>

      {formOpen && (
        <div className="rounded-panel border border-border bg-surface-soft p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-ink">{t('new_entry_title')}</h3>
            <button type="button" onClick={() => setFormOpen(false)} className="text-muted hover:text-ink"><X size={16} /></button>
          </div>
          <form onSubmit={handleCreate} className="grid gap-3 sm:grid-cols-2">
            {formDirectionError && <ErrorAlert error={formDirectionError} onDismiss={() => setFormDirectionError(null)} />}

            <label className="block">
              <span className="text-xs text-muted">{t('field_line')}</span>
              <select required value={form.formLineId}
                onChange={(e) => setForm((f) => ({ ...f, formLineId: e.target.value }))}
                className={`${SELECT_CLS} mt-1 w-full`}>
                <option value="">{t('select_placeholder')}</option>
                {lines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="text-xs text-muted">{t('field_direction')}</span>
              <select required value={form.formDirectionId}
                onChange={(e) => setForm((f) => ({ ...f, formDirectionId: e.target.value }))}
                disabled={!form.formLineId}
                className={`${SELECT_CLS} mt-1 w-full disabled:opacity-50`}>
                <option value="">{t('select_placeholder')}</option>
                {formDirections.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
            </label>

            <div className="block">
              <span className="text-xs text-muted">{t('field_departure')}</span>
              <div className="mt-1 flex items-center gap-1">
                <select value={form.hour}
                  onChange={(e) => setForm((f) => ({ ...f, hour: e.target.value }))} className={SELECT_CLS}>
                  {HOURS.map((h) => <option key={h} value={h}>{h}</option>)}
                </select>
                <span className="text-sm font-semibold text-muted">:</span>
                <select value={form.minute}
                  onChange={(e) => setForm((f) => ({ ...f, minute: e.target.value }))} className={SELECT_CLS}>
                  {MINUTES.map((m) => <option key={m} value={m}>{m}</option>)}
                </select>
              </div>
            </div>

            <label className="block">
              <span className="text-xs text-muted">{t('field_name')}</span>
              <input value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                className={`${SELECT_CLS} mt-1 w-full`} />
            </label>

            <div className="sm:col-span-2">
              <span className="text-xs text-muted">{t('field_days')}</span>
              <div className="mt-1 flex flex-wrap gap-2">
                {ALL_DAYS.map((d) => (
                  <button key={d} type="button" onClick={() => toggleDay(d)}
                    className={`rounded-panel border px-2.5 py-1 text-xs font-medium transition ${
                      form.daysOfWeek.includes(d)
                        ? 'border-accent bg-accent text-white shadow-sm'
                        : 'border-border text-muted hover:border-accent-subtle hover:text-ink'
                    }`}>
                    {t(DAY_KEYS[d])}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex flex-wrap gap-4 sm:col-span-2">
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.ridesOnHolidays}
                  onChange={(e) => setForm((f) => ({ ...f, ridesOnHolidays: e.target.checked }))} />
                <span className="text-sm text-ink">{t('field_holidays')}</span>
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.receivesPassengers}
                  onChange={(e) => setForm((f) => ({ ...f, receivesPassengers: e.target.checked }))} />
                <span className="text-sm text-ink">{t('field_receives')}</span>
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.isActive}
                  onChange={(e) => setForm((f) => ({ ...f, isActive: e.target.checked }))} />
                <span className="text-sm text-ink">{t('field_active')}</span>
              </label>
            </div>

            <div className="flex gap-2 sm:col-span-2">
              <button type="submit"
                disabled={saving || form.daysOfWeek.length === 0 || !form.formLineId || !form.formDirectionId}
                className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white shadow-sm transition hover:bg-accent-strong disabled:opacity-60">
                {saving ? t('saving') : t('save')}
              </button>
              <button type="button" onClick={() => setFormOpen(false)}
                className="rounded-panel border border-border px-4 py-1.5 text-sm text-ink transition hover:bg-surface-alt">
                {t('cancel')}
              </button>
            </div>
          </form>
        </div>
      )}

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      <DataTable
        columns={columns}
        rows={filteredTimetables}
        page={0}
        totalPages={1}
        onPageChange={() => {}}
        loading={loading}
      />
    </AdminPagePanel>
  )
}

export default AdminTimetablePage
