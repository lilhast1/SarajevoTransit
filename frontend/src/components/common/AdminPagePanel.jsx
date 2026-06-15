import { SELECT_CLS } from '../admin/constants'

function Header({ title, subtitle, actions }) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div className="min-w-0 flex-1">
        <h2 className="text-2xl font-bold tracking-tight text-ink">{title}</h2>
        {subtitle && <p className="mt-0.5 text-sm text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="shrink-0">{actions}</div>}
    </div>
  )
}

function Toolbar({ children }) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      {children}
    </div>
  )
}

function ToolbarSpacer() {
  return <div className="ml-auto" />
}

function ToolbarGroup({ label, children }) {
  return (
    <div className="flex items-center gap-2">
      {label && <span className="whitespace-nowrap text-sm text-muted">{label}</span>}
      {children}
    </div>
  )
}

function ToolbarDivider() {
  return <div className="h-5 w-px bg-border" aria-hidden />
}

function Body({ children }) {
  return <div className="space-y-4">{children}</div>
}

function AdminPagePanel({ children, className = '' }) {
  return (
    <div className={`space-y-5 rounded-panel border border-border bg-surface p-5 shadow-panel ${className}`}>
      {children}
    </div>
  )
}

AdminPagePanel.Header = Header
AdminPagePanel.Toolbar = Toolbar
AdminPagePanel.ToolbarSpacer = ToolbarSpacer
AdminPagePanel.ToolbarGroup = ToolbarGroup
AdminPagePanel.ToolbarDivider = ToolbarDivider
AdminPagePanel.Body = Body

export { AdminPagePanel, SELECT_CLS }
