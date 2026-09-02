import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listIssues } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { EMPTY_ISSUE_FILTERS } from '../constants/filters'
import { sampleIssue, sampleUser } from '../test/fixtures'
import { IssueListPage } from './IssueListPage'

vi.mock('../api/issuesApi', () => ({
  listIssues: vi.fn(),
}))

vi.mock('../api/usersApi', () => ({
  listUsers: vi.fn(),
}))

describe('IssueListPage', () => {
  beforeEach(() => {
    vi.mocked(listUsers).mockResolvedValue([sampleUser])
    vi.mocked(listIssues).mockResolvedValue([sampleIssue])
  })

  it('renders issues from the API', async () => {
    render(
      <MemoryRouter>
        <IssueListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(sampleIssue.title)).toBeInTheDocument()
    expect(listIssues).toHaveBeenCalledWith(EMPTY_ISSUE_FILTERS)
  })

  it('refetches issues when filters change and when they are cleared', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <IssueListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(sampleIssue.title)).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Status'), 'IN_PROGRESS')
    await waitFor(() => {
      expect(listIssues).toHaveBeenCalledWith(expect.objectContaining({ status: 'IN_PROGRESS' }))
    })

    await user.click(screen.getByRole('button', { name: 'Clear filters' }))
    await waitFor(() => {
      expect(listIssues).toHaveBeenCalledWith(EMPTY_ISSUE_FILTERS)
    })
  })

  it('shows an error when the issue list cannot be loaded', async () => {
    vi.mocked(listIssues).mockRejectedValue(new Error('Unable to load issues'))

    render(
      <MemoryRouter>
        <IssueListPage />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load issues')
  })
})
