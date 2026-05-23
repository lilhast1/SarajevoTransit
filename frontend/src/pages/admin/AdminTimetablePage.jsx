import { useEffect, useState, useCallback } from 'react'
import { Plus, X } from 'lucide-react'
import { DataTable } from '../../components/admin/DataTable'
import { PanelCard } from '../../components/common/PanelCard'
import { ErrorAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'
import { VEHICLE_TYPE_META_BY_ID } from '../../constants/vehicleColors'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)

const DAY_LABELS = { 1: 'Mon', 2: 'Tue', 3: 'Wed', 4: 'Thu', 5: 'Fri', 6: 'Sat', 7: 'Sun' }
const ALL_DAYS = [1, 2, 3, 4, 5, 6, 7]
const DAY_TYPES = [
  { key: 'weekday', label: 'Weekday', days: [1, 2, 3, 4, 5] },
  { key: 'saturday', label: 'Saturday', days: [6] },
  { key: 'sunday', label: 'Sunday', days: [7] },
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
  // filter state (table view)
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  const [lines, setLines] = useState([])
  const [lineId, setLineId] = useState('')
  const [directions, setDirections] = useState([])
  const [directionId, setDirectionId] = useState('')
  const [dayType, setDayType] = useState('weekday')
  const [timetables, setTimetables] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // form state
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [formDirections, setFormDirections] = useState([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    const q = vehicleTypeId ? `?vehicleTypeId=${vehicleTypeId}` : ''
    setLineId('')
    setDirectionId('')
    setDirections([])
    gatewayClient.getLines(q).then(setLines).catch(() => {})
  }, [vehicleTypeId])

  // load directions for the table filter
  useEffect(() => {
    if (!lineId) { setDirections([]); setDirectionId(''); return }
    gatewayClient.getDirections(`?lineId=${lineId}&activeOnly=true`)
      .then(setDirections)
      .catch(() => setDirections([]))
    setDirectionId('')
  }, [lineId])

  // load directions for the form's line selector
  useEffect(() => {
    if (!form.formLineId) { setFormDirections([]); setForm((f) => ({ ...f, formDirectionId: '' })); return }
    gatewayClient.getDirections(`?lineId=${form.formLineId}&activeOnly=true`)
      .then(setFormDirections)
      .catch(() => setFormDirections([]))
    setForm((f) => ({ ...f, formDirectionId: '' }))
  }, [form.formLineId])

  const load = useCallback(async () => {
    if (!lineId || !directionId) return
    setLoading(true)
    setError(null)
    try {
      const query = new URLSearchParams({ lineId, directionId, activeOnly: false })
      const res = await gatewayClient.getTimetables(`?${query}`)
      const dayNums = DAY_TYPES.find((d) => d.key === dayType)?.days ?? []
      const filtered = res.filter((t) => (t.daysOfWeek ?? []).some((d) => dayNums.includes(d)))
      setTimetables(filtered.sort((a, b) => String(a.departureTime).localeCompare(String(b.departureTime))))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [lineId, directionId, dayType])

  useEffect(() => { load() }, [load])

  function openForm() {
    setForm({ ...EMPTY_FORM, formLineId: lineId, formDirectionId: directionId })
    if (lineId) {
      gatewayClient.getDirections(`?lineId=${lineId}&activeOnly=true`)
        .then(setFormDirections)
        .catch(() => setFormDirections([]))
    }
    setFormOpen(true)
  }

  async function handleDelete(id) {
    if (!window.confirm('Delete this timetable entry?')) return
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
    { key: 'departureTime', label: 'Departure', render: (r) => String(r.departureTime ?? '—').slice(0, 5) },
    { key: 'name', label: 'Name', render: (r) => r.name ?? '—' },
    {
      key: 'daysOfWeek', label: 'Days', render: (r) =>
        (r.daysOfWeek ?? []).map((d) => DAY_LABELS[d]).join(', ')
    },
    {
      key: 'isActive', label: 'Active', render: (r) => (
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${r.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
          {r.isActive ? 'Yes' : 'No'}
        </span>
      )
    },
    {
      key: 'actions', label: 'Actions', render: (r) => (
        <button
          type="button"
          onClick={() => handleDelete(r.id)}
          className="rounded border border-red-300 px-2 py-0.5 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-950"
        >
          Delete
        </button>
      )
    },
  ]

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-ink">Timetables</h2>
          <p className="mt-1 text-sm text-muted">View, add, and remove timetable entries per line and direction.</p>
        </div>
        <button
          type="button"
          onClick={openForm}
          className="flex items-center gap-1.5 rounded-panel border border-accent bg-accent px-3 py-2 text-sm font-medium text-white"
        >
          <Plus size={15} /> New Entry
        </button>
      </div>

      {/* vehicle type filter */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm text-muted">Type:</span>
        {[{ id: '', label: 'All' }, ...VEHICLE_TYPES].map((vt) => (
          <button
            key={vt.id}
            type="button"
            onClick={() => setVehicleTypeId(vt.id)}
            className={`rounded-panel border px-3 py-1 text-xs font-medium transition ${
              vehicleTypeId === vt.id
                ? 'border-accent bg-accent text-white'
                : 'border-border text-muted hover:bg-surface-alt'
            }`}
          >
            {vt.label}
          </button>
        ))}
      </div>

      {/* table filters */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted">Line:</span>
          <select
            value={lineId}
            onChange={(e) => setLineId(e.target.value)}
            className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
          >
            <option value="">— Select —</option>
            {lines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted">Direction:</span>
          <select
            value={directionId}
            onChange={(e) => setDirectionId(e.target.value)}
            disabled={!lineId}
            className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink disabled:opacity-50"
          >
            <option value="">— Select —</option>
            {directions.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </div>
      </div>

      <div className="flex gap-2">
        {DAY_TYPES.map((dt) => (
          <button
            key={dt.key}
            type="button"
            onClick={() => setDayType(dt.key)}
            className={`rounded-panel border px-3 py-1 text-xs font-medium transition ${
              dayType === dt.key
                ? 'border-accent bg-accent text-white'
                : 'border-border text-muted hover:bg-surface-alt'
            }`}
          >
            {dt.label}
          </button>
        ))}
      </div>

      {formOpen && (
        <PanelCard tone="soft">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-ink">New Timetable Entry</h3>
            <button type="button" onClick={() => setFormOpen(false)} className="text-muted hover:text-ink"><X size={16} /></button>
          </div>
          <form onSubmit={handleCreate} className="grid gap-3 sm:grid-cols-2">

            {/* line + direction */}
            <label className="block">
              <span className="text-xs text-muted">Line</span>
              <select
                required
                value={form.formLineId}
                onChange={(e) => setForm((f) => ({ ...f, formLineId: e.target.value }))}
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
              >
                <option value="">— Select —</option>
                {lines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
              </select>
            </label>
            <label className="block">
              <span className="text-xs text-muted">Direction</span>
              <select
                required
                value={form.formDirectionId}
                onChange={(e) => setForm((f) => ({ ...f, formDirectionId: e.target.value }))}
                disabled={!form.formLineId}
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink disabled:opacity-50"
              >
                <option value="">— Select —</option>
                {formDirections.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
            </label>

            {/* 24h time picker */}
            <div className="block">
              <span className="text-xs text-muted">Departure Time (24h)</span>
              <div className="mt-1 flex items-center gap-1">
                <select
                  value={form.hour}
                  onChange={(e) => setForm((f) => ({ ...f, hour: e.target.value }))}
                  className="rounded-panel border border-border bg-surface px-2 py-1.5 text-sm text-ink"
                >
                  {HOURS.map((h) => <option key={h} value={h}>{h}</option>)}
                </select>
                <span className="text-sm font-semibold text-muted">:</span>
                <select
                  value={form.minute}
                  onChange={(e) => setForm((f) => ({ ...f, minute: e.target.value }))}
                  className="rounded-panel border border-border bg-surface px-2 py-1.5 text-sm text-ink"
                >
                  {MINUTES.map((m) => <option key={m} value={m}>{m}</option>)}
                </select>
              </div>
            </div>

            {/* name */}
            <label className="block">
              <span className="text-xs text-muted">Name (optional)</span>
              <input
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
              />
            </label>

            {/* days of week */}
            <div className="sm:col-span-2">
              <span className="text-xs text-muted">Days of Week</span>
              <div className="mt-1 flex flex-wrap gap-2">
                {ALL_DAYS.map((d) => (
                  <button
                    key={d}
                    type="button"
                    onClick={() => toggleDay(d)}
                    className={`rounded border px-2.5 py-1 text-xs font-medium transition ${
                      form.daysOfWeek.includes(d)
                        ? 'border-accent bg-accent text-white'
                        : 'border-border text-muted hover:bg-surface-alt'
                    }`}
                  >
                    {DAY_LABELS[d]}
                  </button>
                ))}
              </div>
            </div>

            {/* flags */}
            <div className="flex flex-wrap gap-4 sm:col-span-2">
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.ridesOnHolidays} onChange={(e) => setForm((f) => ({ ...f, ridesOnHolidays: e.target.checked }))} />
                <span className="text-sm text-ink">Rides on holidays</span>
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.receivesPassengers} onChange={(e) => setForm((f) => ({ ...f, receivesPassengers: e.target.checked }))} />
                <span className="text-sm text-ink">Receives passengers</span>
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={form.isActive} onChange={(e) => setForm((f) => ({ ...f, isActive: e.target.checked }))} />
                <span className="text-sm text-ink">Active</span>
              </label>
            </div>

            <div className="flex gap-2 sm:col-span-2">
              <button
                type="submit"
                disabled={saving || form.daysOfWeek.length === 0 || !form.formLineId || !form.formDirectionId}
                className="rounded-panel border border-accent bg-accent px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60"
              >
                {saving ? 'Saving…' : 'Save'}
              </button>
              <button type="button" onClick={() => setFormOpen(false)} className="rounded-panel border border-border px-4 py-1.5 text-sm text-ink">
                Cancel
              </button>
            </div>
          </form>
        </PanelCard>
      )}

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      {lineId && directionId ? (
        <DataTable
          columns={columns}
          rows={timetables}
          page={0}
          totalPages={1}
          onPageChange={() => {}}
          loading={loading}
        />
      ) : (
        <p className="text-sm text-muted">Select a line and direction to load timetable entries.</p>
      )}
    </div>
  )
}

export default AdminTimetablePage
