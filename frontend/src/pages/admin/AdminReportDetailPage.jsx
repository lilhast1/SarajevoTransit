import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, X } from 'lucide-react'
import { gatewayClient } from '../../services/gatewayClient'
import { ErrorAlert } from '../../components/common/Alerts'

const STATUS_BADGE = {
  RECEIVED: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  IN_PROGRESS: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  RESOLVED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
}

function Field({ label, children }) {
  return (
    <div>
      <dt className="text-xs text-muted">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium text-ink">{children ?? '—'}</dd>
    </div>
  )
}

export function AdminReportDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [report, setReport] = useState(null)
  const [reporterName, setReporterName] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [lightboxSrc, setLightboxSrc] = useState(null)
  const [statusSaving, setStatusSaving] = useState(false)

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const r = await gatewayClient.getReportById(id)
        setReport(r)
        if (r.reporterUserId) {
          gatewayClient.getUserById(r.reporterUserId)
            .then((u) => setReporterName(u.fullName ?? null))
            .catch(() => {})
        }
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  async function handleStatusChange(status) {
    setStatusSaving(true)
    try {
      await gatewayClient.updateReportStatus(id, status)
      setReport((r) => ({ ...r, status }))
    } catch (err) {
      setError(err.message)
    } finally {
      setStatusSaving(false)
    }
  }

  if (loading) {
    return <p className="text-sm text-muted">Loading…</p>
  }

  return (
    <div className="max-w-2xl space-y-5">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate('/admin/reports')}
          className="flex items-center gap-1.5 rounded-panel border border-border px-3 py-1.5 text-sm text-muted hover:bg-surface-alt hover:text-ink"
        >
          <ArrowLeft size={14} />
          Back
        </button>
        <h2 className="text-xl font-semibold text-ink">Report #{id}</h2>
      </div>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      {report && (
        <>
          <div className="rounded-panel border border-border bg-surface p-5">
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4">
              <Field label="Date">
                {report.createdAt ? new Date(report.createdAt).toLocaleString() : null}
              </Field>
              <Field label="Category">{report.category}</Field>

              <Field label="Reporter">
                {reporterName
                  ? reporterName
                  : report.reporterUserId
                    ? `User #${report.reporterUserId}`
                    : null}
              </Field>
              <div>
                <dt className="text-xs text-muted">Status</dt>
                <dd className="mt-0.5 flex items-center gap-2">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[report.status] ?? ''}`}>
                    {report.status}
                  </span>
                  <select
                    value={report.status}
                    onChange={(e) => handleStatusChange(e.target.value)}
                    disabled={statusSaving}
                    className="rounded border border-border bg-surface px-2 py-0.5 text-xs text-ink disabled:opacity-60"
                  >
                    {['RECEIVED', 'IN_PROGRESS', 'RESOLVED'].map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </dd>
              </div>

              <Field label="Line ID">{report.lineId}</Field>
              <Field label="Station ID">{report.stationId}</Field>
              <Field label="Vehicle Registration">{report.vehicleRegistrationNumber}</Field>
              <Field label="Vehicle ID">{report.vehicleId}</Field>

              <div className="col-span-2">
                <dt className="text-xs text-muted">Description</dt>
                <dd className="mt-1 text-sm text-ink whitespace-pre-wrap">{report.description ?? '—'}</dd>
              </div>
            </dl>
          </div>

          {report.photoUrls?.length > 0 && (
            <div className="rounded-panel border border-border bg-surface p-5">
              <p className="mb-3 text-sm font-medium text-ink">
                Photos <span className="text-xs text-muted">({report.photoUrls.length}) — click to enlarge</span>
              </p>
              <div className="flex flex-wrap gap-3">
                {report.photoUrls.map((src, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => setLightboxSrc(src)}
                    className="overflow-hidden rounded-panel border border-border hover:opacity-80 transition-opacity"
                  >
                    <img
                      src={src}
                      alt={`Photo ${i + 1}`}
                      className="h-36 w-36 object-cover"
                    />
                  </button>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      {lightboxSrc && (
        <>
          <button
            type="button"
            aria-label="Close image"
            className="fixed inset-0 z-[1300] bg-black/85"
            onClick={() => setLightboxSrc(null)}
          />
          <div className="fixed left-1/2 top-1/2 z-[1400] -translate-x-1/2 -translate-y-1/2">
            <button
              type="button"
              onClick={() => setLightboxSrc(null)}
              className="absolute -right-3 -top-3 flex h-7 w-7 items-center justify-center rounded-full border border-border bg-surface text-ink shadow-lg"
            >
              <X size={14} />
            </button>
            <img
              src={lightboxSrc}
              alt="Full size"
              className="max-h-[85vh] max-w-[90vw] rounded-panel object-contain shadow-xl"
            />
          </div>
        </>
      )}
    </div>
  )
}

export default AdminReportDetailPage
