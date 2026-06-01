import { History, Route, User, Wallet } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { TABS } from './_shared'

const ICONS = {
  user: User,
  route: Route,
  history: History,
  wallet: Wallet,
}

export function ProfileTabs({ activeTab, onTabChange }) {
  const { t } = useTranslation('profile')
  const [, setSearchParams] = useSearchParams()

  function handleSelect(tabId) {
    onTabChange(tabId)
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      if (tabId === 'account') next.delete('tab')
      else next.set('tab', tabId)
      return next
    }, { replace: true })
  }

  function handleKeyDown(event, index) {
    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft' && event.key !== 'Home' && event.key !== 'End') return
    event.preventDefault()
    let nextIndex = index
    if (event.key === 'ArrowRight') nextIndex = (index + 1) % TABS.length
    if (event.key === 'ArrowLeft') nextIndex = (index - 1 + TABS.length) % TABS.length
    if (event.key === 'Home') nextIndex = 0
    if (event.key === 'End') nextIndex = TABS.length - 1
    const nextTab = TABS[nextIndex]
    handleSelect(nextTab.id)
    const button = document.getElementById(`profile-tab-${nextTab.id}`)
    button?.focus()
  }

  return (
    <div
      role="tablist"
      aria-label={t('tab_label')}
      className="flex justify-center overflow-x-auto border-b border-border"
    >
      <div className="inline-flex gap-1 sm:gap-2">
        {TABS.map((tab, index) => {
          const Icon = ICONS[tab.iconKey]
          const isActive = activeTab === tab.id
          return (
            <button
              key={tab.id}
              id={`profile-tab-${tab.id}`}
              type="button"
              role="tab"
              aria-selected={isActive}
              aria-controls={`profile-panel-${tab.id}`}
              tabIndex={isActive ? 0 : -1}
              onClick={() => handleSelect(tab.id)}
              onKeyDown={(event) => handleKeyDown(event, index)}
              className={`relative inline-flex shrink-0 items-center gap-2 border-b-2 px-3 py-2.5 text-sm font-medium transition sm:px-4 ${
                isActive
                  ? 'border-accent text-ink'
                  : 'border-transparent text-muted hover:border-border hover:text-ink'
              }`}
            >
              <Icon size={15} aria-hidden="true" />
              <span>{t(`tab_${tab.id}`)}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
