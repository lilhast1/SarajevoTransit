import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { gatewayClient } from '../services/gatewayClient'
import { useAppContext } from '../context/AppContext'

const categories = [
  'BREAKDOWN',
  'CROWDING',
  'HYGIENE',
  'AGGRESSIVE_BEHAVIOR',
  'DELAY',
  'OTHER',
]

export function ReportProblemPage() {
  const navigate = useNavigate()
  const { session } = useAppContext()
  const [userId, setUserId] = useState(session?.userId || null)
  const [lineId, setLineId] = useState('')
  const [stationId, setStationId] = useState('')
  const [vehicleReg, setVehicleReg] = useState('')
  const [category, setCategory] = useState('OTHER')
  const [description, setDescription] = useState('')
  const [photoUrls, setPhotoUrls] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!userId) {
      // try fetch current user to obtain id
      gatewayClient
        .getCurrentUser()
        .then((u) => setUserId(u.id))
        .catch(() => {})
    }
  }, [userId])

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const payload = {
        reporterUserId: userId,
        lineId: lineId ? Number(lineId) : null,
        stationId: stationId ? Number(stationId) : null,
        vehicleRegistrationNumber: vehicleReg || null,
        category,
        description,
        photoUrls: photoUrls
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean),
      }

      await gatewayClient.createProblemReport(payload)
      navigate('/profile')
    } catch (err) {
      setError(err.message || String(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-2xl">
      <h2 className="text-lg font-semibold">Prijavi problem</h2>
      <p className="text-sm text-muted">Prijavi kvar, gužvu ili vanrednu situaciju.</p>

      <form className="mt-4 grid gap-3" onSubmit={handleSubmit}>
        <label className="block">
          <span className="text-sm text-muted">Kategorija</span>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full rounded-panel border px-3 py-2"
          >
            {categories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span className="text-sm text-muted">Opis</span>
          <textarea
            required
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={5}
            maxLength={1000}
            className="w-full rounded-panel border p-3"
          />
        </label>

        <div className="grid grid-cols-2 gap-2">
          <label>
            <span className="text-sm text-muted">Broj linije (opcionalno)</span>
            <input
              value={lineId}
              onChange={(e) => setLineId(e.target.value)}
              type="number"
              className="w-full rounded-panel border px-3 py-2"
            />
          </label>

          <label>
            <span className="text-sm text-muted">ID stanice (opcionalno)</span>
            <input
              value={stationId}
              onChange={(e) => setStationId(e.target.value)}
              type="number"
              className="w-full rounded-panel border px-3 py-2"
            />
          </label>
        </div>

        <label>
          <span className="text-sm text-muted">Registracija vozila (opcionalno)</span>
          <input
            value={vehicleReg}
            onChange={(e) => setVehicleReg(e.target.value)}
            className="w-full rounded-panel border px-3 py-2"
          />
        </label>

        <label>
          <span className="text-sm text-muted">Fotografije (URL, odvojeni zarezom)</span>
          <input
            value={photoUrls}
            onChange={(e) => setPhotoUrls(e.target.value)}
            className="w-full rounded-panel border px-3 py-2"
          />
        </label>

        {error ? <div className="text-error">{error}</div> : null}

        <div className="flex gap-2">
          <button
            type="submit"
            disabled={submitting}
            className="rounded-panel border border-accent bg-accent px-4 py-2 text-white"
          >
            {submitting ? 'Šaljem...' : 'Pošalji prijavu'}
          </button>
          <button type="button" onClick={() => navigate(-1)} className="rounded-panel border px-4 py-2">
            Otkaži
          </button>
        </div>
      </form>
    </div>
  )
}

export default ReportProblemPage
