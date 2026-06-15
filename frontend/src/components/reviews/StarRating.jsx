import { useState } from 'react'
import { Star } from 'lucide-react'

export function StarRating({ value = 0, onChange, readOnly = false, size = 20 }) {
  const [hovered, setHovered] = useState(0)

  if (readOnly) {
    return (
      <span className="inline-flex items-center gap-0.5" aria-label={`${value} out of 5 stars`}>
        {[1, 2, 3, 4, 5].map((n) => (
          <Star
            key={n}
            size={size}
            className={n <= Math.round(value) ? 'text-amber-400' : 'text-border'}
            fill={n <= Math.round(value) ? 'currentColor' : 'none'}
            aria-hidden="true"
          />
        ))}
      </span>
    )
  }

  const active = hovered || value

  return (
    <span className="inline-flex items-center gap-0.5" role="group" aria-label="Select rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          aria-label={`${n} star${n !== 1 ? 's' : ''}`}
          aria-pressed={value === n}
          onClick={() => onChange?.(n)}
          onMouseEnter={() => setHovered(n)}
          onMouseLeave={() => setHovered(0)}
          className="p-0.5 transition-transform hover:scale-110 focus:outline-none"
        >
          <Star
            size={size}
            className={n <= active ? 'text-amber-400' : 'text-muted'}
            fill={n <= active ? 'currentColor' : 'none'}
            aria-hidden="true"
          />
        </button>
      ))}
    </span>
  )
}
