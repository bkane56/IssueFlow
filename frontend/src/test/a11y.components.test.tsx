import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { StatusMessage } from '../components/StatusMessage'
import { AppLayout } from '../layouts/AppLayout'

describe('accessibility components', () => {
  it('StatusMessage uses role=status for info messages', () => {
    render(<StatusMessage>Loading data.</StatusMessage>)

    const message = screen.getByRole('status')
    expect(message).toHaveTextContent('Loading data.')
    expect(message).toHaveAttribute('aria-live', 'polite')
  })

  it('StatusMessage uses role=alert for error messages', () => {
    render(<StatusMessage tone="error">Request failed.</StatusMessage>)

    const message = screen.getByRole('alert')
    expect(message).toHaveTextContent('Request failed.')
    expect(message).toHaveAttribute('aria-live', 'assertive')
  })

  it('AppLayout includes a skip link and main content landmark', () => {
    render(
      <MemoryRouter>
        <AppLayout />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute(
      'href',
      '#main-content',
    )
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content')
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument()
  })
})
