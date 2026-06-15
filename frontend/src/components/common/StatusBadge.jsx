const STATUS_VARIANTS = {
  OPERATIONAL: 'success',
  IN_MAINTENANCE: 'warning',
  OUT_OF_SERVICE: 'danger',
  RETIRED: 'muted',
  ACTIVE: 'success',
  INACTIVE: 'muted',

  RECEIVED: 'warning',
  IN_PROGRESS: 'info',
  RESOLVED: 'success',

  VISIBLE: 'success',
  HIDDEN: 'muted',

  GENERAL: 'info',
  DELAY: 'warning',
  DISRUPTION: 'danger',
  ROUTE_CHANGE: 'warning',
  TIMETABLE_CHANGE: 'info',
  UPCOMING_DEPARTURE: 'success',
}

const VARIANT_STYLES = {
  success: 'bg-success-soft/70 text-success dark:bg-success-soft/20 dark:text-[var(--success)]',
  warning: 'bg-warning-soft/70 text-warning dark:bg-warning-soft/20 dark:text-[var(--warning)]',
  danger: 'bg-danger-soft/70 text-danger dark:bg-danger-soft/20 dark:text-[var(--danger)]',
  info: 'bg-accent-muted text-accent dark:bg-accent-soft dark:text-[var(--accent)]',
  muted: 'bg-surface-alt text-muted',
}

export function StatusBadge({ status, variant, label, icon, className = '' }) {
  const resolvedVariant = variant || STATUS_VARIANTS[status] || 'muted'
  const displayLabel = label || (status ? status.replace(/_/g, ' ') : '—')
  const style = VARIANT_STYLES[resolvedVariant] || VARIANT_STYLES.muted

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium leading-4 ${style} ${className}`}
    >
      {icon && <span className="shrink-0">{icon}</span>}
      {displayLabel}
    </span>
  )
}
