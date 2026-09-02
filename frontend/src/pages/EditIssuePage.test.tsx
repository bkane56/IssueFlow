import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getIssue, updateIssue } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { sampleIssue, sampleUser } from '../test/fixtures'
import { EditIssuePage } from './EditIssuePage'

vi.mock('../api/issuesApi', () => ({
  getIssue: vi.fn(),
  updateIssue: vi.fn(),
}))

vi.mock('../api/usersApi', () => ({
  listUsers: vi.fn(),
}))

describe('EditIssuePage', () => {
  beforeEach(() => {
    vi.mocked(listUsers).mockResolvedValue([sampleUser])
    vi.mocked(getIssue).mockResolvedValue(sampleIssue)
    vi.mocked(updateIssue).mockReset()
  })

  it('loads the issue and saves changes', async () => {
    const user = userEvent.setup()
    vi.mocked(updateIssue).mockResolvedValue(sampleIssue)

    render(
      <MemoryRouter initialEntries={['/issues/10/edit']}>
        <Routes>
          <Route path="/issues/:id/edit" element={<EditIssuePage />} />
          <Route path="/issues/:id" element={<p>Returned to issue 10</p>} />
        </Routes>
      </MemoryRouter>,
    )

    const title = await screen.findByDisplayValue(sampleIssue.title)
    await user.clear(title)
    await user.type(title, 'Checkout API timeouts during peak traffic')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Returned to issue 10')).toBeInTheDocument()
    expect(updateIssue).toHaveBeenCalledWith(
      10,
      expect.objectContaining({ title: 'Checkout API timeouts during peak traffic' }),
    )
  })

  it('shows validation when the title is cleared', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/issues/10/edit']}>
        <Routes>
          <Route path="/issues/:id/edit" element={<EditIssuePage />} />
        </Routes>
      </MemoryRouter>,
    )

    const title = await screen.findByLabelText('Title')
    await user.clear(title)
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Title is required')).toBeInTheDocument()
    expect(updateIssue).not.toHaveBeenCalled()
  })

  it('shows an error when the issue cannot be loaded', async () => {
    vi.mocked(getIssue).mockRejectedValue(new Error('Issue 1042 was not found'))

    render(
      <MemoryRouter initialEntries={['/issues/1042/edit']}>
        <Routes>
          <Route path="/issues/:id/edit" element={<EditIssuePage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('Issue 1042 was not found')
  })
})
