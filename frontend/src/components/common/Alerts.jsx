/**
 * Error Alert Component
 * Displays error messages in a user-friendly way
 */

import { AlertCircle, X } from 'lucide-react'

export function ErrorAlert({ error, onDismiss, className = '' }) {
  if (!error) return null

  const message = typeof error === 'string' ? error : error.message || 'An error occurred'

  return (
    <div
      className={`flex items-center gap-3 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-800 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-100 ${className}`}
      role="alert"
    >
      <AlertCircle size={18} className="flex-shrink-0" />
      <div className="flex-1">{message}</div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="flex-shrink-0 text-red-600 hover:text-red-800 dark:text-red-400"
          aria-label="Dismiss error"
        >
          <X size={16} />
        </button>
      )}
    </div>
  )
}

/**
 * Success Alert Component
 */
export function SuccessAlert({ message, onDismiss, className = '' }) {
  if (!message) return null

  return (
    <div
      className={`flex items-center gap-3 rounded-lg border border-green-300 bg-green-50 p-3 text-sm text-green-800 dark:border-green-900/50 dark:bg-green-900/20 dark:text-green-100 ${className}`}
      role="status"
    >
      <div className="flex-1">{message}</div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="flex-shrink-0 text-green-600 hover:text-green-800 dark:text-green-400"
          aria-label="Dismiss message"
        >
          <X size={16} />
        </button>
      )}
    </div>
  )
}
