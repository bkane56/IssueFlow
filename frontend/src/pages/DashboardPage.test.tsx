import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getDashboard } from '../api/dashboardApi'
import { listIssues } from '../api/issuesApi'
import { sampleIssue } from '../test/fixtures'
import { DashboardPage } from './DashboardPage'

vi.mock('../api/dashboardApi', () => ({
  getDashboard: vi.fn(),
}))

vi.mock('../api/issuesApi', () => ({
  listIssues: vi.fn(),
}))

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(getDashboard).mockResolvedValue({
      open: 12,
      critical: 2,
      inProgress: 5,
      resolved: 18,
    })
    vi.mocked(listIssues).mockResolvedValue([sampleIssue])
  })

  it('renders dashboard statistics', async () => {
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Open Issues')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('Critical Issues')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'In Progress' })).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Resolved' })).toBeInTheDocument()
    expect(screen.getByText('18')).toBeInTheDocument()
    expect(screen.getByText(sampleIssue.title)).toBeInTheDocument()
  })
})
