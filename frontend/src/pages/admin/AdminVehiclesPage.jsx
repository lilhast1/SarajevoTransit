import {
  AlertCircle, AlertTriangle, Calendar, CheckCircle, ChevronDown, ChevronUp,
  Edit, Plus, RefreshCw, Truck, Wrench, X
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { gatewayClient } from '../../services/gatewayClient'
import { transitApi } from '../../services/transitApi'
import { VS_TYPE_MAP, STATUS_STYLES, STATUS_LABELS } from '../VehiclesPage'
import { TransitMap } from '../../components/map/TransitMap'

const TABS = ['fleet', 'maintenance']
const VEHICLE_TYPES = ['BUS', 'TRAM', 'TROLLEY', 'MINIBUS']
const VEHICLE_STATUSES = ['OPERATIONAL', 'IN_MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED']

const VEHICLE_TO_LINE_TYPE = {
  BUS: 'bus',
  TRAM: 'tram',
  TROLLEY: 'trolleybus',
  MINIBUS: 'minibus',
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

function EditVehicleModal({ vehicle, onClose, onSaved }) {
  const [form, setForm] = useState({
    registrationNumber: vehicle.registrationNumber || '',
    type: vehicle.type || 'BUS',
    capacity: vehicle.capacity || '',
    status: vehicle.status || 'OPERATIONAL',
    manufactureDate: vehicle.manufactureDate || '',
    serviceCycleIntervalDays: vehicle.serviceCycleIntervalDays || '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  function setField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const payload = {
        registrationNumber: form.registrationNumber.trim(),
        type: form.type,
        capacity: parseInt(form.capacity, 10) || null,
        status: form.status,
        manufactureDate: form.manufactureDate || null,
        serviceCycleIntervalDays: form.serviceCycleIntervalDays ? parseInt(form.serviceCycleIntervalDays, 10) : null,
      }
      const updated = await gatewayClient.updateVehicle(vehicle.id, payload)
      onSaved(updated)
    } catch (err) {
      setError(err.message || 'Failed to update vehicle')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <button type="button" className="fixed inset-0 z-[1100] bg-black/40" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-[1200] w-[min(520px,92vw)] -translate-x-1/2 -translate-y-1/2 rounded-panel border border-border bg-surface p-5 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-base font-semibold text-ink">Edit Vehicle — {vehicle.registrationNumber}</h3>
          <button type="button" onClick={onClose} className="rounded-panel border border-border p-1.5 text-muted hover:bg-surface-alt">
            <X size={15} />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Registration</span>
            <input required type="text" value={form.registrationNumber}
              onChange={(e) => setField('registrationNumber', e.target.value)}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
          </label>
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Type</span>
              <select value={form.type} onChange={(e) => setField('type', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none">
                {VEHICLE_TYPES.map((t) => <option key={t} value={t}>{VS_TYPE_MAP[t]?.label || t}</option>)}
              </select>
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Capacity</span>
              <input type="number" min="1" value={form.capacity}
                onChange={(e) => setField('capacity', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
            </label>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Status</span>
              <select value={form.status} onChange={(e) => setField('status', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none">
                {VEHICLE_STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s] || s}</option>)}
              </select>
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Manufacture Date</span>
              <input type="date" value={form.manufactureDate}
                onChange={(e) => setField('manufactureDate', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
            </label>
          </div>
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Service cycle interval (days) — leave blank to disable</span>
            <input type="number" min="1" value={form.serviceCycleIntervalDays}
              onChange={(e) => setField('serviceCycleIntervalDays', e.target.value)}
              placeholder="e.g. 90"
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
          </label>
          {error && (
            <div className="flex items-center gap-2 rounded-panel border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              <AlertCircle size={13} />{error}
            </div>
          )}
          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={onClose} disabled={saving}
              className="rounded-panel border border-border px-4 py-1.5 text-sm text-muted hover:bg-surface-alt disabled:opacity-50">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50">
              {saving ? 'Saving…' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </>
  )
}

function AssignLineModal({ vehicle, lines, vehicles, onClose, onSaved }) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [jpPositions, setJpPositions] = useState([])
  const [pickedJpVehicle, setPickedJpVehicle] = useState(null)

  const typeLabel = VS_TYPE_MAP[vehicle.type]?.label || vehicle.type

  useEffect(() => {
    const typeId = VS_TYPE_MAP[vehicle.type]?.typeId
    if (!typeId) return
    transitApi.getVehiclePositions([typeId])
      .then((positions) => {
        setJpPositions(positions || [])
        if (vehicle.internalId) {
          const existing = (positions || []).find((p) => String(p.id) === String(vehicle.internalId))
          if (existing) setPickedJpVehicle(existing)
        }
      })
      .catch(() => setJpPositions([]))
  }, [vehicle.type, vehicle.internalId])

  const takenJpIds = useMemo(
    () => new Set(
      vehicles
        .filter((v) => v.id !== vehicle.id && v.internalId)
        .map((v) => String(v.internalId))
    ),
    [vehicles, vehicle.id]
  )

  const normCode = (c) => String(c ?? '').trim().toLowerCase().replace(/^0+/, '')

  // Line to assign is derived from the picked JP vehicle's lineCode, not from any dropdown
  const detectedLine = useMemo(() => {
    if (!pickedJpVehicle?.lineCode) return null
    return lines.find((l) => normCode(l.code) === normCode(pickedJpVehicle.lineCode)) || null
  }, [pickedJpVehicle, lines])

  const mapPositions = useMemo(() => jpPositions.map((p) => {
    const idStr = String(p.id)
    const isPicked = pickedJpVehicle && String(pickedJpVehicle.id) === idStr
    const isTaken = takenJpIds.has(idStr)
    return { ...p, color: isPicked ? '#f59e0b' : isTaken ? '#e2e8f0' : '#94a3b8', isTaken }
  }), [jpPositions, pickedJpVehicle, takenJpIds])

  function handleVehicleClick(jpVehicle) {
    if (takenJpIds.has(String(jpVehicle.id))) return
    setPickedJpVehicle((prev) =>
      prev && String(prev.id) === String(jpVehicle.id) ? null : jpVehicle
    )
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const payload = {
        lineId: detectedLine?.id ?? null,
        lineCode: detectedLine?.code ?? (pickedJpVehicle?.lineCode ? String(pickedJpVehicle.lineCode) : null),
        lineName: detectedLine?.name ?? null,
      }
      const updated = await gatewayClient.assignVehicleToLine(vehicle.id, payload)
      const withLine = {
        ...updated,
        assignedLineName: detectedLine?.name ?? updated.assignedLineName,
        assignedLineCode: payload.lineCode ?? updated.assignedLineCode,
        assignedLineId:   detectedLine?.id ?? updated.assignedLineId,
      }
      if (pickedJpVehicle && String(pickedJpVehicle.id) !== (vehicle.internalId || '')) {
        await gatewayClient.updateVehicle(vehicle.id, { internalId: String(pickedJpVehicle.id) })
        onSaved({ ...withLine, internalId: String(pickedJpVehicle.id) })
      } else if (!pickedJpVehicle && vehicle.internalId) {
        await gatewayClient.updateVehicle(vehicle.id, { internalId: '' })
        onSaved({ ...withLine, internalId: null })
      } else {
        onSaved(withLine)
      }
    } catch (err) {
      setError(err.message || 'Failed to assign line')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <button type="button" className="fixed inset-0 z-[1100] bg-black/40" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-[1200] w-[min(580px,92vw)] -translate-x-1/2 -translate-y-1/2 rounded-panel border border-border bg-surface p-5 shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold text-ink">Link to Live Vehicle — {vehicle.registrationNumber}</h3>
          <button type="button" onClick={onClose} className="rounded-panel border border-border p-1.5 text-muted hover:bg-surface-alt">
            <X size={15} />
          </button>
        </div>

        <p className="mb-3 text-xs text-muted">
          Click a <span className="font-medium text-ink">{typeLabel.toLowerCase()}</span> on the map — its line will be detected and saved to this vehicle.
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          {jpPositions.length > 0 ? (
            <TransitMap
              vehicles={mapPositions}
              onVehicleClick={handleVehicleClick}
              selectedVehicleId={pickedJpVehicle?.id}
              className="h-64 rounded-panel"
            />
          ) : (
            <div className="flex h-64 items-center justify-center rounded-panel border border-border bg-surface-alt text-xs text-muted">
              Loading live {typeLabel.toLowerCase()} positions…
            </div>
          )}

          <div className="flex flex-wrap items-center gap-3 text-[10px] text-muted">
            <span className="flex items-center gap-1"><span className="inline-block h-2 w-2 rounded-full bg-slate-400" />Available</span>
            <span className="flex items-center gap-1"><span className="inline-block h-2 w-2 rounded-full bg-slate-200 border border-slate-300" />Already mapped</span>
            <span className="flex items-center gap-1"><span className="inline-block h-2 w-2 rounded-full bg-amber-400" />Selected</span>
          </div>

          {pickedJpVehicle ? (
            <div className="flex flex-col gap-1.5 rounded-panel border border-amber-200 bg-amber-50 px-3 py-2 text-xs dark:border-amber-900 dark:bg-amber-950/20">
              <div className="flex items-center justify-between">
                <span className="font-semibold text-ink">
                  #{pickedJpVehicle.id}
                  {pickedJpVehicle.name ? ` — ${pickedJpVehicle.name}` : ''}
                  {pickedJpVehicle.speed != null ? ` · ${Math.round(pickedJpVehicle.speed)} km/h` : ''}
                </span>
                <button type="button" onClick={() => setPickedJpVehicle(null)} className="text-muted hover:text-ink underline">Clear</button>
              </div>
              {detectedLine ? (
                <span className="text-emerald-700 dark:text-emerald-400">
                  Line: <span className="font-medium">{detectedLine.code} — {detectedLine.name}</span>
                </span>
              ) : pickedJpVehicle.lineCode ? (
                <span className="text-amber-700 dark:text-amber-400">
                  Line code <span className="font-medium">{pickedJpVehicle.lineCode}</span> not in database — will save code only
                </span>
              ) : (
                <span className="text-muted">No line code from live feed — line will be cleared</span>
              )}
            </div>
          ) : (
            <p className="text-[10px] text-muted italic">
              No vehicle selected.{vehicle.internalId ? ' Saving without a selection will unlink the current live vehicle.' : ''}
            </p>
          )}

          {error && <div className="text-xs text-red-600">{error}</div>}
          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={onClose} disabled={saving}
              className="rounded-panel border border-border px-4 py-1.5 text-sm text-muted hover:bg-surface-alt disabled:opacity-50">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50">
              {saving ? 'Saving…' : pickedJpVehicle ? 'Link & Assign Line' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </>
  )
}

function AssignDriverModal({ vehicle, drivers, onClose, onSaved }) {
  const today = new Date().toISOString().split('T')[0]
  const [driverId, setDriverId] = useState('')
  const [assignmentDate, setAssignmentDate] = useState(today)
  const [assignWeek, setAssignWeek] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  const compatibleDrivers = drivers.filter(
    (d) => !d.licensedVehicleTypes?.length || d.licensedVehicleTypes.includes(vehicle.type)
  )

  function getWeekDates(dateStr) {
    const d = new Date(dateStr)
    const dow = d.getDay()
    const monday = new Date(d)
    monday.setDate(d.getDate() - (dow === 0 ? 6 : dow - 1))
    return Array.from({ length: 7 }, (_, i) => {
      const dt = new Date(monday)
      dt.setDate(monday.getDate() + i)
      return dt.toISOString().split('T')[0]
    })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!driverId) { setError('Select a driver'); return }
    const allDates = assignWeek ? getWeekDates(assignmentDate) : [assignmentDate]
    const validDates = allDates.filter((d) => d >= today)
    if (validDates.length === 0) { setError('No future dates in selection'); return }

    setSaving(true)
    setError(null)
    try {
      for (const date of validDates) {
        const existing = await gatewayClient.getAssignmentsByDate(date)
        const clash = existing.find(
          (a) => a.driverId === parseInt(driverId, 10) &&
                 a.lineCode === vehicle.assignedLineCode &&
                 a.vehicleId !== vehicle.id
        )
        if (clash) {
          setError(`Driver already assigned to line ${vehicle.assignedLineCode || '—'} on ${date}`)
          setSaving(false)
          return
        }
      }
      for (const date of validDates) {
        await gatewayClient.createAssignment({
          driverId: parseInt(driverId, 10),
          vehicleId: vehicle.id,
          lineId: vehicle.assignedLineId || null,
          lineCode: vehicle.assignedLineCode || null,
          lineName: vehicle.assignedLineName || null,
          assignmentDate: date,
        })
      }
      const updated = await gatewayClient.assignVehicleToDriver(vehicle.id, parseInt(driverId, 10))
      onSaved(updated)
    } catch (err) {
      setError(err.message || 'Failed to create assignment')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <button type="button" className="fixed inset-0 z-[1100] bg-black/40" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-[1200] w-[min(440px,92vw)] -translate-x-1/2 -translate-y-1/2 rounded-panel border border-border bg-surface p-5 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-base font-semibold text-ink">Schedule Driver — {vehicle.registrationNumber}</h3>
          <button type="button" onClick={onClose} className="rounded-panel border border-border p-1.5 text-muted hover:bg-surface-alt">
            <X size={15} />
          </button>
        </div>
        {vehicle.assignedLineCode && (
          <p className="mb-3 text-xs text-muted">
            Line: <span className="font-medium text-ink">{vehicle.assignedLineCode}{vehicle.assignedLineName ? ` — ${vehicle.assignedLineName}` : ''}</span>
          </p>
        )}
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">
              Driver (licensed for {VS_TYPE_MAP[vehicle.type]?.label || vehicle.type})
            </span>
            <select value={driverId} onChange={(e) => setDriverId(e.target.value)}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none">
              <option value="">Select driver…</option>
              {compatibleDrivers.map((d) => (
                <option key={d.id} value={d.id}>{d.fullName || `Driver #${d.id}`}</option>
              ))}
            </select>
            {compatibleDrivers.length === 0 && (
              <p className="text-xs text-amber-600">No drivers licensed for {VS_TYPE_MAP[vehicle.type]?.label || vehicle.type}.</p>
            )}
          </label>
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Assignment Date</span>
            <input required type="date" value={assignmentDate} min={today}
              onChange={(e) => setAssignmentDate(e.target.value)}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
          </label>
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={assignWeek} onChange={(e) => setAssignWeek(e.target.checked)}
              className="rounded border-border accent-accent" />
            <span className="text-xs text-ink">Assign for entire week (Mon–Sun of selected date)</span>
          </label>
          {assignWeek && (
            <p className="text-[10px] text-muted">
              Will create assignments for: {getWeekDates(assignmentDate).filter(d => d >= today).join(', ')}
            </p>
          )}
          {error && (
            <div className="flex items-center gap-2 rounded-panel border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              <AlertCircle size={13} />{error}
            </div>
          )}
          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={onClose} disabled={saving}
              className="rounded-panel border border-border px-4 py-1.5 text-sm text-muted hover:bg-surface-alt disabled:opacity-50">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50">
              {saving ? 'Scheduling…' : assignWeek ? 'Schedule Week' : 'Schedule Day'}
            </button>
          </div>
        </form>
      </div>
    </>
  )
}

function CreateVehicleModal({ onClose, onCreated }) {
  const [form, setForm] = useState({
    registrationNumber: '',
    type: 'BUS',
    capacity: '',
    status: 'OPERATIONAL',
    manufactureDate: '',
    serviceCycleIntervalDays: '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  function setField(key, value) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const payload = {
        registrationNumber: form.registrationNumber.trim(),
        type: form.type,
        capacity: parseInt(form.capacity, 10) || null,
        status: form.status,
        manufactureDate: form.manufactureDate || null,
        serviceCycleIntervalDays: form.serviceCycleIntervalDays ? parseInt(form.serviceCycleIntervalDays, 10) : null,
      }
      const created = await gatewayClient.addVehicle(payload)
      onCreated(created)
    } catch (err) {
      setError(err.message || 'Failed to create vehicle')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <button type="button" className="fixed inset-0 z-[1100] bg-black/40" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-[1200] w-[min(520px,92vw)] -translate-x-1/2 -translate-y-1/2 rounded-panel border border-border bg-surface p-5 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-base font-semibold text-ink">New Vehicle</h3>
          <button type="button" onClick={onClose} className="rounded-panel border border-border p-1.5 text-muted hover:bg-surface-alt">
            <X size={15} />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Registration Number *</span>
            <input required type="text" value={form.registrationNumber}
              onChange={(e) => setField('registrationNumber', e.target.value)}
              placeholder="e.g. A12-B-345"
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
          </label>
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Type *</span>
              <select value={form.type} onChange={(e) => setField('type', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none">
                {VEHICLE_TYPES.map((t) => <option key={t} value={t}>{VS_TYPE_MAP[t]?.label || t}</option>)}
              </select>
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Status</span>
              <select value={form.status} onChange={(e) => setField('status', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none">
                {VEHICLE_STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s] || s}</option>)}
              </select>
            </label>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Capacity</span>
              <input type="number" min="1" value={form.capacity}
                onChange={(e) => setField('capacity', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Manufacture Date</span>
              <input type="date" value={form.manufactureDate}
                onChange={(e) => setField('manufactureDate', e.target.value)}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
            </label>
          </div>
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">Service cycle interval (days) — leave blank to disable</span>
            <input type="number" min="1" value={form.serviceCycleIntervalDays}
              onChange={(e) => setField('serviceCycleIntervalDays', e.target.value)}
              placeholder="e.g. 90"
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
          </label>
          {error && (
            <div className="flex items-center gap-2 rounded-panel border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
              <AlertCircle size={13} />{error}
            </div>
          )}
          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={onClose} disabled={saving}
              className="rounded-panel border border-border px-4 py-1.5 text-sm text-muted hover:bg-surface-alt disabled:opacity-50">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50">
              {saving ? 'Creating…' : 'Create Vehicle'}
            </button>
          </div>
        </form>
      </div>
    </>
  )
}

function FleetTab({ vehicles, lines, drivers, onUpdate, onAdd }) {
  const driverById = useMemo(() => Object.fromEntries(drivers.map((d) => [d.id, d])), [drivers])
  const lineByCode = useMemo(() => Object.fromEntries(lines.map((l) => [l.code, l])), [lines])
  const [editVehicle, setEditVehicle] = useState(null)
  const [assignLineVehicle, setAssignLineVehicle] = useState(null)
  const [assignDriverVehicle, setAssignDriverVehicle] = useState(null)
  const [createVehicle, setCreateVehicle] = useState(false)
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState('all')
  const [jpPositions, setJpPositions] = useState([])
  const [selectedVehicleId, setSelectedVehicleId] = useState(null)
  const [todayAssignments, setTodayAssignments] = useState([])

  useEffect(() => {
    transitApi.getVehiclePositions([1, 2, 3, 4]).then(setJpPositions).catch(() => {})
    const interval = setInterval(() => {
      transitApi.getVehiclePositions([1, 2, 3, 4]).then(setJpPositions).catch(() => {})
    }, 5000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (!selectedVehicleId) { setTodayAssignments([]); return }
    gatewayClient.getAssignmentsByDate(new Date().toISOString().split('T')[0])
      .then(setTodayAssignments).catch(() => setTodayAssignments([]))
  }, [selectedVehicleId])

  const filtered = useMemo(() => {
    return vehicles.filter((v) => {
      const matchType = typeFilter === 'all' || v.type === typeFilter
      const q = search.toLowerCase()
      const matchSearch = !search ||
        v.registrationNumber?.toLowerCase().includes(q) ||
        v.internalId?.toLowerCase().includes(q) ||
        v.assignedLineCode?.toLowerCase().includes(q) ||
        (v.assignedLineName || lineByCode[v.assignedLineCode]?.name || '').toLowerCase().includes(q)
      return matchType && matchSearch
    })
  }, [vehicles, typeFilter, search, lineByCode])

  const selectedVehicle = useMemo(
    () => (selectedVehicleId ? vehicles.find((v) => v.id === selectedVehicleId) : null),
    [vehicles, selectedVehicleId],
  )

  const mapVehicles = useMemo(() => {
    if (selectedVehicle) {
      const typeKey = VS_TYPE_MAP[selectedVehicle.type]?.key

      // 1. Exact 1:1 match by internalId → jp.id (set via Line modal picker)
      if (selectedVehicle.internalId) {
        const exact = jpPositions.find(
          (p) => String(p.id).trim() === String(selectedVehicle.internalId).trim()
        )
        if (exact) return [exact]
      }
      // 2. Internal GPS position
      if (selectedVehicle.lastLat != null && selectedVehicle.lastLon != null) {
        return [{
          id: selectedVehicle.id,
          lineCode: selectedVehicle.assignedLineCode || `#${selectedVehicle.id}`,
          name: `${VS_TYPE_MAP[selectedVehicle.type]?.label || ''} ${selectedVehicle.registrationNumber}`,
          latitude: selectedVehicle.lastLat,
          longitude: selectedVehicle.lastLon,
          type: typeKey || 'bus',
          typeLabel: VS_TYPE_MAP[selectedVehicle.type]?.label || '',
          color: VS_TYPE_MAP[selectedVehicle.type]?.color || '#334155',
        }]
      }
      // 3. Vehicle assigned to a line → show only JP vehicles on that line
      if (selectedVehicle.assignedLineCode) {
        const norm = (c) => String(c ?? '').trim().toLowerCase().replace(/^0+/, '')
        return jpPositions.filter(
          (p) => norm(p.lineCode) === norm(selectedVehicle.assignedLineCode)
        )
      }
      // 4. Not assigned to any line → show all live vehicles of same type
      if (typeKey) return jpPositions.filter((p) => p.type === typeKey)
      return jpPositions
    }
    if (typeFilter === 'all') return jpPositions
    const typeKey = VS_TYPE_MAP[typeFilter]?.key || typeFilter.toLowerCase()
    return jpPositions.filter((p) => p.type === typeKey)
  }, [selectedVehicle, jpPositions, typeFilter])

  const focusPositions = useMemo(
    () => mapVehicles.map((p) => [p.latitude, p.longitude]),
    [mapVehicles],
  )

  const mappedJpIds = useMemo(() => new Set(jpPositions.map((p) => String(p.id))), [jpPositions])

  const vehicleAssignmentToday = useMemo(
    () => todayAssignments.find((a) => a.vehicleId === selectedVehicleId),
    [todayAssignments, selectedVehicleId],
  )

  function handleSaved(updated) {
    onUpdate(updated)
    setEditVehicle(null)
    setAssignLineVehicle(null)
    setAssignDriverVehicle(null)
  }

  function handleCreated(created) {
    onAdd(created)
    setCreateVehicle(false)
  }

  function getLineName(v) {
    if (!v.assignedLineCode) return '—'
    const name = v.assignedLineName || lineByCode[v.assignedLineCode]?.name
    return name ? `${v.assignedLineCode} — ${name}` : v.assignedLineCode
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        <input
          type="text"
          placeholder="Search by registration, internal ID, or line…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none flex-1 min-w-[200px]"
        />
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}
          className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none">
          <option value="all">All types</option>
          {VEHICLE_TYPES.map((t) => <option key={t} value={t}>{VS_TYPE_MAP[t]?.label || t}</option>)}
        </select>
        <button type="button" onClick={() => setCreateVehicle(true)}
          className="flex items-center gap-1.5 rounded-panel border border-accent bg-accent px-3 py-1.5 text-xs font-medium text-white hover:opacity-90">
          <Plus size={13} />New Vehicle
        </button>
      </div>

      <p className="text-xs text-muted">{filtered.length} vehicle(s) shown{selectedVehicleId ? ' · click row again to deselect' : ' · click a row to see details'}</p>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_400px]">
        <div className="overflow-x-auto">
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="border-b border-border text-xs text-muted">
                <th className="text-left py-2 pr-4">Registration</th>
                <th className="text-left py-2 pr-4">Type</th>
                <th className="text-left py-2 pr-4">Status</th>
                <th className="text-left py-2 pr-2">Live</th>
                <th className="text-left py-2 pr-4">Line</th>
                <th className="text-left py-2 pr-4">Driver</th>
                <th className="text-left py-2 pr-4">Service Cycle</th>
                <th className="text-left py-2 pr-4">Mfg Date</th>
                <th className="text-left py-2">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((v) => {
                const typeInfo = VS_TYPE_MAP[v.type] || VS_TYPE_MAP.BUS
                const isSelected = v.id === selectedVehicleId
                const isMapped = v.internalId ? mappedJpIds.has(String(v.internalId)) : false
                return (
                  <tr key={v.id}
                    onClick={() => setSelectedVehicleId((prev) => prev === v.id ? null : v.id)}
                    className={`border-b border-border cursor-pointer transition ${isSelected ? 'bg-accent/5 border-l-2 border-l-accent' : 'hover:bg-surface-alt'}`}>
                    <td className="py-2 pr-4 font-medium text-ink">{v.registrationNumber}</td>
                    <td className="py-2 pr-4">
                      <span className="flex items-center gap-1.5">
                        <span className="inline-block h-2 w-2 rounded-full" style={{ backgroundColor: typeInfo.color }} />
                        {typeInfo.label}
                      </span>
                    </td>
                    <td className="py-2 pr-4">
                      <span className={`rounded px-1.5 py-0.5 text-[10px] font-semibold ${STATUS_STYLES[v.status] || ''}`}>
                        {STATUS_LABELS[v.status] || v.status}
                      </span>
                    </td>
                    <td className="py-2 pr-2">
                      <span
                        title={isMapped ? `Mapped to JP vehicle ${v.internalId}` : 'Not mapped to live vehicle'}
                        className={`inline-block h-2.5 w-2.5 rounded-full ${isMapped ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-600'}`}
                      />
                    </td>
                    <td className="py-2 pr-4 text-muted max-w-[180px] truncate">{getLineName(v)}</td>
                    <td className="py-2 pr-4 text-muted">{v.assignedDriverId ? (driverById[v.assignedDriverId]?.fullName || `#${v.assignedDriverId}`) : '—'}</td>
                    <td className="py-2 pr-4 text-muted">{v.serviceCycleIntervalDays ? `${v.serviceCycleIntervalDays}d` : '—'}</td>
                    <td className="py-2 pr-4 text-muted">{formatDate(v.manufactureDate)}</td>
                    <td className="py-2" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center gap-1">
                        <button type="button" onClick={() => setEditVehicle(v)}
                          title="Edit vehicle"
                          className="rounded p-1 text-muted hover:bg-surface-alt hover:text-ink">
                          <Edit size={13} />
                        </button>
                        <button type="button" onClick={() => setAssignLineVehicle(v)}
                          disabled={v.status !== 'OPERATIONAL'}
                          title={v.status !== 'OPERATIONAL' ? 'Vehicle must be Operational to assign a line' : 'Assign to line'}
                          className="rounded p-1 text-muted hover:bg-surface-alt hover:text-ink text-xs font-medium disabled:opacity-30 disabled:cursor-not-allowed">
                          Line
                        </button>
                        <button type="button" onClick={() => setAssignDriverVehicle(v)}
                          disabled={v.status !== 'OPERATIONAL'}
                          title={v.status !== 'OPERATIONAL' ? 'Vehicle must be Operational to schedule a driver' : 'Schedule driver'}
                          className="rounded p-1 text-muted hover:bg-surface-alt hover:text-ink text-xs font-medium disabled:opacity-30 disabled:cursor-not-allowed">
                          Driver
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
        <TransitMap
          vehicles={mapVehicles}
          focusPositions={focusPositions}
          focusKey={selectedVehicleId}
          className="h-[400px] rounded-panel lg:h-[calc(100vh-320px)]"
        />
      </div>

      {selectedVehicle && (
        <div className="rounded-panel border border-border bg-surface p-4 flex flex-col gap-3">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <p className="text-sm font-semibold text-ink">{selectedVehicle.registrationNumber}</p>
              <p className="text-xs text-muted">
                {VS_TYPE_MAP[selectedVehicle.type]?.label || selectedVehicle.type}
                {selectedVehicle.capacity ? ` · ${selectedVehicle.capacity} seats` : ''}
                {selectedVehicle.manufactureDate ? ` · Built ${formatDate(selectedVehicle.manufactureDate)}` : ''}
                {selectedVehicle.serviceCycleIntervalDays ? ` · Service every ${selectedVehicle.serviceCycleIntervalDays}d` : ''}
              </p>
            </div>
            <span className={`rounded px-2 py-0.5 text-xs font-semibold ${STATUS_STYLES[selectedVehicle.status] || ''}`}>
              {STATUS_LABELS[selectedVehicle.status] || selectedVehicle.status}
            </span>
          </div>
          <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-xs">
            <div className="flex items-center gap-1 text-muted">
              <span className="font-medium text-ink">Line:</span>
              {getLineName(selectedVehicle)}
            </div>
            <div className="flex items-center gap-1 text-muted">
              <span className="font-medium text-ink">Today's driver:</span>
              {vehicleAssignmentToday
                ? (driverById[vehicleAssignmentToday.driverId]?.fullName || `Driver #${vehicleAssignmentToday.driverId}`)
                : 'None scheduled'}
            </div>
          </div>
        </div>
      )}

      {editVehicle && <EditVehicleModal vehicle={editVehicle} onClose={() => setEditVehicle(null)} onSaved={handleSaved} />}
      {assignLineVehicle && <AssignLineModal vehicle={assignLineVehicle} lines={lines} vehicles={vehicles} onClose={() => setAssignLineVehicle(null)} onSaved={handleSaved} />}
      {assignDriverVehicle && <AssignDriverModal vehicle={assignDriverVehicle} drivers={drivers} onClose={() => setAssignDriverVehicle(null)} onSaved={handleSaved} />}
      {createVehicle && <CreateVehicleModal onClose={() => setCreateVehicle(false)} onCreated={handleCreated} />}
    </div>
  )
}

function MaintenanceTab({ vehicles, onUpdate }) {
  const [alerts, setAlerts] = useState([])
  const [alertsLoading, setAlertsLoading] = useState(true)
  const [alertsError, setAlertsError] = useState(null)

  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState('all')
  const [selectedVehicleId, setSelectedVehicleId] = useState(null)
  const [serviceRecords, setServiceRecords] = useState([])
  const [recordsLoading, setRecordsLoading] = useState(false)

  const [statusChanging, setStatusChanging] = useState(false)
  const [newStatus, setNewStatus] = useState('')
  const [statusError, setStatusError] = useState(null)

  const [showServiceForm, setShowServiceForm] = useState(false)
  const [serviceForm, setServiceForm] = useState({ serviceStart: '', serviceEnd: '', description: '', cost: '', partsChanged: '' })
  const [serviceSaving, setServiceSaving] = useState(false)
  const [serviceError, setServiceError] = useState(null)

  useEffect(() => {
    setAlertsLoading(true)
    gatewayClient.getMaintenanceAlerts()
      .then((data) => { setAlerts(Array.isArray(data) ? data : []); setAlertsLoading(false) })
      .catch((err) => { setAlertsError(err.message); setAlertsLoading(false) })
  }, [])

  useEffect(() => {
    if (!selectedVehicleId) { setServiceRecords([]); return }
    setRecordsLoading(true)
    gatewayClient.getVehicleServiceRecords(selectedVehicleId)
      .then((data) => { setServiceRecords(Array.isArray(data) ? data : []); setRecordsLoading(false) })
      .catch(() => { setServiceRecords([]); setRecordsLoading(false) })
  }, [selectedVehicleId])

  const overdueIds = useMemo(() => new Set(alerts.map((a) => a.vehicleId)), [alerts])

  const filteredVehicles = useMemo(() => {
    const q = search.toLowerCase()
    return vehicles.filter((v) => {
      const matchType = typeFilter === 'all' || v.type === typeFilter
      const matchSearch = !q ||
        v.registrationNumber?.toLowerCase().includes(q) ||
        v.internalId?.toLowerCase().includes(q) ||
        VS_TYPE_MAP[v.type]?.label.toLowerCase().includes(q)
      return matchType && matchSearch
    })
  }, [vehicles, search, typeFilter])

  const selectedVehicle = useMemo(
    () => (selectedVehicleId ? vehicles.find((v) => v.id === selectedVehicleId) : null),
    [vehicles, selectedVehicleId],
  )

  async function handleStatusChange(e) {
    e.preventDefault()
    if (!newStatus || !selectedVehicleId) return
    setStatusChanging(true)
    setStatusError(null)
    try {
      const updated = await gatewayClient.updateVehicleStatus(selectedVehicleId, newStatus)
      if (updated && onUpdate) onUpdate(updated)
      setNewStatus('')
    } catch (err) {
      setStatusError(err.message)
    } finally {
      setStatusChanging(false)
    }
  }

  function toUtcIso(localDateTimeStr) {
    if (!localDateTimeStr) return null
    // datetime-local gives local time without tz; convert to UTC for backend
    return new Date(localDateTimeStr).toISOString().slice(0, 19)
  }

  async function handleServiceSubmit(e) {
    e.preventDefault()
    setServiceError(null)
    if (new Date(serviceForm.serviceStart) < new Date()) {
      setServiceError('Service start date cannot be in the past')
      return
    }
    if (serviceForm.serviceEnd && new Date(serviceForm.serviceEnd) <= new Date(serviceForm.serviceStart)) {
      setServiceError('Service end must be after start')
      return
    }
    setServiceSaving(true)
    try {
      const payload = {
        serviceStart: toUtcIso(serviceForm.serviceStart),
        serviceEnd: toUtcIso(serviceForm.serviceEnd),
        description: serviceForm.description || null,
        cost: serviceForm.cost ? parseFloat(serviceForm.cost) : null,
        partsChanged: serviceForm.partsChanged || null,
      }
      const saved = await gatewayClient.addServiceRecord(selectedVehicleId, payload)
      setServiceRecords((prev) => [saved, ...prev])
      setShowServiceForm(false)
      setServiceForm({ serviceStart: '', serviceEnd: '', description: '', cost: '', partsChanged: '' })
    } catch (err) {
      setServiceError(err.message)
    } finally {
      setServiceSaving(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      {/* Overdue alerts banner */}
      {!alertsLoading && !alertsError && alerts.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 rounded-panel border border-amber-200 bg-amber-50 px-4 py-2.5 text-sm text-amber-800 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-300">
          <AlertTriangle size={15} className="shrink-0" />
          <span className="font-medium">{alerts.length} vehicle(s) overdue for service.</span>
          <span className="text-xs">Overdue vehicles are marked below.</span>
        </div>
      )}
      {!alertsLoading && !alertsError && alerts.length === 0 && (
        <div className="flex items-center gap-2 rounded-panel border border-emerald-200 bg-emerald-50 px-4 py-2.5 text-sm text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-400">
          <CheckCircle size={15} /> All vehicles up to date on scheduled maintenance.
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[300px_1fr]">
        {/* Left: vehicle list */}
        <div className="flex flex-col gap-2">
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Search…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 rounded-panel border border-border bg-surface px-2 py-1.5 text-xs text-ink placeholder:text-muted focus:border-accent focus:outline-none"
            />
            <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}
              className="rounded-panel border border-border bg-surface px-2 py-1.5 text-xs text-ink focus:border-accent focus:outline-none">
              <option value="all">All</option>
              {VEHICLE_TYPES.map((t) => <option key={t} value={t}>{VS_TYPE_MAP[t]?.label || t}</option>)}
            </select>
          </div>
          <div className="flex flex-col gap-1 max-h-[calc(100vh-340px)] overflow-y-auto">
            {filteredVehicles.length === 0 && (
              <p className="py-4 text-center text-xs text-muted">No vehicles match.</p>
            )}
            {filteredVehicles.map((v) => {
              const typeInfo = VS_TYPE_MAP[v.type] || VS_TYPE_MAP.BUS
              const isSelected = v.id === selectedVehicleId
              const isOverdue = overdueIds.has(v.id)
              return (
                <button key={v.id} type="button"
                  onClick={() => setSelectedVehicleId((prev) => prev === v.id ? null : v.id)}
                  className={`flex items-center gap-2 rounded-panel border px-3 py-2 text-left transition ${
                    isSelected ? 'border-accent bg-accent/5' : 'border-border hover:bg-surface-alt'
                  }`}>
                  <span className="inline-block h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: typeInfo.color }} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-xs font-medium text-ink">{v.registrationNumber}</span>
                    <span className="block text-[10px] text-muted">{typeInfo.label}</span>
                  </span>
                  {isOverdue && (
                    <span className="shrink-0 rounded px-1 py-0.5 text-[9px] font-bold bg-red-100 text-red-700 dark:bg-red-950/30 dark:text-red-400">
                      OVERDUE
                    </span>
                  )}
                  <span className={`shrink-0 rounded px-1.5 py-0.5 text-[9px] font-semibold ${STATUS_STYLES[v.status] || ''}`}>
                    {STATUS_LABELS[v.status] || v.status}
                  </span>
                </button>
              )
            })}
          </div>
        </div>

        {/* Right: selected vehicle detail */}
        {!selectedVehicle ? (
          <div className="flex items-center justify-center rounded-panel border border-border p-8 text-sm text-muted">
            Select a vehicle from the list to manage its status and service records.
          </div>
        ) : (
          <div className="flex flex-col gap-4 rounded-panel border border-border p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-ink">{selectedVehicle.registrationNumber}</p>
                <p className="text-xs text-muted">{VS_TYPE_MAP[selectedVehicle.type]?.label || selectedVehicle.type}
                  {selectedVehicle.assignedLineCode ? ` · Line ${selectedVehicle.assignedLineCode}` : ''}
                </p>
                {overdueIds.has(selectedVehicle.id) && (
                  <p className="mt-0.5 text-xs font-medium text-red-600">⚠ Overdue for service</p>
                )}
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className={`rounded px-2 py-0.5 text-xs font-semibold ${STATUS_STYLES[selectedVehicle.status] || ''}`}>
                  {STATUS_LABELS[selectedVehicle.status] || selectedVehicle.status}
                </span>
                <form onSubmit={handleStatusChange} className="flex items-center gap-1.5">
                  <select value={newStatus} onChange={(e) => setNewStatus(e.target.value)}
                    className="rounded-panel border border-border bg-surface px-2 py-1 text-xs text-ink focus:border-accent focus:outline-none">
                    <option value="">Change status…</option>
                    {VEHICLE_STATUSES.map((s) => (
                      <option key={s} value={s} disabled={s === selectedVehicle.status}>{STATUS_LABELS[s] || s}</option>
                    ))}
                  </select>
                  <button type="submit" disabled={!newStatus || statusChanging}
                    className="rounded-panel bg-accent px-2 py-1 text-xs font-medium text-white hover:opacity-90 disabled:opacity-50">
                    {statusChanging ? 'Saving…' : 'Apply'}
                  </button>
                </form>
              </div>
            </div>
            {statusError && <p className="text-xs text-red-600">{statusError}</p>}

            <div className="flex items-center justify-between border-t border-border pt-3">
              <h4 className="text-xs font-semibold uppercase tracking-wide text-ink">Service Records</h4>
              <button type="button" onClick={() => setShowServiceForm((p) => !p)}
                className="rounded-panel border border-accent px-2 py-1 text-xs font-medium text-accent hover:bg-accent/5">
                {showServiceForm ? 'Cancel' : '+ Schedule Service'}
              </button>
            </div>

            {showServiceForm && (
              <form onSubmit={handleServiceSubmit} className="flex flex-col gap-2 rounded-panel bg-surface-alt p-3">
                <div className="grid grid-cols-2 gap-2">
                  <label className="flex flex-col gap-0.5">
                    <span className="text-xs font-medium text-muted">Service Start *</span>
                    <input required type="datetime-local" value={serviceForm.serviceStart}
                      min={new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16)}
                      onChange={(e) => setServiceForm((p) => ({ ...p, serviceStart: e.target.value }))}
                      className="rounded-panel border border-border bg-surface px-2 py-1 text-xs text-ink focus:border-accent focus:outline-none" />
                  </label>
                  <label className="flex flex-col gap-0.5">
                    <span className="text-xs font-medium text-muted">Service End (optional)</span>
                    <input type="datetime-local" value={serviceForm.serviceEnd}
                      min={serviceForm.serviceStart || new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16)}
                      onChange={(e) => setServiceForm((p) => ({ ...p, serviceEnd: e.target.value }))}
                      className="rounded-panel border border-border bg-surface px-2 py-1 text-xs text-ink focus:border-accent focus:outline-none" />
                  </label>
                </div>
                <label className="flex flex-col gap-0.5">
                  <span className="text-xs font-medium text-muted">Description</span>
                  <textarea rows={2} value={serviceForm.description}
                    onChange={(e) => setServiceForm((p) => ({ ...p, description: e.target.value }))}
                    className="rounded-panel border border-border bg-surface px-2 py-1 text-xs text-ink focus:border-accent focus:outline-none resize-none" />
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <label className="flex flex-col gap-0.5">
                    <span className="text-xs font-medium text-muted">Cost (KM)</span>
                    <input type="number" min="0" step="0.01" value={serviceForm.cost}
                      onChange={(e) => setServiceForm((p) => ({ ...p, cost: e.target.value }))}
                      className="rounded-panel border border-border bg-surface px-2 py-1 text-xs text-ink focus:border-accent focus:outline-none" />
                  </label>
                  <label className="flex flex-col gap-0.5">
                    <span className="text-xs font-medium text-muted">Parts Changed</span>
                    <input type="text" value={serviceForm.partsChanged}
                      onChange={(e) => setServiceForm((p) => ({ ...p, partsChanged: e.target.value }))}
                      className="rounded-panel border border-border bg-surface px-2 py-1 text-xs text-ink focus:border-accent focus:outline-none" />
                  </label>
                </div>
                {serviceError && <p className="text-xs text-red-600">{serviceError}</p>}
                <div className="flex justify-end">
                  <button type="submit" disabled={serviceSaving}
                    className="rounded-panel bg-accent px-3 py-1 text-xs font-medium text-white hover:opacity-90 disabled:opacity-50">
                    {serviceSaving ? 'Saving…' : 'Schedule Service'}
                  </button>
                </div>
              </form>
            )}

            {recordsLoading ? (
              <p className="text-xs text-muted">Loading service history…</p>
            ) : serviceRecords.length === 0 ? (
              <p className="text-xs text-muted">No service records for this vehicle.</p>
            ) : (
              <table className="w-full text-xs border-collapse">
                <thead>
                  <tr className="border-b border-border text-muted">
                    <th className="text-left py-1.5 pr-3">Start</th>
                    <th className="text-left py-1.5 pr-3">End</th>
                    <th className="text-left py-1.5 pr-3">Description</th>
                    <th className="text-left py-1.5 pr-3">Cost</th>
                    <th className="text-left py-1.5">Parts</th>
                  </tr>
                </thead>
                <tbody>
                  {serviceRecords.map((r) => (
                    <tr key={r.id} className="border-b border-border hover:bg-surface-alt">
                      <td className="py-1.5 pr-3 text-ink">{r.serviceStart ? new Date(r.serviceStart + 'Z').toLocaleString() : '—'}</td>
                      <td className="py-1.5 pr-3 text-muted">{r.serviceEnd ? new Date(r.serviceEnd + 'Z').toLocaleString() : <span className="text-amber-600">In progress</span>}</td>
                      <td className="py-1.5 pr-3 text-muted max-w-[160px] truncate">{r.description || '—'}</td>
                      <td className="py-1.5 pr-3 text-muted">{r.cost != null ? `${r.cost} KM` : '—'}</td>
                      <td className="py-1.5 text-muted">{r.partsChanged || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

export function AdminVehiclesPage() {
  const [activeTab, setActiveTab] = useState('fleet')
  const [vehicles, setVehicles] = useState([])
  const [lines, setLines] = useState([])
  const [drivers, setDrivers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [alertCount, setAlertCount] = useState(0)

  async function loadData() {
    setLoading(true)
    setError(null)
    try {
      const [vRes, lRes, dRes, aRes] = await Promise.allSettled([
        gatewayClient.getVehicles('?size=500&sort=id,asc'),
        transitApi.getLines(),
        gatewayClient.getDriverProfiles(),
        gatewayClient.getMaintenanceAlerts(),
      ])
      if (vRes.status === 'fulfilled') {
        const v = vRes.value
        setVehicles(Array.isArray(v?.content) ? v.content : Array.isArray(v) ? v : [])
      }
      if (lRes.status === 'fulfilled') setLines(lRes.value || [])
      if (dRes.status === 'fulfilled') setDrivers(dRes.value || [])
      if (aRes.status === 'fulfilled') setAlertCount((aRes.value || []).length)
    } catch (err) {
      setError(err.message || 'Failed to load fleet data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  function handleVehicleUpdate(updated) {
    setVehicles((prev) => prev.map((v) => v.id === updated.id ? updated : v))
  }

  function handleVehicleAdd(created) {
    setVehicles((prev) => [created, ...prev])
  }

  const TAB_LABELS = {
    fleet: 'Fleet',
    maintenance: alertCount > 0 ? `Maintenance (${alertCount})` : 'Maintenance',
  }

  if (loading) return <p className="py-8 text-center text-sm text-muted">Loading fleet…</p>
  if (error) return (
    <div className="flex items-center gap-2 rounded-panel border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      <AlertCircle size={16} />{error}
    </div>
  )

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-ink">
            <Truck size={18} />Fleet Management
          </h2>
          <p className="text-xs text-muted">{vehicles.length} vehicles total</p>
        </div>
        <button type="button" onClick={loadData}
          className="rounded-panel border border-border p-1.5 text-muted hover:bg-surface-alt hover:text-ink">
          <RefreshCw size={14} />
        </button>
      </div>

      <div className="border-b border-border">
        <nav className="flex gap-0.5">
          {TABS.map((tab) => (
            <button key={tab} type="button" onClick={() => setActiveTab(tab)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 transition ${
                activeTab === tab
                  ? 'border-accent text-ink'
                  : 'border-transparent text-muted hover:text-ink'
              } ${tab === 'maintenance' && alertCount > 0 ? 'text-amber-600 dark:text-amber-400' : ''}`}>
              {TAB_LABELS[tab]}
              {tab === 'maintenance' && alertCount > 0 && (
                <span className="ml-1.5 inline-flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[9px] font-bold text-white">
                  {alertCount}
                </span>
              )}
            </button>
          ))}
        </nav>
      </div>

      {activeTab === 'fleet' && (
        <FleetTab vehicles={vehicles} lines={lines} drivers={drivers} onUpdate={handleVehicleUpdate} onAdd={handleVehicleAdd} />
      )}
      {activeTab === 'maintenance' && <MaintenanceTab vehicles={vehicles} onUpdate={handleVehicleUpdate} />}
    </div>
  )
}
