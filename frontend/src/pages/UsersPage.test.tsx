import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createUser, listUsers } from '../api/usersApi'
import { sampleUser } from '../test/fixtures'
import { UsersPage } from './UsersPage'

vi.mock('../api/usersApi', () => ({
  listUsers: vi.fn(),
  createUser: vi.fn(),
}))

describe('UsersPage', () => {
  beforeEach(() => {
    vi.mocked(listUsers).mockResolvedValue([sampleUser])
    vi.mocked(createUser).mockReset()
  })

  it('shows validation messages when required fields are empty', async () => {
    const user = userEvent.setup()
    render(<UsersPage />)

    expect(await screen.findByText(sampleUser.name)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Add user' }))

    expect(await screen.findByText('Name is required')).toBeInTheDocument()
    expect(screen.getByText('Email is required')).toBeInTheDocument()
    expect(createUser).not.toHaveBeenCalled()
  })

  it('adds a user and refreshes the list', async () => {
    const user = userEvent.setup()
    const created = {
      id: 8,
      name: 'Casey Nguyen',
      email: 'casey.nguyen@issueflow.local',
      active: true,
    }
    vi.mocked(createUser).mockResolvedValue(created)
    vi.mocked(listUsers)
      .mockResolvedValueOnce([sampleUser])
      .mockResolvedValueOnce([sampleUser, created])

    render(<UsersPage />)
    expect(await screen.findByText(sampleUser.name)).toBeInTheDocument()

    await user.type(screen.getByLabelText('Name'), 'Casey Nguyen')
    await user.type(screen.getByLabelText('Email'), 'casey.nguyen@issueflow.local')
    await user.click(screen.getByRole('button', { name: 'Add user' }))

    expect(await screen.findByText('Casey Nguyen')).toBeInTheDocument()
    expect(createUser).toHaveBeenCalledWith('Casey Nguyen', 'casey.nguyen@issueflow.local')
  })
})
