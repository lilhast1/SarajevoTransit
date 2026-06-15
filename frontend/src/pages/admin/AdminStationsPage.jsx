import { useEffect, useState, useCallback, useMemo } from 'react'
import { Plus, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { DataTable } from '../../components/admin/DataTable'
import { AdminPagePanel, SELECT_CLS } from '../../components/common/AdminPagePanel'
import { StatusBadge } from '../../components/common/StatusBadge'
import { ErrorAlert } from '../../components/common/Alerts'
import { gatewayClient } from '../../services/gatewayClient'

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
  const [search, setSearch] = useState('')

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

  const filteredStations = useMemo(() => {
    const q = search.toLowerCase().trim()
    if (!q) return stations
    return stations.filter((s) =>
      s.code?.toLowerCase().includes(q) ||
      s.name?.toLowerCase().includes(q) ||
      s.address?.toLowerCase().includes(q)
    )
  }, [stations, search])

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
      const payload = { ...form, latitude: Number(form.latitude), longitude: Number(form.longitude) }
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
        code: station.code, name: station.name, address: station.address,
        latitude: station.latitude, longitude: station.longitude, isActive: !station.isActive,
      })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const field = (key) => ({
    value: form[key],
    onChange: (e) => setForm((f) => ({ ...f, [key]: e.target.value })),
  })

  const columns = [
    { key: 'code', label: t('col_code') },
    { key: 'name', label: t('col_name') },
    { key: 'address', label: t('col_address') },
    { key: 'latitude', label: t('col_lat'), render: (r) => r.latitude?.toFixed(5) ?? '—' },
    { key: 'longitude', label: t('col_lng'), render: (r) => r.longitude?.toFixed(5) ?? '—' },
    {
      key: 'isActive', label: t('col_active'), render: (r) => <StatusBadge status={r.isActive ? 'ACTIVE' : 'INACTIVE'} />
    },
    {
      key: 'actions', label: t('col_actions'), render: (r) => (
        <div className="flex gap-2">
          <button type="button" onClick={() => openEdit(r)}
            className="rounded-panel border border-border px-2.5 py-1 text-xs font-medium text-ink transition hover:bg-surface-alt">
            {t('edit')}
          </button>
          <button type="button" onClick={() => handleToggleActive(r)}
            className={`rounded-panel border px-2.5 py-1 text-xs font-medium transition ${
              r.isActive
                ? 'border-warning-soft text-warning hover:bg-warning-soft/20'
                : 'border-success-soft text-success hover:bg-success-soft/20'
            }`}>
            {r.isActive ? t('deactivate') : t('activate')}
          </button>
        </div>
      )
    },
  ]

  return (
    <AdminPagePanel>
      <AdminPagePanel.Header
        title={t('title')}
        subtitle={t('subtitle')}
        actions={
          <button type="button" onClick={openCreate}
            className="flex items-center gap-1.5 rounded-panel bg-accent px-3 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-accent-strong">
            <Plus size={15} /> {t('new_station')}
          </button>
        }
      />

      <AdminPagePanel.Toolbar>
        <input
          type="text"
          placeholder={t('search_placeholder')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="rounded-panel border border-border bg-surface px-3 py-1.5 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none max-w-[320px]"
        />
        {search && (
          <span className="text-xs text-muted">
            {t('showing', { shown: filteredStations.length, total: stations.length })}
          </span>
        )}
      </AdminPagePanel.Toolbar>

      {formOpen && (
        <div className="rounded-panel border border-border bg-surface-soft p-4">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-sm font-semibold text-ink">{editingStation ? t('edit_title') : t('new_title')}</h3>
            <button type="button" onClick={closeForm} className="text-muted hover:text-ink"><X size={16} /></button>
          </div>
          <form onSubmit={handleSave} className="grid gap-3 sm:grid-cols-2">
            <label className="block">
              <span className="text-xs text-muted">{t('field_code')}</span>
              <input required className={`${SELECT_CLS} mt-1 w-full`} {...field('code')} />
            </label>
            <label className="block">
              <span className="text-xs text-muted">{t('field_name')}</span>
              <input required className={`${SELECT_CLS} mt-1 w-full`} {...field('name')} />
            </label>
            <label className="block sm:col-span-2">
              <span className="text-xs text-muted">{t('field_address')}</span>
              <input className={`${SELECT_CLS} mt-1 w-full`} {...field('address')} />
            </label>
            <label className="block">
              <span className="text-xs text-muted">{t('field_lat')}</span>
              <input required type="number" step="any" className={`${SELECT_CLS} mt-1 w-full`} {...field('latitude')} />
            </label>
            <label className="block">
              <span className="text-xs text-muted">{t('field_lng')}</span>
              <input required type="number" step="any" className={`${SELECT_CLS} mt-1 w-full`} {...field('longitude')} />
            </label>
            <label className="flex items-center gap-2 self-end pb-1.5">
              <input type="checkbox" checked={form.isActive}
                onChange={(e) => setForm((f) => ({ ...f, isActive: e.target.checked }))} />
              <span className="text-sm text-ink">{t('field_active')}</span>
            </label>
            <div className="flex gap-2 sm:col-span-2">
              <button type="submit" disabled={saving}
                className="rounded-panel bg-accent px-4 py-1.5 text-sm font-medium text-white shadow-sm transition hover:bg-accent-strong disabled:opacity-60">
                {saving ? t('saving') : t('save')}
              </button>
              <button type="button" onClick={closeForm}
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
        rows={filteredStations}
        page={0}
        totalPages={1}
        onPageChange={() => {}}
        loading={loading}
      />
    </AdminPagePanel>
  )
}

export default AdminStationsPage
