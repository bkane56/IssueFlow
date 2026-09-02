import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { axe } from 'vitest-axe'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getDashboard } from '../api/dashboardApi'
import { listIssues } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { sampleIssue, sampleUser } from './fixtures'
import { DashboardPage } from '../pages/DashboardPage'
import { CreateIssuePage } from '../pages/CreateIssuePage'
import { UsersPage } from '../pages/UsersPage'

vi.mock('../api/dashboardApi', () => ({
  getDashboard: vi.fn(),
}))

vi.mock('../api/issuesApi', () => ({
  listIssues: vi.fn(),
  createIssue: vi.fn(),
}))

vi.mock('../api/usersApi', () => ({
  listUsers: vi.fn(),
  createUser: vi.fn(),
}))

describe('accessibility smoke tests', () => {
  beforeEach(() => {
    vi.mocked(getDashboard).mockResolvedValue({
      open: 12,
      critical: 2,
      inProgress: 5,
      resolved: 18,
    })
    vi.mocked(listIssues).mockResolvedValue([sampleIssue])
    vi.mocked(listUsers).mockResolvedValue([sampleUser])
  })

  it('DashboardPage has no axe violations after data loads', async () => {
    const { container, findByText } = render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    )

    await findByText('Open Issues')
    const results = await axe(container, { rules: { 'color-contrast': { enabled: false } } })
    expect(results.violations).toHaveLength(0)
  })

  it('CreateIssuePage has no axe violations', async () => {
    const { container, findByRole } = render(
      <MemoryRouter>
        <CreateIssuePage />
      </MemoryRouter>,
    )

    await findByRole('heading', { name: 'New Issue' })
    const results = await axe(container, { rules: { 'color-contrast': { enabled: false } } })
    expect(results.violations).toHaveLength(0)
  })

  it('UsersPage has no axe violations after data loads', async () => {
    const { container, findByText } = render(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    )

    await findByText(sampleUser.name)
    const results = await axe(container, { rules: { 'color-contrast': { enabled: false } } })
    expect(results.violations).toHaveLength(0)
  })
})
