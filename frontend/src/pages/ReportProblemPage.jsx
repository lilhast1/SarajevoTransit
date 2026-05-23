import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { gatewayClient } from '../services/gatewayClient'
import { useAppContext } from '../context/AppContext'
import { SearchableSelect } from '../components/common/SearchableSelect'
import { PhotoUpload } from '../components/common/PhotoUpload'
import { VEHICLE_TYPE_META_BY_ID } from '../constants/vehicleColors'

const VEHICLE_TYPES = Object.values(VEHICLE_TYPE_META_BY_ID)

const CATEGORIES = [
  { value: 'BREAKDOWN', label: 'Breakdown' },
  { value: 'CROWDING', label: 'Crowding' },
  { value: 'HYGIENE', label: 'Hygiene' },
  { value: 'AGGRESSIVE_BEHAVIOR', label: 'Aggressive Behavior' },
  { value: 'DELAY', label: 'Delay' },
  { value: 'OTHER', label: 'Other' },
]

const INPUT_CLS = 'w-full rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none'
const LABEL_CLS = 'block text-sm text-muted'

export function ReportProblemPage() {
  const navigate = useNavigate()
  const { session } = useAppContext()

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
      setError('You must be logged in to submit a report.')
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
      <h2 className="text-lg font-semibold text-ink">Report Problem</h2>
      <p className="mt-1 text-sm text-muted">Report a breakdown, crowding, or other unusual situation.</p>

      <form className="mt-4 grid gap-4" onSubmit={handleSubmit}>
        <label className="block">
          <span className={LABEL_CLS}>Category</span>
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
          <span className={LABEL_CLS}>Description</span>
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
          <span className={LABEL_CLS}>Vehicle type (optional)</span>
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
              All
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
            <span className={LABEL_CLS}>Line (optional)</span>
            <div className="mt-1">
              <SearchableSelect
                value={selectedLine}
                onChange={setSelectedLine}
                options={lines}
                getLabel={(l) => `${l.code} – ${l.name}`}
                getValue={(l) => l.id}
                placeholder="Select line…"
              />
            </div>
          </div>

          <div>
            <span className={LABEL_CLS}>
              Station (optional){selectedLine ? ` — ${lineStations.length} stops` : ''}
            </span>
            <div className="mt-1">
              {selectedLine ? (
                <SearchableSelect
                  value={selectedStation}
                  onChange={setSelectedStation}
                  options={lineStations}
                  getLabel={(s) => s.name}
                  getValue={(s) => s.id}
                  placeholder="Select stop…"
                />
              ) : (
                <SearchableSelect
                  value={selectedStation}
                  onChange={setSelectedStation}
                  loadOptions={loadStations}
                  getLabel={(s) => s.name}
                  getValue={(s) => s.id}
                  placeholder="Search station…"
                />
              )}
            </div>
          </div>
        </div>

        <label className="block">
          <span className={LABEL_CLS}>Vehicle Registration (optional)</span>
          <input
            value={vehicleReg}
            onChange={(e) => setVehicleReg(e.target.value)}
            className={`mt-1 ${INPUT_CLS}`}
            placeholder="e.g. A12-E-345"
          />
        </label>

        <div>
          <span className={LABEL_CLS}>Photos (optional)</span>
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
            {submitting ? 'Submitting…' : 'Submit Report'}
          </button>
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="rounded-panel border border-border bg-surface px-4 py-2 text-sm text-ink hover:bg-surface-alt"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}

export default ReportProblemPage
