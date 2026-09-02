import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getDashboard } from '../api/dashboardApi'
import { listIssues } from '../api/issuesApi'
import { issueWith, sampleIssue } from '../test/fixtures'
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

  it('omits resolved and closed issues from the highest priority list', async () => {
    vi.mocked(listIssues).mockResolvedValue([
      issueWith({
        id: 11,
        title: 'Resolved payment confirmation failure',
        status: 'RESOLVED',
        priorityScore: 200,
      }),
      issueWith({
        id: 12,
        title: 'Closed login page outage',
        status: 'CLOSED',
        priorityScore: 190,
      }),
      issueWith({
        id: 13,
        title: 'Open documentation gap on export',
        status: 'NEW',
        priority: 'P4',
        priorityScore: 10,
      }),
      sampleIssue,
    ])

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(sampleIssue.title)).toBeInTheDocument()
    expect(screen.getByText('Open documentation gap on export')).toBeInTheDocument()
    expect(screen.queryByText('Resolved payment confirmation failure')).not.toBeInTheDocument()
    expect(screen.queryByText('Closed login page outage')).not.toBeInTheDocument()
  })

  it('shows an error when dashboard statistics cannot be loaded', async () => {
    vi.mocked(getDashboard).mockRejectedValue(new Error('Unable to load dashboard'))

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load dashboard')
  })
})
