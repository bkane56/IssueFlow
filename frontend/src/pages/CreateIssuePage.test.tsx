import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createIssue } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { sampleIssue, sampleUser } from '../test/fixtures'
import { CreateIssuePage } from './CreateIssuePage'

vi.mock('../api/issuesApi', () => ({
  createIssue: vi.fn(),
}))

vi.mock('../api/usersApi', () => ({
  listUsers: vi.fn(),
}))

describe('CreateIssuePage', () => {
  beforeEach(() => {
    vi.mocked(listUsers).mockResolvedValue([sampleUser])
    vi.mocked(createIssue).mockReset()
  })

  it('shows validation messages when required fields are empty', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <CreateIssuePage />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Create issue' }))

    expect(await screen.findByText('Title is required')).toBeInTheDocument()
    expect(screen.getByText('Description is required')).toBeInTheDocument()
    expect(document.getElementById('title-input')).toHaveAttribute('aria-invalid', 'true')
    expect(document.getElementById('description-input')).toHaveAttribute('aria-invalid', 'true')
    expect(createIssue).not.toHaveBeenCalled()
  })

  it('creates an issue and navigates to the detail page', async () => {
    const user = userEvent.setup()
    vi.mocked(createIssue).mockResolvedValue(sampleIssue)

    render(
      <MemoryRouter initialEntries={['/issues/new']}>
        <Routes>
          <Route path="/issues/new" element={<CreateIssuePage />} />
          <Route path="/issues/:id" element={<p>Opened issue 10</p>} />
        </Routes>
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Title'), sampleIssue.title)
    await user.type(screen.getByLabelText('Description'), sampleIssue.description)
    await user.click(screen.getByRole('button', { name: 'Create issue' }))

    expect(await screen.findByText('Opened issue 10')).toBeInTheDocument()
    expect(createIssue).toHaveBeenCalled()
  })

  it('shows an error when create fails', async () => {
    const user = userEvent.setup()
    vi.mocked(createIssue).mockRejectedValue(new Error('Unable to create the issue'))

    render(
      <MemoryRouter>
        <CreateIssuePage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText('Title'), sampleIssue.title)
    await user.type(screen.getByLabelText('Description'), sampleIssue.description)
    await user.click(screen.getByRole('button', { name: 'Create issue' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to create the issue')
  })
})
