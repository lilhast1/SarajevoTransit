import { useEffect, useState, useCallback } from 'react'
import { Plus, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { DataTable } from '../../components/admin/DataTable'
import { PanelCard } from '../../components/common/PanelCard'
import { ErrorAlert } from '../../components/common/Alerts'
import { VEHICLE_TYPE_META_BY_ID } from '../../constants/vehicleColors'
import { gatewayClient } from '../../services/gatewayClient'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)

const EMPTY_FORM = { code: '', name: '', address: '', latitude: '', longitude: '', isActive: true }

export function AdminStationsPage() {
  const { t } = useTranslation('admin-stations')
  const [stations, setStations] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editingStation, setEditingStation] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  // line filter state
  const [vehicleTypeId, setVehicleTypeId] = useState('')
  const [lines, setLines] = useState([])
  const [lineId, setLineId] = useState('')
  const [directions, setDirections] = useState([])
  const [directionId, setDirectionId] = useState('')
  const [directionStationIds, setDirectionStationIds] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await gatewayClient.getStations('')
      setStations(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  // vehicle type → lines
  useEffect(() => {
    const q = vehicleTypeId ? `?vehicleTypeId=${vehicleTypeId}` : ''
    setLineId('')
    setDirections([])
    setDirectionId('')
    setDirectionStationIds(null)
    gatewayClient.getLines(q).then(setLines).catch(() => {})
  }, [vehicleTypeId])

  // line → directions
  useEffect(() => {
    if (!lineId) {
      setDirections([])
      setDirectionId('')
      setDirectionStationIds(null)
      return
    }
    gatewayClient.getDirections(`?lineId=${lineId}&activeOnly=true`)
      .then(setDirections)
      .catch(() => setDirections([]))
    setDirectionId('')
    setDirectionStationIds(null)
  }, [lineId])

  // direction → station IDs
  useEffect(() => {
    if (!directionId) { setDirectionStationIds(null); return }
    gatewayClient.getDirectionStations(directionId)
      .then((res) => setDirectionStationIds(new Set(res.map((s) => s.stationId))))
      .catch(() => setDirectionStationIds(null))
  }, [directionId])

  const filteredStations = directionStationIds
    ? stations.filter((s) => directionStationIds.has(s.id))
    : stations

  function openCreate() {
    setEditingStation(null)
    setForm(EMPTY_FORM)
    setFormOpen(true)
  }

  function openEdit(station) {
    setEditingStation(station)
    setForm({
      code: station.code ?? '',
      name: station.name ?? '',
      address: station.address ?? '',
      latitude: station.latitude ?? '',
      longitude: station.longitude ?? '',
      isActive: station.isActive ?? true,
    })
    setFormOpen(true)
  }

  function closeForm() {
    setFormOpen(false)
    setEditingStation(null)
  }

  async function handleSave(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const payload = {
        ...form,
        latitude: Number(form.latitude),
        longitude: Number(form.longitude),
      }
      if (editingStation) {
        await gatewayClient.updateStation(editingStation.id, payload)
      } else {
        await gatewayClient.createStation(payload)
      }
      closeForm()
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleActive(station) {
    const action = station.isActive ? 'deactivate' : 'activate'
    if (!window.confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} station "${station.name}"?`)) return
    try {
      await gatewayClient.updateStation(station.id, {
        code: station.code,
        name: station.name,
        address: station.address,
        latitude: station.latitude,
        longitude: station.longitude,
        isActive: !station.isActive,
      })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const field = (key) => ({
    value: form[key],
    onChange: (e) => setForm((f) => ({ ...f, [key]: e.target.value }),)
  })

  const columns = [
    { key: 'code', label: t('col_code') },
    { key: 'name', label: t('col_name') },
    { key: 'address', label: t('col_address') },
    { key: 'latitude', label: t('col_lat'), render: (r) => r.latitude?.toFixed(5) ?? '—' },
    { key: 'longitude', label: t('col_lng'), render: (r) => r.longitude?.toFixed(5) ?? '—' },
    {
      key: 'isActive', label: t('col_active'), render: (r) => (
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${r.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
          {r.isActive ? 'Yes' : 'No'}
        </span>
      )
    },
    {
      key: 'actions', label: t('col_actions'), render: (r) => (
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => openEdit(r)}
            className="rounded border border-border px-2 py-0.5 text-xs text-ink hover:bg-surface-alt"
          >
            {t('edit')}
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
            {r.isActive ? t('deactivate') : t('activate')}
          </button>
        </div>
      )
    },
  ]

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-ink">{t('title')}</h2>
          <p className="mt-1 text-sm text-muted">{t('subtitle')}</p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="flex items-center gap-1.5 rounded-panel border border-accent bg-accent px-3 py-2 text-sm font-medium text-white"
        >
          <Plus size={15} /> {t('new_station')}
        </button>
      </div>

      {/* vehicle type filter */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm text-muted">{t('type_label')}</span>
        {[{ id: '', label: t('all') }, ...VEHICLE_TYPES].map((vt) => (
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

      {/* line + direction filter */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted">{t('line_label')}</span>
          <select
            value={lineId}
            onChange={(e) => setLineId(e.target.value)}
            className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
          >
            <option value="">{t('all_lines')}</option>
            {lines.map((l) => <option key={l.id} value={l.id}>{l.code} – {l.name}</option>)}
          </select>
        </div>
        {lineId && (
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted">{t('direction_label')}</span>
            <select
              value={directionId}
              onChange={(e) => setDirectionId(e.target.value)}
              className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink"
            >
              <option value="">{t('all_directions')}</option>
              {directions.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
          </div>
        )}
        {directionStationIds && (
          <span className="text-xs text-muted">
            {t('showing', { shown: filteredStations.length, total: stations.length })}
          </span>
        )}
      </div>

      {formOpen && (
        <PanelCard tone="soft">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-ink">{editingStation ? t('edit_title') : t('new_title')}</h3>
            <button type="button" onClick={closeForm} className="text-muted hover:text-ink"><X size={16} /></button>
          </div>
          <form onSubmit={handleSave} className="grid gap-3 sm:grid-cols-2">
            <label className="block">
              <span className="text-xs text-muted">{t('field_code')}</span>
              <input required className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink" {...field('code')} />
            </label>
            <label className="block">
              <span className="text-xs text-muted">{t('field_name')}</span>
              <input required className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink" {...field('name')} />
            </label>
            <label className="block sm:col-span-2">
              <span className="text-xs text-muted">{t('field_address')}</span>
              <input className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink" {...field('address')} />
            </label>
            <label className="block">
              <span className="text-xs text-muted">{t('field_lat')}</span>
              <input required type="number" step="any" className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink" {...field('latitude')} />
            </label>
            <label className="block">
              <span className="text-xs text-muted">{t('field_lng')}</span>
              <input required type="number" step="any" className="mt-1 w-full rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink" {...field('longitude')} />
            </label>
            <label className="flex items-center gap-2 self-end pb-1.5">
              <input
                type="checkbox"
                checked={form.isActive}
                onChange={(e) => setForm((f) => ({ ...f, isActive: e.target.checked }))}
              />
              <span className="text-sm text-ink">{t('field_active')}</span>
            </label>
            <div className="flex gap-2 sm:col-span-2">
              <button
                type="submit"
                disabled={saving}
                className="rounded-panel border border-accent bg-accent px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60"
              >
                {saving ? t('saving') : t('save')}
              </button>
              <button type="button" onClick={closeForm} className="rounded-panel border border-border px-4 py-1.5 text-sm text-ink">
                {t('cancel')}
              </button>
            </div>
          </form>
        </PanelCard>
      )}

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      <DataTable
        columns={columns}
        rows={filteredStations}
        page={0}
        totalPages={1}
        onPageChange={() => {}}
        loading={loading}
      />
    </div>
  )
}

export default AdminStationsPage
