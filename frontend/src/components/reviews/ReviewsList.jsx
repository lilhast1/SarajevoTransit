import { useCallback, useEffect, useState } from 'react'
import { Loader2, ChevronLeft, ChevronRight, MessageSquare, User } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { StarRating } from './StarRating'
import { transitApi } from '../../services/transitApi'
import { gatewayClient } from '../../services/gatewayClient'

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

export function ReviewsList({ lineId, refreshKey = 0 }) {
  const { t } = useTranslation('lines')
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [reviewerNames, setReviewerNames] = useState({})

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await transitApi.getReviewsByLine(lineId, page)
      setData(res)

      const ids = [...new Set((res.content ?? []).map((r) => r.reviewerUserId).filter(Boolean))]
      const entries = await Promise.all(
        ids.map((id) =>
          gatewayClient.getUserById(id)
            .then((u) => [id, u.fullName ?? u.email ?? `#${id}`])
            .catch(() => [id, `#${id}`])
        )
      )
      setReviewerNames((prev) => ({ ...prev, ...Object.fromEntries(entries) }))
    } catch (err) {
      setError(err.message || t('review_load_failed'))
    } finally {
      setLoading(false)
    }
  }, [lineId, page, refreshKey, t])

  useEffect(() => { load() }, [load])

  const reviews = data.content ?? []

  if (loading) {
    return (
      <div className="flex items-center justify-center gap-2 py-8 text-muted">
        <Loader2 size={16} className="animate-spin" />
        <span className="text-sm">{t('loading')}</span>
      </div>
    )
  }

  if (error) {
    return <p className="py-4 text-center text-sm text-red-500">{error}</p>
  }

  if (reviews.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 py-8 text-center">
        <MessageSquare size={28} className="text-muted" />
        <p className="text-sm text-muted">{t('review_no_reviews')}</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      <div className="divide-y divide-border rounded-lg border border-border bg-surface">
        {reviews.map((r) => (
          <div key={r.id} className="px-4 py-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <StarRating value={r.rating} readOnly size={14} />
              <span className="text-xs text-muted">{formatDate(r.createdAt)}</span>
            </div>
            {r.reviewText && (
              <p className="mt-1.5 text-sm text-ink">{r.reviewText}</p>
            )}
            <div className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-0.5 text-[11px] text-muted">
              <span className="inline-flex items-center gap-1">
                <User size={11} aria-hidden="true" />
                {reviewerNames[r.reviewerUserId] ?? `#${r.reviewerUserId}`}
              </span>
              <span>{t('review_ride_date')}: {r.rideDate}</span>
            </div>
          </div>
        ))}
      </div>

      {data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="inline-flex items-center rounded-lg border border-border p-1.5 text-muted transition hover:bg-surface-alt disabled:opacity-40"
            aria-label="Previous page"
          >
            <ChevronLeft size={14} />
          </button>
          <span className="text-xs text-muted">{page + 1} / {data.totalPages}</span>
          <button
            type="button"
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="inline-flex items-center rounded-lg border border-border p-1.5 text-muted transition hover:bg-surface-alt disabled:opacity-40"
            aria-label="Next page"
          >
            <ChevronRight size={14} />
          </button>
        </div>
      )}
    </div>
  )
}
