import { AlertCircle, Plus, RefreshCw, User, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { gatewayClient } from '../../services/gatewayClient'
import { VS_TYPE_MAP } from '../VehiclesPage'

const VEHICLE_TYPES = ['BUS', 'TRAM', 'TROLLEY', 'MINIBUS']

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

function CreateDriverModal({ onClose, onCreated }) {
  const [form, setForm] = useState({
    fullName: '',
    phone: '',
    licenseNumber: '',
    licensedVehicleTypes: [],
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  function toggleType(type) {
    setForm((prev) => ({
      ...prev,
      licensedVehicleTypes: prev.licensedVehicleTypes.includes(type)
        ? prev.licensedVehicleTypes.filter((t) => t !== type)
        : [...prev.licensedVehicleTypes, type],
    }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const phone = form.phone.trim()
    const PHONE_REGEX = /^[+]?[\d\s\-().]{7,20}$/
    if (phone && !PHONE_REGEX.test(phone)) {
      setError('Enter a valid phone number (7–20 digits, +, -, spaces, parentheses allowed)')
      return
    }
    setSaving(true)
    setError(null)
    try {
      const payload = {
        fullName: form.fullName.trim() || null,
        phone: form.phone.trim() || null,
        licenseNumber: form.licenseNumber.trim() || null,
        licensedVehicleTypes: form.licensedVehicleTypes,
      }
      const created = await gatewayClient.createDriverProfile(payload)
      onCreated(created)
    } catch (err) {
      setError(err.message || 'Failed to create driver')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <button type="button" className="fixed inset-0 z-[1100] bg-black/40" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-[1200] w-[min(480px,92vw)] -translate-x-1/2 -translate-y-1/2 rounded-panel border border-border bg-surface p-5 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-base font-semibold text-ink">Create Driver Profile</h3>
          <button type="button" onClick={onClose} className="rounded-panel border border-border p-1.5 text-muted hover:bg-surface-alt">
            <X size={15} />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-3">
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Full Name</span>
              <input type="text" value={form.fullName}
                onChange={(e) => setForm((p) => ({ ...p, fullName: e.target.value }))}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-xs font-medium text-muted">Phone</span>
              <input type="text" value={form.phone}
                onChange={(e) => setForm((p) => ({ ...p, phone: e.target.value }))}
                className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
            </label>
          </div>
          <label className="flex flex-col gap-1">
            <span className="text-xs font-medium text-muted">License Number</span>
            <input type="text" value={form.licenseNumber}
              onChange={(e) => setForm((p) => ({ ...p, licenseNumber: e.target.value }))}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink focus:border-accent focus:outline-none" />
          </label>
          <div className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-muted">Licensed Vehicle Types</span>
            <div className="flex flex-wrap gap-2">
              {VEHICLE_TYPES.map((type) => {
                const isSelected = form.licensedVehicleTypes.includes(type)
                const meta = VS_TYPE_MAP[type]
                return (
                  <button key={type} type="button" onClick={() => toggleType(type)}
                    className={`flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition ${
                      isSelected ? 'border-accent bg-accent text-white' : 'border-border text-muted hover:bg-surface-alt'
                    }`}>
                    <span className="inline-block h-2 w-2 rounded-full" style={{ backgroundColor: meta?.color }} />
                    {meta?.label || type}
                  </button>
                )
              })}
            </div>
          </div>
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
              {saving ? 'Creating…' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </>
  )
}

export function AdminDriversPage() {
  const [drivers, setDrivers] = useState([])
  const [vehicles, setVehicles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showCreateDriver, setShowCreateDriver] = useState(false)
  const [selectedDriver, setSelectedDriver] = useState(null)
  const [driverAssignments, setDriverAssignments] = useState([])

  const vehicleById = useMemo(() => Object.fromEntries(vehicles.map((v) => [v.id, v])), [vehicles])

  async function loadData() {
    setLoading(true)
    setError(null)
    try {
      const [dRes, vRes] = await Promise.allSettled([
        gatewayClient.getDriverProfiles(),
        gatewayClient.getVehicles('?size=500'),
      ])
      if (dRes.status === 'fulfilled') setDrivers(dRes.value || [])
      if (vRes.status === 'fulfilled') {
        const v = vRes.value
        setVehicles(Array.isArray(v?.content) ? v.content : Array.isArray(v) ? v : [])
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  async function loadDriverAssignments(driver) {
    setSelectedDriver(driver)
    try {
      const assignments = await gatewayClient.getDriverAssignments(driver.id)
      setDriverAssignments(assignments || [])
    } catch (err) {
      setDriverAssignments([])
    }
  }

  function handleDriverCreated(driver) {
    setDrivers((prev) => [...prev, driver])
    setShowCreateDriver(false)
  }

  if (loading) return <p className="py-8 text-center text-sm text-muted">Loading drivers…</p>
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
            <User size={18} />Driver Management
          </h2>
          <p className="text-xs text-muted">{drivers.length} driver profile(s)</p>
        </div>
        <div className="flex items-center gap-2">
          <button type="button" onClick={() => setShowCreateDriver(true)}
            className="flex items-center gap-1.5 rounded-panel border border-accent bg-accent px-3 py-1.5 text-xs font-medium text-white hover:opacity-90">
            <Plus size={13} />New Driver
          </button>
          <button type="button" onClick={loadData}
            className="rounded-panel border border-border p-1.5 text-muted hover:bg-surface-alt hover:text-ink">
            <RefreshCw size={14} />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[320px_1fr]">
        {/* Driver list */}
        <div className="flex flex-col gap-1.5">
          {drivers.length === 0 && (
            <p className="text-sm text-muted py-4 text-center">No driver profiles yet.</p>
          )}
          {drivers.map((d) => {
            const isSelected = selectedDriver?.id === d.id
            return (
              <button key={d.id} type="button" onClick={() => loadDriverAssignments(d)}
                className={`rounded-panel border px-3 py-2.5 text-left transition ${
                  isSelected ? 'border-accent bg-accent/5' : 'border-border hover:bg-surface-alt'
                }`}>
                <div className="flex items-center gap-2">
                  <User size={14} className="shrink-0 text-muted" />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ink truncate">{d.fullName || `Driver #${d.id}`}</p>
                    <p className="text-xs text-muted">{d.phone || 'no phone'} · {d.licenseNumber || 'no license'}</p>
                  </div>
                </div>
                {d.licensedVehicleTypes?.length > 0 && (
                  <div className="mt-1 flex flex-wrap gap-1">
                    {d.licensedVehicleTypes.map((t) => (
                      <span key={t} className="rounded px-1.5 py-0.5 text-[10px] font-medium"
                        style={{ backgroundColor: VS_TYPE_MAP[t]?.color + '22', color: VS_TYPE_MAP[t]?.color }}>
                        {VS_TYPE_MAP[t]?.label || t}
                      </span>
                    ))}
                  </div>
                )}
              </button>
            )
          })}
        </div>

        {/* Driver detail */}
        {selectedDriver ? (
          <div className="rounded-panel border border-border p-4 flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-semibold text-ink">{selectedDriver.fullName || `Driver #${selectedDriver.id}`}</h3>
                <p className="text-xs text-muted">License: {selectedDriver.licenseNumber || '—'} · {selectedDriver.phone || 'no phone'}</p>
              </div>
            </div>
            <div className="flex flex-col gap-2">
              <h4 className="text-sm font-medium text-ink">Assignment History</h4>
              {driverAssignments.length === 0 ? (
                <p className="text-sm text-muted">No assignments yet.</p>
              ) : (
                <table className="w-full text-sm border-collapse">
                  <thead>
                    <tr className="border-b border-border text-xs text-muted">
                      <th className="text-left py-2 pr-4">Date</th>
                      <th className="text-left py-2 pr-4">Vehicle</th>
                      <th className="text-left py-2 pr-4">Line</th>
                      <th className="text-left py-2">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {driverAssignments.map((a) => {
                      const vehicle = vehicleById[a.vehicleId]
                      return (
                        <tr key={a.id} className="border-b border-border hover:bg-surface-alt">
                          <td className="py-2 pr-4 text-ink">{formatDate(a.assignmentDate)}</td>
                          <td className="py-2 pr-4 text-muted">
                            {vehicle ? `${vehicle.registrationNumber} (${VS_TYPE_MAP[vehicle.type]?.label || vehicle.type})` : `#${a.vehicleId}`}
                          </td>
                          <td className="py-2 pr-4 text-muted">{a.lineCode ? `${a.lineCode}${a.lineName ? ` — ${a.lineName}` : ''}` : '—'}</td>
                          <td className="py-2">
                            <span className={`rounded px-1.5 py-0.5 text-[10px] font-semibold ${a.active ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                              {a.active ? 'Active' : 'Cancelled'}
                            </span>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center rounded-panel border border-border p-8 text-muted">
            Select a driver to view details
          </div>
        )}
      </div>

      {showCreateDriver && (
        <CreateDriverModal onClose={() => setShowCreateDriver(false)} onCreated={handleDriverCreated} />
      )}
    </div>
  )
}
