import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { gatewayClient } from '../services/gatewayClient'
import { useAppContext } from '../context/AppContext'
import { SearchableSelect } from '../components/common/SearchableSelect'
import { PhotoUpload } from '../components/common/PhotoUpload'
import { VEHICLE_TYPE_META_BY_ID } from '../constants/vehicleColors'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)

const INPUT_CLS = 'w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none'
const LABEL_CLS = 'block text-sm text-muted'

export function ReportProblemPage() {
  const navigate = useNavigate()
  const { t } = useTranslation('report')
  const { session } = useAppContext()

  const CATEGORIES = [
    { value: 'BREAKDOWN', label: t('cat_breakdown') },
    { value: 'CROWDING', label: t('cat_crowding') },
    { value: 'HYGIENE', label: t('cat_hygiene') },
    { value: 'AGGRESSIVE_BEHAVIOR', label: t('cat_aggressive') },
    { value: 'DELAY', label: t('cat_delay') },
    { value: 'OTHER', label: t('cat_other') },
  ]

  const [vehicleTypeId, setVehicleTypeId] = useState(null)
  const [lines, setLines] = useState([])
  const [selectedLine, setSelectedLine] = useState(null)
  const [lineStations, setLineStations] = useState([])
  const [selectedStation, setSelectedStation] = useState(null)
  const [vehicleReg, setVehicleReg] = useState('')
  const [category, setCategory] = useState('OTHER')
  const [description, setDescription] = useState('')
  const [photos, setPhotos] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    const q = vehicleTypeId
      ? `?vehicleTypeId=${vehicleTypeId}&activeOnly=true`
      : '?activeOnly=true'
    setSelectedLine(null)
    setLineStations([])
    setSelectedStation(null)
    gatewayClient.getLines(q).then(setLines).catch(() => {})
  }, [vehicleTypeId])

  // When a line is selected, load its stations from all directions
  useEffect(() => {
    if (!selectedLine) {
      setLineStations([])
      setSelectedStation(null)
      return
    }
    gatewayClient.getDirections(`?lineId=${selectedLine.id}&activeOnly=true`)
      .then(async (directions) => {
        const all = await Promise.all(
          directions.map((d) => gatewayClient.getDirectionStations(d.id).catch(() => []))
        )
        const seen = new Set()
        const unique = []
        all.flat().forEach((s) => {
          if (!seen.has(s.stationId)) {
            seen.add(s.stationId)
            unique.push({ id: s.stationId, name: s.stationName })
          }
        })
        setLineStations(unique)
        setSelectedStation(null)
      })
      .catch(() => setLineStations([]))
  }, [selectedLine])

  async function loadStations(query) {
    if (!query || query.length < 2) return []
    try {
      const params = new URLSearchParams({ name: query, activeOnly: true })
      return await gatewayClient.getStations(`?${params}`)
    } catch {
      return []
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)

    if (!session?.userId) {
      setError(t('login_required'))
      return
    }

    setSubmitting(true)
    try {
      await gatewayClient.createProblemReport({
        reporterUserId: session.userId,
        lineId: selectedLine ? selectedLine.id : null,
        stationId: selectedStation ? selectedStation.id : null,
        vehicleRegistrationNumber: vehicleReg || null,
        category,
        description,
        photoUrls: photos,
      })
      navigate('/profile')
    } catch (err) {
      setError(err.message || String(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-2xl">
      <h2 className="text-lg font-semibold text-ink">{t('title')}</h2>
      <p className="mt-1 text-sm text-muted">{t('subtitle')}</p>

      <form className="mt-4 grid gap-4" onSubmit={handleSubmit}>
        <label className="block">
          <span className={LABEL_CLS}>{t('category')}</span>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className={INPUT_CLS}
          >
            {CATEGORIES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className={LABEL_CLS}>{t('description')}</span>
          <textarea
            required
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={4}
            maxLength={1000}
            className={INPUT_CLS}
          />
        </label>

        <div>
          <span className={LABEL_CLS}>{t('vehicle_type')}</span>
          <div className="mt-1 flex flex-wrap gap-1.5">
            <button
              type="button"
              onClick={() => setVehicleTypeId(null)}
              className={`rounded-panel border px-3 py-1 text-xs font-medium transition ${
                vehicleTypeId === null
                  ? 'border-accent bg-accent text-white'
                  : 'border-border text-muted hover:bg-surface-alt'
              }`}
            >
              {t('all')}
            </button>
            {VEHICLE_TYPES.map((vt) => (
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
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <span className={LABEL_CLS}>{t('line')}</span>
            <div className="mt-1">
              <SearchableSelect
                value={selectedLine}
                onChange={setSelectedLine}
                options={lines}
                getLabel={(l) => `${l.code} – ${l.name}`}
                getValue={(l) => l.id}
                placeholder={t('select_line')}
              />
            </div>
          </div>

          <div>
            <span className={LABEL_CLS}>
              {t('station')}{selectedLine ? ` ${t('stops_count', { count: lineStations.length })}` : ''}
            </span>
            <div className="mt-1">
              {selectedLine ? (
                <SearchableSelect
                  value={selectedStation}
                  onChange={setSelectedStation}
                  options={lineStations}
                  getLabel={(s) => s.name}
                  getValue={(s) => s.id}
                  placeholder={t('select_stop')}
                />
              ) : (
                <SearchableSelect
                  value={selectedStation}
                  onChange={setSelectedStation}
                  loadOptions={loadStations}
                  getLabel={(s) => s.name}
                  getValue={(s) => s.id}
                  placeholder={t('search_station')}
                />
              )}
            </div>
          </div>
        </div>

        <label className="block">
          <span className={LABEL_CLS}>{t('vehicle_reg')}</span>
          <input
            value={vehicleReg}
            onChange={(e) => setVehicleReg(e.target.value)}
            className={`mt-1 ${INPUT_CLS}`}
            placeholder={t('vehicle_reg_placeholder')}
          />
        </label>

        <div>
          <span className={LABEL_CLS}>{t('photos')}</span>
          <div className="mt-1">
            <PhotoUpload photos={photos} onChange={setPhotos} />
          </div>
        </div>

        {error && <div className="rounded-panel border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">{error}</div>}

        <div className="flex gap-2">
          <button
            type="submit"
            disabled={submitting}
            className="rounded-panel border border-accent bg-accent px-4 py-2 text-sm text-white disabled:opacity-60"
          >
            {submitting ? t('submitting') : t('submit')}
          </button>
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="rounded-panel border border-border bg-surface px-4 py-2 text-sm text-ink hover:bg-surface-alt"
          >
            {t('cancel')}
          </button>
        </div>
      </form>
    </div>
  )
}

export default ReportProblemPage
