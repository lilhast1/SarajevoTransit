import { LoadingSpinner } from '../common/LoadingStates'

export function DataTable({ columns, rows, page, totalPages, onPageChange, loading, expandedRowId, renderExpandedRow }) {
  if (loading) return <LoadingSpinner />

  return (
    <div className="overflow-x-auto rounded-panel border border-border">
      <table className="w-full text-sm">
        <thead className="border-b border-border bg-surface-alt text-left">
          <tr>
            {columns.map((col) => (
              <th key={col.key} className="px-3 py-2 font-medium text-muted">
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-3 py-6 text-center text-muted">
                No records found.
              </td>
            </tr>
          ) : (
            rows.map((row, i) => {
              const isExpanded = expandedRowId != null && (row.id ?? i) === expandedRowId
              return (
                <>
                  <tr key={row.id ?? i} className="border-b border-border last:border-0 hover:bg-surface-alt/50">
                    {columns.map((col) => (
                      <td key={col.key} className="px-3 py-2 text-ink">
                        {col.render ? col.render(row) : row[col.key] ?? '—'}
                      </td>
                    ))}
                  </tr>
                  {isExpanded && renderExpandedRow && (
                    <tr key={`${row.id ?? i}-expanded`} className="border-b border-border bg-surface-alt/30">
                      <td colSpan={columns.length} className="px-3 py-3">
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

      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-border px-3 py-2">
          <span className="text-xs text-muted">
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => onPageChange(page - 1)}
              className="rounded-panel border border-border px-3 py-1 text-xs text-ink disabled:opacity-40"
            >
              Prev
            </button>
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
              className="rounded-panel border border-border px-3 py-1 text-xs text-ink disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
