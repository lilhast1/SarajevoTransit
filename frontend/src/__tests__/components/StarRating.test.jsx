import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { StarRating } from '../../components/reviews/StarRating'

describe('StarRating (read-only)', () => {
  it('renders 5 stars', () => {
    render(<StarRating value={3} readOnly />)
    const container = screen.getByLabelText('3 out of 5 stars')
    expect(container).toBeInTheDocument()
  })

  it('shows correct aria-label for value', () => {
    render(<StarRating value={5} readOnly />)
    expect(screen.getByLabelText('5 out of 5 stars')).toBeInTheDocument()
  })
})

describe('StarRating (interactive)', () => {
  it('calls onChange with the clicked star value', () => {
    const onChange = vi.fn()
    render(<StarRating value={0} onChange={onChange} />)
    const buttons = screen.getAllByRole('button')
    expect(buttons).toHaveLength(5)
    fireEvent.click(buttons[2])
    expect(onChange).toHaveBeenCalledWith(3)
  })

  it('renders star buttons with aria-labels', () => {
    render(<StarRating value={0} onChange={vi.fn()} />)
    expect(screen.getByLabelText('1 star')).toBeInTheDocument()
    expect(screen.getByLabelText('2 stars')).toBeInTheDocument()
    expect(screen.getByLabelText('5 stars')).toBeInTheDocument()
  })

  it('marks the selected star as pressed', () => {
    render(<StarRating value={3} onChange={vi.fn()} />)
    const thirdStar = screen.getByLabelText('3 stars')
    expect(thirdStar).toHaveAttribute('aria-pressed', 'true')
  })
})
