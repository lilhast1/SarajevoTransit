import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from '../../components/common/StatusBadge'

describe('StatusBadge', () => {
  it('renders the status text for OPERATIONAL', () => {
    render(<StatusBadge status="OPERATIONAL" />)
    expect(screen.getByText('OPERATIONAL')).toBeInTheDocument()
  })

  it('uses success class for OPERATIONAL', () => {
    const { container } = render(<StatusBadge status="OPERATIONAL" />)
    expect(container.firstChild.className).toContain('text-success')
  })

  it('uses danger class for OUT_OF_SERVICE', () => {
    const { container } = render(<StatusBadge status="OUT_OF_SERVICE" />)
    expect(container.firstChild.className).toContain('text-danger')
  })

  it('uses warning class for IN_MAINTENANCE', () => {
    const { container } = render(<StatusBadge status="IN_MAINTENANCE" />)
    expect(container.firstChild.className).toContain('text-warning')
  })

  it('renders custom label instead of status key', () => {
    render(<StatusBadge status="OPERATIONAL" label="Online" />)
    expect(screen.getByText('Online')).toBeInTheDocument()
    expect(screen.queryByText('OPERATIONAL')).not.toBeInTheDocument()
  })

  it('falls back to muted for unknown status', () => {
    const { container } = render(<StatusBadge status="UNKNOWN_XYZ" />)
    expect(container.firstChild.className).toContain('text-muted')
  })

  it('renders — for missing status', () => {
    render(<StatusBadge />)
    expect(screen.getByText('—')).toBeInTheDocument()
  })
})
