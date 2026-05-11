const toneMap = {
  default: 'bg-surface',
  soft: 'bg-surface-soft',
  accent: 'bg-[var(--accent-soft)]',
  ink: 'bg-[var(--surface-ink)] text-[var(--ink)]',
}

export function PanelCard({ children, className = '', tone = 'default' }) {
  return (
    <section
      className={`rounded-panel border border-border p-4 shadow-panel transition-colors ${toneMap[tone] || toneMap.default} ${className}`}
    >
      {children}
    </section>
  )
}
