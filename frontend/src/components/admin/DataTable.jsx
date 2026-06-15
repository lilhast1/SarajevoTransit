import { ChevronLeft, ChevronRight } from 'lucide-react'

function TableSkeleton({ rows = 5, columns = 4 }) {
  return (
    <div className="space-y-3 p-4">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex gap-4">
          {Array.from({ length: columns }).map((_, j) => (
            <div
              key={j}
              className="h-4 animate-pulse rounded bg-border"
              style={{ width: `${60 + Math.random() * 40}%` }}
            />
          ))}
        </div>
      ))}
    </div>
  )
}

export function DataTable({ columns, rows, page, totalPages, onPageChange, loading, expandedRowId, renderExpandedRow }) {
  if (loading) {
    return (
      <div className="overflow-hidden rounded-panel border border-border bg-surface">
        <TableSkeleton rows={5} columns={columns.length} />
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-panel border border-border bg-surface shadow-panel">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border bg-surface-soft">
              {columns.map((col) => (
                <th
                  key={col.key}
                  className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-muted"
                >
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-12 text-center text-sm text-muted">
                  <div className="flex flex-col items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-surface-alt">
                      <span className="text-lg">{'\u2014'}</span>
                    </div>
                    <span>No records found.</span>
                  </div>
                </td>
              </tr>
            ) : (
              rows.map((row, i) => {
                const isExpanded = expandedRowId != null && (row.id ?? i) === expandedRowId
                return (
                  <>
                    <tr
                      key={row.id ?? i}
                      className="border-b border-border last:border-0 transition-colors hover:bg-accent-muted/40"
                    >
                      {columns.map((col) => (
                        <td key={col.key} className="px-4 py-3 text-ink">
                          {col.render ? col.render(row) : row[col.key] ?? '\u2014'}
                        </td>
                      ))}
                    </tr>
                    {isExpanded && renderExpandedRow && (
                      <tr key={`${row.id ?? i}-expanded`} className="border-b border-border bg-accent-muted/20">
                        <td colSpan={columns.length} className="px-4 py-4">
                          {renderExpandedRow(row)}
                        </td>
                      </tr>
                    )}
                  </>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-border px-4 py-3">
          <span className="text-xs text-muted">
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex items-center gap-1">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => onPageChange(page - 1)}
              className="flex items-center gap-1 rounded-panel border border-border px-2.5 py-1.5 text-xs font-medium text-ink transition hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft size={14} />
              Prev
            </button>
            {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
              const start = Math.max(0, Math.min(page - 2, totalPages - 5))
              const p = start + i
              return (
                <button
                  key={p}
                  type="button"
                  onClick={() => onPageChange(p)}
                  className={`min-w-[28px] rounded-panel px-2 py-1.5 text-xs font-medium transition ${
                    p === page
                      ? 'bg-accent text-white'
                      : 'text-muted hover:bg-surface-alt hover:text-ink'
                  }`}
                >
                  {p + 1}
                </button>
              )
            })}
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
              className="flex items-center gap-1 rounded-panel border border-border px-2.5 py-1.5 text-xs font-medium text-ink transition hover:bg-surface-alt disabled:cursor-not-allowed disabled:opacity-40"
            >
              Next
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
