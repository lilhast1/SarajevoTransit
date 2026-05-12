/**
 * Loading & Skeleton Components
 * For better UX when data is loading
 */

import { PanelCard } from './PanelCard'

export function LoadingSkeletons({ count = 3 }) {
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </>
  )
}

export function SkeletonCard() {
  return (
    <PanelCard>
      <div className="space-y-3">
        <div className="h-6 w-1/3 animate-pulse rounded bg-surface-alt" />
        <div className="h-4 w-2/3 animate-pulse rounded bg-surface-alt" />
        <div className="h-4 w-full animate-pulse rounded bg-surface-alt" />
      </div>
    </PanelCard>
  )
}

export function LoadingSpinner({ label = 'Loading...' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-8">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-border border-t-accent" />
      <p className="text-sm text-muted">{label}</p>
    </div>
  )
}

export function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <PanelCard className="text-center" tone="soft">
      <div className="space-y-3">
        {Icon && <Icon className="mx-auto" size={32} />}
        <h3 className="text-base font-semibold text-ink">{title}</h3>
        {description && <p className="text-sm text-muted">{description}</p>}
        {action && <div className="mt-4">{action}</div>}
      </div>
    </PanelCard>
  )
}
