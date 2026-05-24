import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { PanelCard } from '../components/common/PanelCard'

export function NotFoundPage() {
  const { t } = useTranslation('notFound')
  return (
    <PanelCard>
      <h2 className="text-xl font-semibold text-ink">{t('title')}</h2>
      <p className="mt-2 text-sm text-muted">{t('subtitle')}</p>
      <Link to="/" className="mt-3 inline-block text-sm font-medium text-accent">
        {t('home')}
      </Link>
    </PanelCard>
  )
}
