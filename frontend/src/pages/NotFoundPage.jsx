import { Link } from 'react-router-dom'
import { PanelCard } from '../components/common/PanelCard'

export function NotFoundPage() {
  return (
    <PanelCard>
      <h2 className="text-xl font-semibold text-ink">Page not found</h2>
      <p className="mt-2 text-sm text-muted">The route you requested does not exist.</p>
      <Link to="/" className="mt-3 inline-block text-sm font-medium text-accent">
        Go back home
      </Link>
    </PanelCard>
  )
}
