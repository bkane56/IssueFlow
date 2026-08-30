import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createIssue } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { sampleUser } from '../test/fixtures'
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
    expect(createIssue).not.toHaveBeenCalled()
  })
})
