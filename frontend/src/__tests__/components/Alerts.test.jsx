import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ErrorAlert, SuccessAlert } from '../../components/common/Alerts'

describe('ErrorAlert', () => {
  it('renders nothing when error is null', () => {
    const { container } = render(<ErrorAlert error={null} />)
    expect(container.firstChild).toBeNull()
  })

  it('renders a string error message', () => {
    render(<ErrorAlert error="Something went wrong" />)
    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
  })

  it('renders error.message from an Error object', () => {
    render(<ErrorAlert error={{ message: 'Network failure' }} />)
    expect(screen.getByText('Network failure')).toBeInTheDocument()
  })

  it('falls back to generic message when object has no message', () => {
    render(<ErrorAlert error={{}} />)
    expect(screen.getByText('An error occurred')).toBeInTheDocument()
  })

  it('does not render dismiss button when onDismiss is not provided', () => {
    render(<ErrorAlert error="Error" />)
    expect(screen.queryByLabelText('Dismiss error')).not.toBeInTheDocument()
  })

  it('calls onDismiss when dismiss button is clicked', () => {
    const onDismiss = vi.fn()
    render(<ErrorAlert error="Error" onDismiss={onDismiss} />)
    fireEvent.click(screen.getByLabelText('Dismiss error'))
    expect(onDismiss).toHaveBeenCalledOnce()
  })
})

describe('SuccessAlert', () => {
  it('renders nothing when message is falsy', () => {
    const { container } = render(<SuccessAlert message="" />)
    expect(container.firstChild).toBeNull()
  })

  it('renders the success message', () => {
    render(<SuccessAlert message="Saved successfully" />)
    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(screen.getByText('Saved successfully')).toBeInTheDocument()
  })

  it('calls onDismiss when dismiss button is clicked', () => {
    const onDismiss = vi.fn()
    render(<SuccessAlert message="Done" onDismiss={onDismiss} />)
    fireEvent.click(screen.getByLabelText('Dismiss message'))
    expect(onDismiss).toHaveBeenCalledOnce()
  })
})
