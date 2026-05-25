import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { gatewayClient } from '../services/gatewayClient'
import { PanelCard } from '../components/common/PanelCard'
import { ErrorAlert } from '../components/common/Alerts'

const STATUS_STEPS = ['RECEIVED', 'IN_PROGRESS', 'RESOLVED']

const STATUS_BADGE = {
  RECEIVED:    'bg-yellow-100 text-yellow-700 border-yellow-300 dark:bg-yellow-900/30 dark:text-yellow-400 dark:border-yellow-800',
  IN_PROGRESS: 'bg-blue-100   text-blue-700   border-blue-300   dark:bg-blue-900/30   dark:text-blue-400   dark:border-blue-800',
  RESOLVED:    'bg-green-100  text-green-700  border-green-300  dark:bg-green-900/30  dark:text-green-400  dark:border-green-800',
}

const STATUS_CIRCLE = {
  done:    'bg-accent border-accent text-white',
  current: 'border-accent bg-surface text-accent',
  pending: 'border-border bg-surface text-muted',
}

function Field({ label, children }) {
  return (
    <div>
      <dt className="text-xs text-muted">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium text-ink">{children ?? '—'}</dd>
    </div>
  )
}

function StatusTimeline({ status, t }) {
  const current = STATUS_STEPS.indexOf(status)
  return (
    <div className="flex items-center gap-0">
      {STATUS_STEPS.map((step, i) => {
        const state = i < current ? 'done' : i === current ? 'current' : 'pending'
        return (
          <div key={step} className="flex flex-1 items-center">
            <div className="flex flex-col items-center gap-1">
              <div className={`flex h-7 w-7 items-center justify-center rounded-full border-2 text-xs font-bold transition-colors ${STATUS_CIRCLE[state]}`}>
                {state === 'done' ? '✓' : i + 1}
              </div>
              <span className={`text-[10px] font-medium whitespace-nowrap ${state === 'pending' ? 'text-muted' : 'text-ink'}`}>
                {t(`status_${step.toLowerCase()}`)}
              </span>
            </div>
            {i < STATUS_STEPS.length - 1 && (
              <div className={`h-0.5 flex-1 mx-1 mb-4 transition-colors ${i < current ? 'bg-accent' : 'bg-border'}`} />
            )}
          </div>
        )
      })}
    </div>
  )
}

export function MyReportDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { t } = useTranslation('my-reports')

  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [lightboxSrc, setLightboxSrc] = useState(null)

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const r = await gatewayClient.getMyReportById(id)
        setReport(r)
      } catch (err) {
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  if (loading) {
    return <p className="text-sm text-muted">Loading…</p>
  }

  return (
    <div className="max-w-2xl space-y-5">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate('/my-reports')}
          className="flex items-center gap-1.5 rounded-panel border border-border px-3 py-1.5 text-sm text-muted hover:bg-surface-alt hover:text-ink"
        >
          <ArrowLeft size={14} aria-hidden="true" />
          {t('back')}
        </button>
        <h2 className="text-xl font-semibold text-ink">{t('report_prefix', { id })}</h2>
      </div>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      {report && (
        <>
          <PanelCard tone="soft">
            <p className="mb-4 text-sm font-medium text-ink">{t('status_label')}</p>
            <StatusTimeline status={report.status} t={t} />
            <div className="mt-4 flex items-center gap-2">
              <span className={`rounded-full border px-3 py-1 text-xs font-semibold ${STATUS_BADGE[report.status] ?? ''}`}>
                {t(`status_${report.status?.toLowerCase()}`)}
              </span>
              {report.updatedAt && (
                <span className="text-xs text-muted">
                  {t('last_updated', { date: new Date(report.updatedAt).toLocaleString() })}
                </span>
              )}
            </div>
          </PanelCard>

          <PanelCard tone="default">
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4">
              <Field label={t('col_date')}>
                {report.createdAt ? new Date(report.createdAt).toLocaleString() : null}
              </Field>
              <Field label={t('col_category')}>
                {report.category?.toLowerCase().replace('_', ' ')}
              </Field>
              <Field label={t('col_line_id')}>{report.lineId}</Field>
              <Field label={t('col_station_id')}>{report.stationId}</Field>
              <Field label={t('col_vehicle_reg')}>{report.vehicleRegistrationNumber}</Field>
              <Field label={t('col_vehicle_id')}>{report.vehicleId}</Field>
              <div className="col-span-2">
                <dt className="text-xs text-muted">{t('col_description')}</dt>
                <dd className="mt-1 whitespace-pre-wrap text-sm text-ink">{report.description ?? '—'}</dd>
              </div>
            </dl>
          </PanelCard>

          {report.photoUrls?.length > 0 && (
            <PanelCard tone="default">
              <p className="mb-3 text-sm font-medium text-ink">
                {t('photos_label')}
                <span className="ml-1 text-xs text-muted">{t('click_to_enlarge', { count: report.photoUrls.length })}</span>
              </p>
              <div className="flex flex-wrap gap-3">
                {report.photoUrls.map((src, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => setLightboxSrc(src)}
                    className="overflow-hidden rounded-panel border border-border transition-opacity hover:opacity-80"
                  >
                    <img
                      src={src}
                      alt={t('photo_prefix', { num: i + 1 })}
                      className="h-36 w-36 object-cover"
                    />
                  </button>
                ))}
              </div>
            </PanelCard>
          )}
        </>
      )}

      {lightboxSrc && (
        <>
          <button
            type="button"
            aria-label={t('close_image')}
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
              alt={t('full_size')}
              className="max-h-[85vh] max-w-[90vw] rounded-panel object-contain shadow-xl"
            />
          </div>
        </>
      )}
    </div>
  )
}

export default MyReportDetailPage
