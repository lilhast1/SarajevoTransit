import { useEffect, useState, useCallback } from 'react'
import { Plus, X } from 'lucide-react'
import { DataTable } from '../../components/admin/DataTable'
import { PanelCard } from '../../components/common/PanelCard'
import { ErrorAlert } from '../../components/common/Alerts'
import { VEHICLE_TYPE_META_BY_ID } from '../../constants/vehicleColors'
import { gatewayClient } from '../../services/gatewayClient'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)

const EMPTY_FORM = { code: '', name: '', vehicleTypeId: 1, isActive: true }

export function AdminLinesPage() {
  const [lines, setLines] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editingLine, setEditingLine] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await gatewayClient.getLines('')
      setLines(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  function openCreate() {
    setEditingLine(null)
    setForm(EMPTY_FORM)
    setFormOpen(true)
  }

  function openEdit(line) {
    setEditingLine(line)
    setForm({
      code: line.code ?? '',
      name: line.name ?? '',
      vehicleTypeId: line.vehicleTypeId ?? 1,
      isActive: line.isActive ?? true,
    })
    setFormOpen(true)
  }

  function closeForm() {
    setFormOpen(false)
    setEditingLine(null)
  }

  async function handleSave(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const payload = { ...form, vehicleTypeId: Number(form.vehicleTypeId) }
      if (editingLine) {
        await gatewayClient.updateLine(editingLine.id, payload)
      } else {
        await gatewayClient.createLine(payload)
      }
      closeForm()
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleActive(line) {
    const action = line.isActive ? 'deactivate' : 'activate'
    if (!window.confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} line "${line.name}"?`)) return
    try {
      await gatewayClient.updateLine(line.id, {
        code: line.code,
        name: line.name,
        vehicleTypeId: line.vehicleTypeId,
        isActive: !line.isActive,
      })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const columns = [
    { key: 'code', label: 'Code' },
    { key: 'name', label: 'Name' },
    {
      key: 'vehicleTypeId', label: 'Type', render: (r) =>
        VEHICLE_TYPE_META_BY_ID[r.vehicleTypeId]?.label ?? r.vehicleTypeName ?? '—'
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
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => openEdit(r)}
            className="rounded border border-border px-2 py-0.5 text-xs text-ink hover:bg-surface-alt"
          >
            Edit
          </button>
          <button
            type="button"
            onClick={() => handleToggleActive(r)}
            className={`rounded border px-2 py-0.5 text-xs ${
              r.isActive
                ? 'border-yellow-300 text-yellow-700 hover:bg-yellow-50 dark:hover:bg-yellow-950'
                : 'border-green-300 text-green-700 hover:bg-green-50 dark:hover:bg-green-950'
            }`}
          >
            {r.isActive ? 'Deactivate' : 'Activate'}
          </button>
        </div>
      )
    },
  ]

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-ink">Lines</h2>
          <p className="mt-1 text-sm text-muted">Create, edit, and activate/deactivate transit lines.</p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex items-center gap-1.5 rounded-panel border border-accent bg-accent px-3 py-2 text-sm font-medium text-white"
        >
          <Plus size={15} /> New Line
        </button>
      </div>

      {formOpen && (
        <PanelCard tone="soft">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-ink">{editingLine ? 'Edit Line' : 'New Line'}</h3>
            <button type="button" onClick={closeForm} className="text-muted hover:text-ink"><X size={16} /></button>
          </div>
          <form onSubmit={handleSave} className="grid gap-3 sm:grid-cols-2">
            <label className="block">
              <span className="text-xs text-muted">Code</span>
              <input
                required
                value={form.code}
                onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
              />
            </label>
            <label className="block">
              <span className="text-xs text-muted">Name</span>
              <input
                required
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
              />
            </label>
            <label className="block">
              <span className="text-xs text-muted">Vehicle Type</span>
              <select
                value={form.vehicleTypeId}
                onChange={(e) => setForm((f) => ({ ...f, vehicleTypeId: Number(e.target.value) }))}
                className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
              >
                {VEHICLE_TYPES.map((vt) => (
                  <option key={vt.id} value={vt.id}>{vt.label}</option>
                ))}
              </select>
            </label>
            <label className="flex items-center gap-2 self-end pb-1.5">
              <input
                type="checkbox"
                checked={form.isActive}
                onChange={(e) => setForm((f) => ({ ...f, isActive: e.target.checked }))}
              />
              <span className="text-sm text-ink">Active</span>
            </label>
            <div className="flex gap-2 sm:col-span-2">
              <button
                type="submit"
                disabled={saving}
                className="rounded-panel border border-accent bg-accent px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60"
              >
                {saving ? 'Saving…' : 'Save'}
              </button>
              <button type="button" onClick={closeForm} className="rounded-panel border border-border px-4 py-1.5 text-sm text-ink">
                Cancel
              </button>
            </div>
          </form>
        </PanelCard>
      )}

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      <DataTable
        columns={columns}
        rows={lines}
        page={0}
        totalPages={1}
        onPageChange={() => {}}
        loading={loading}
      />
    </div>
  )
}

export default AdminLinesPage
