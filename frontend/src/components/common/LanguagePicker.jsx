import { useEffect, useRef, useState } from 'react'
import { Check, Languages } from 'lucide-react'
import { useAppContext } from '../../context/AppContext'
import { useTranslation } from 'react-i18next'

const LANGUAGES = [
  { code: 'en', label: 'English', short: 'EN' },
  { code: 'bs', label: 'Bosanski', short: 'BS' },
  { code: 'sr', label: 'Srpski', short: 'SR' },
  { code: 'hr', label: 'Hrvatski', short: 'HR' },
]

export function LanguagePicker() {
  const { language, setLanguage } = useAppContext()
  const { t } = useTranslation('nav')
  const [open, setOpen] = useState(false)
  const [focusedIndex, setFocusedIndex] = useState(0)
  const ref = useRef(null)
  const triggerRef = useRef(null)
  const optionRefs = useRef([])

  const current = LANGUAGES.find((l) => l.code === language) || LANGUAGES[0]

  useEffect(() => {
    function handleClick(e) {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  useEffect(() => {
    if (open && optionRefs.current[focusedIndex]) {
      optionRefs.current[focusedIndex].focus()
    }
  }, [open, focusedIndex])

  function handleTriggerKeyDown(e) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      setFocusedIndex(LANGUAGES.findIndex((l) => l.code === language))
      setOpen(true)
    }
  }

  function handleOptionKeyDown(e, index) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setFocusedIndex((index + 1) % LANGUAGES.length)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setFocusedIndex((index - 1 + LANGUAGES.length) % LANGUAGES.length)
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      setLanguage(LANGUAGES[index].code)
      setOpen(false)
      triggerRef.current?.focus()
    } else if (e.key === 'Escape') {
      setOpen(false)
      triggerRef.current?.focus()
    }
  }

  function handleSelect(code) {
    setLanguage(code)
    setOpen(false)
  }

  return (
    <div ref={ref} className="relative">
      <button
        ref={triggerRef}
        type="button"
        onClick={() => {
          setFocusedIndex(LANGUAGES.findIndex((l) => l.code === language))
          setOpen((v) => !v)
        }}
        onKeyDown={handleTriggerKeyDown}
        className="flex items-center gap-1 rounded-panel border border-border px-2 py-2 text-xs font-semibold text-muted transition hover:bg-surface-alt hover:text-ink"
        aria-label={t('select_language')}
        aria-expanded={open}
        aria-haspopup="listbox"
      >
        <Languages size={14} />
        <span>{current.short}</span>
      </button>

      {open && (
        <div
          role="listbox"
          aria-label={t('select_language')}
          className="absolute right-0 top-full z-50 mt-2 w-40 rounded-panel border border-border bg-surface shadow-xl"
        >
          {LANGUAGES.map((lang, index) => (
            <button
              key={lang.code}
              ref={(el) => { optionRefs.current[index] = el }}
              role="option"
              aria-selected={language === lang.code}
              type="button"
              onClick={() => handleSelect(lang.code)}
              onKeyDown={(e) => handleOptionKeyDown(e, index)}
              className={`flex w-full items-center justify-between px-4 py-2.5 text-sm transition ${
                language === lang.code
                  ? 'bg-accent/10 font-semibold text-accent'
                  : 'text-ink hover:bg-surface-alt'
              } first:rounded-t-panel last:rounded-b-panel`}
            >
              <span>{lang.label}</span>
              {language === lang.code && <Check size={13} />}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
