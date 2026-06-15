import { useState, useEffect, useRef, useCallback } from 'react'
import { ChevronDown, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'

function useDebounce(value, delay) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return debounced
}

/**
 * Searchable dropdown. Two modes:
 * - Static: pass `options` array, filtering is client-side
 * - Async: pass `loadOptions(query) => Promise<item[]>`, called on search
 *
 * Props:
 *   value        – currently selected item (full object) or null
 *   onChange     – (item | null) => void
 *   options      – item[] (static mode)
 *   loadOptions  – async (query: string) => item[]  (async mode)
 *   getLabel     – (item) => string  (default: item.label)
 *   getValue     – (item) => any    (default: item.id)
 *   placeholder  – string
 *   disabled     – bool
 */
export function SearchableSelect({
  value,
  onChange,
  options,
  loadOptions,
  getLabel = (item) => item.label ?? String(item.id),
  getValue = (item) => item.id,
  placeholder = 'Select…',
  disabled = false,
}) {
  const { t } = useTranslation('common')
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [asyncItems, setAsyncItems] = useState([])
  const [loading, setLoading] = useState(false)
  const containerRef = useRef(null)
  const inputRef = useRef(null)

  const debouncedQuery = useDebounce(query, 250)

  // Close on outside click
  useEffect(() => {
    function onMouseDown(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false)
        setQuery('')
      }
    }
    document.addEventListener('mousedown', onMouseDown)
    return () => document.removeEventListener('mousedown', onMouseDown)
  }, [])

  // Async load
  useEffect(() => {
    if (!loadOptions || !open) return
    setLoading(true)
    loadOptions(debouncedQuery)
      .then(setAsyncItems)
      .catch(() => setAsyncItems([]))
      .finally(() => setLoading(false))
  }, [debouncedQuery, open, loadOptions])

  // Focus search input when opened
  useEffect(() => {
    if (open) inputRef.current?.focus()
  }, [open])

  const items = loadOptions ? asyncItems : (options ?? [])

  const filtered = loadOptions
    ? items
    : items.filter((item) =>
        getLabel(item).toLowerCase().includes(query.toLowerCase())
      )

  const handleSelect = useCallback((item) => {
    onChange(item)
    setOpen(false)
    setQuery('')
  }, [onChange])

  const handleClear = useCallback((e) => {
    e.stopPropagation()
    onChange(null)
  }, [onChange])

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between rounded-panel border border-border bg-surface px-3 py-2 text-left text-sm text-ink disabled:opacity-50"
      >
        <span className={value ? 'text-ink' : 'text-muted'}>
          {value ? getLabel(value) : placeholder}
        </span>
        <span className="flex items-center gap-1">
          {value && (
            <span
              role="button"
              tabIndex={0}
              onClick={handleClear}
              onKeyDown={(e) => e.key === 'Enter' && handleClear(e)}
              className="rounded p-0.5 text-muted hover:text-ink"
              aria-label={t('clear_selection')}
            >
              <X size={13} aria-hidden="true" />
            </span>
          )}
          <ChevronDown size={14} className="text-muted" />
        </span>
      </button>

      {open && (
        <div className="absolute z-50 mt-1 w-full rounded-panel border border-border bg-surface shadow-lg">
          <div className="border-b border-border px-2 py-1.5">
            <input
              ref={inputRef}
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t('search_placeholder')}
              className="w-full bg-transparent text-sm text-ink placeholder:text-muted focus:outline-none"
            />
          </div>
          <ul className="max-h-52 overflow-auto py-1">
            {loading && (
              <li className="px-3 py-2 text-xs text-muted">{t('loading_long')}</li>
            )}
            {!loading && filtered.length === 0 && (
              <li className="px-3 py-2 text-xs text-muted">{t('no_results')}</li>
            )}
            {!loading && filtered.map((item) => (
              <li
                key={getValue(item)}
                onClick={() => handleSelect(item)}
                className={`cursor-pointer px-3 py-2 text-sm hover:bg-surface-alt ${
                  value && getValue(value) === getValue(item) ? 'bg-surface-alt font-medium text-ink' : 'text-ink'
                }`}
              >
                {getLabel(item)}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
