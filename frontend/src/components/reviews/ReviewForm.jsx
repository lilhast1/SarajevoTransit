import { useState } from 'react'
import { Loader2, CheckCircle, AlertCircle } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { StarRating } from './StarRating'
import { transitApi } from '../../services/transitApi'

export function ReviewForm({ lineId, reviewerUserId, onSuccess }) {
  const { t } = useTranslation('lines')
  const today = new Date().toISOString().split('T')[0]

  const [rating, setRating] = useState(0)
  const [reviewText, setReviewText] = useState('')
  const [rideDate, setRideDate] = useState(today)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!rating) return
    setLoading(true)
    setResult(null)
    try {
      await transitApi.createReview({
        reviewerUserId,
        lineId,
        rating,
        reviewText: reviewText.trim() || null,
        rideDate,
      })
      setResult('success')
      setRating(0)
      setReviewText('')
      setRideDate(today)
      onSuccess?.()
    } catch (err) {
      setResult({ type: 'error', message: err.message || t('review_submit_failed') })
    } finally {
      setLoading(false)
    }
  }

  if (result === 'success') {
    return (
      <div className="flex items-center gap-2 rounded-lg border border-green-200 bg-green-50 px-4 py-3 dark:border-green-900/40 dark:bg-green-950/30">
        <CheckCircle size={16} className="flex-shrink-0 text-green-600 dark:text-green-400" />
        <p className="text-sm text-green-700 dark:text-green-400">{t('review_submitted')}</p>
        <button
          type="button"
          onClick={() => setResult(null)}
          className="ml-auto text-xs text-green-600 underline-offset-2 hover:underline dark:text-green-400"
        >
          {t('review_write_another')}
        </button>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      {result?.type === 'error' && (
        <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 dark:border-red-900/40 dark:bg-red-950/30">
          <AlertCircle size={14} className="mt-0.5 flex-shrink-0 text-red-500" />
          <p className="text-sm text-red-600 dark:text-red-400">{result.message}</p>
        </div>
      )}

      <div className="space-y-1">
        <p className="text-xs font-medium text-muted">{t('review_your_rating')}</p>
        <StarRating value={rating} onChange={setRating} size={24} />
        {!rating && (
          <p className="text-xs text-muted">{t('review_select_rating')}</p>
        )}
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium text-muted" htmlFor="review-text">
          {t('review_comment_label')}
        </label>
        <textarea
          id="review-text"
          value={reviewText}
          onChange={(e) => setReviewText(e.target.value)}
          placeholder={t('review_text_placeholder')}
          maxLength={1500}
          rows={3}
          className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring dark:[color-scheme:dark]"
        />
        <p className="text-right text-[11px] text-muted">{reviewText.length}/1500</p>
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium text-muted" htmlFor="ride-date">
          {t('review_ride_date')}
        </label>
        <input
          id="ride-date"
          type="date"
          value={rideDate}
          max={today}
          onChange={(e) => setRideDate(e.target.value)}
          className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink outline-none ring-accent/30 focus:ring dark:[color-scheme:dark]"
        />
      </div>

      <p className="text-xs text-muted">{t('review_update_note')}</p>

      <button
        type="submit"
        disabled={loading || !rating}
        className="inline-flex items-center gap-2 rounded-panel border border-accent bg-accent px-4 py-2 text-sm font-medium text-white transition hover:bg-accent/90 disabled:opacity-50"
      >
        {loading ? (
          <>
            <Loader2 size={14} className="animate-spin" />
            {t('review_submitting')}
          </>
        ) : t('review_submit')}
      </button>
    </form>
  )
}
