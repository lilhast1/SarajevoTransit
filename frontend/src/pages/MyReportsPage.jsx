import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, ChevronRight, ClipboardList, Loader2 } from 'lucide-react'
import { gatewayClient } from '../services/gatewayClient'
import { useAppContext } from '../context/AppContext'
import { PanelCard } from '../components/common/PanelCard'
import { ErrorAlert } from '../components/common/Alerts'

const STATUS_BADGE = {
  RECEIVED:    'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  IN_PROGRESS: 'bg-blue-100   text-blue-700   dark:bg-blue-900/30   dark:text-blue-400',
  RESOLVED:    'bg-green-100  text-green-700  dark:bg-green-900/30  dark:text-green-400',
}

export function MyReportsPage() {
  const { session } = useAppContext()
  const { t } = useTranslation('my-reports')
  const navigate = useNavigate()

  const [reports, setReports] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchPage = useCallback(async (p) => {
    if (!session?.userId) return
    setLoading(true)
    setError(null)
    try {
      const data = await gatewayClient.getMyReports(session.userId, p)
      setReports(data.content ?? [])
      setTotalPages(data.totalPages ?? 1)
      setTotalElements(data.totalElements ?? 0)
      setPage(data.number ?? p)
    } catch (err) {
      setError(err.message || t('load_error'))
    } finally {
      setLoading(false)
    }
  }, [session?.userId, t])

  useEffect(() => { fetchPage(0) }, [fetchPage])

  return (
    <div className="max-w-3xl space-y-4">
      <PanelCard tone="soft">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-ink">{t('title')}</h2>
            <p className="mt-1 text-sm text-muted">
              {totalElements === 1 ? t('count_one') : t('count_other', { count: totalElements })}
            </p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/report')}
            className="rounded-panel border border-accent bg-accent px-3 py-2 text-sm font-medium text-white hover:bg-accent/90"
          >
            {t('new_report')}
          </button>
        </div>
      </PanelCard>

      <ErrorAlert error={error} onDismiss={() => setError(null)} />

      <PanelCard tone="default">
        {loading && reports.length === 0 ? (
          <div className="flex items-center justify-center py-12 text-muted">
            <Loader2 size={20} className="animate-spin" />
          </div>
        ) : reports.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-3 py-12 text-center">
            <ClipboardList size={32} className="text-muted" />
            <p className="text-sm font-medium text-ink">{t('empty_title')}</p>
            <p className="text-xs text-muted">{t('empty_desc')}</p>
            <button
              type="button"
              onClick={() => navigate('/report')}
              className="mt-1 rounded-panel border border-accent bg-accent px-3 py-1.5 text-sm text-white hover:bg-accent/90"
            >
              {t('new_report')}
            </button>
          </div>
        ) : (
          <div className="divide-y divide-border">
            {reports.map((r) => (
              <button
                key={r.id}
                type="button"
                onClick={() => navigate(`/my-reports/${r.id}`)}
                className="flex w-full items-start gap-4 px-4 py-4 text-left transition hover:bg-surface-alt"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium text-ink">
                      {t('report_prefix', { id: r.id })}
                    </span>
                    <span className="text-xs text-muted capitalize">
                      {r.category?.toLowerCase().replace('_', ' ')}
                    </span>
                  </div>
                  <p className="mt-1 line-clamp-2 text-sm text-muted">{r.description}</p>
                  <p className="mt-1 text-xs text-muted">
                    {r.createdAt ? new Date(r.createdAt).toLocaleString() : ''}
                  </p>
                </div>
                <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_BADGE[r.status] ?? 'bg-gray-100 text-gray-700'}`}>
                  {t(`status_${r.status?.toLowerCase()}`)}
                </span>
              </button>
            ))}
          </div>
        )}

        {!loading && totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-border px-4 py-3">
            <button
              type="button"
              onClick={() => fetchPage(page - 1)}
              disabled={page <= 0}
              className="inline-flex items-center gap-1 rounded-panel border border-border px-3 py-1.5 text-sm text-ink hover:bg-surface-alt disabled:opacity-40"
            >
              <ChevronLeft size={14} />
              {t('previous')}
            </button>
            <span className="text-xs text-muted">{t('page_of', { page: page + 1, total: totalPages })}</span>
            <button
              type="button"
              onClick={() => fetchPage(page + 1)}
              disabled={page >= totalPages - 1}
              className="inline-flex items-center gap-1 rounded-panel border border-border px-3 py-1.5 text-sm text-ink hover:bg-surface-alt disabled:opacity-40"
            >
              {t('next')}
              <ChevronRight size={14} />
            </button>
          </div>
        )}
      </PanelCard>
    </div>
  )
}

export default MyReportsPage
