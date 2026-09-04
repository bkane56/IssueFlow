import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getIssue, getIssueHistory, recalculateTriage, assignIssue, changeIssueStatus } from '../api/issuesApi'
import { enqueueEscalationNotification, listOutboundJobs } from '../api/outboundApi'
import { listUsers } from '../api/usersApi'
import { issueWith, sampleHistory, sampleIssue, sampleOutboundJob, sampleUser } from '../test/fixtures'
import { IssueDetailPage } from './IssueDetailPage'

vi.mock('../api/issuesApi', () => ({
  getIssue: vi.fn(),
  getIssueHistory: vi.fn(),
  assignIssue: vi.fn(),
  changeIssueStatus: vi.fn(),
  recalculateTriage: vi.fn(),
}))

vi.mock('../api/outboundApi', () => ({
  listOutboundJobs: vi.fn(),
  enqueueEscalationNotification: vi.fn(),
}))

vi.mock('../api/usersApi', () => ({
  listUsers: vi.fn(),
}))

describe('IssueDetailPage', () => {
  beforeEach(() => {
    vi.mocked(getIssue).mockResolvedValue(sampleIssue)
    vi.mocked(getIssueHistory).mockResolvedValue(sampleHistory)
    vi.mocked(listUsers).mockResolvedValue([sampleUser])
    vi.mocked(listOutboundJobs).mockResolvedValue([])
    vi.mocked(enqueueEscalationNotification).mockResolvedValue(sampleOutboundJob)
    vi.mocked(recalculateTriage).mockResolvedValue({
      previousPriority: 'P3',
      currentPriority: 'P1',
      changed: true,
      issue: sampleIssue,
    })
  })

  it('renders issue detail, triage explanation, and recalculation result', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/issues/10']}>
        <Routes>
          <Route path="/issues/:id" element={<IssueDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText(sampleIssue.title)).toBeInTheDocument()
    expect(screen.getByText('Triage Explanation')).toBeInTheDocument()
    expect(screen.getByText('+50')).toBeInTheDocument()
    expect(screen.getByText('Issue history')).toBeInTheDocument()
    expect(screen.getByText('ISSUE CREATED')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Recalculate Priority' }))
    expect(await screen.findByText('P3 -> P1')).toBeInTheDocument()
  })

  it('advances status to the next valid workflow step', async () => {
    const user = userEvent.setup()
    vi.mocked(changeIssueStatus).mockResolvedValue(issueWith({ status: 'RESOLVED' }))

    render(
      <MemoryRouter initialEntries={['/issues/10']}>
        <Routes>
          <Route path="/issues/:id" element={<IssueDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await user.click(await screen.findByRole('button', { name: 'Move to Resolved' }))

    expect(changeIssueStatus).toHaveBeenCalledWith(10, 'RESOLVED')
    expect(await screen.findByRole('button', { name: 'Move to Closed' })).toBeInTheDocument()
  })

  it('unassigns the current user', async () => {
    const user = userEvent.setup()
    vi.mocked(assignIssue).mockResolvedValue(issueWith({ assignedUser: null }))

    render(
      <MemoryRouter initialEntries={['/issues/10']}>
        <Routes>
          <Route path="/issues/:id" element={<IssueDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText(sampleIssue.title)).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Change assignee'), '')

    await waitFor(() => {
      expect(assignIssue).toHaveBeenCalledWith(10, null)
    })
    expect(screen.getByText('Assignee').parentElement).toHaveTextContent('Unassigned')
  })

  it('explains that a closed issue cannot be reopened', async () => {
    vi.mocked(getIssue).mockResolvedValue(issueWith({ status: 'CLOSED' }))

    render(
      <MemoryRouter initialEntries={['/issues/10']}>
        <Routes>
          <Route path="/issues/:id" element={<IssueDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(
      await screen.findByText('This issue is closed and cannot be reopened in the current workflow.'),
    ).toBeInTheDocument()
  })

  it('shows an error when the issue cannot be loaded', async () => {
    vi.mocked(getIssue).mockRejectedValue(new Error('Issue 1042 was not found'))

    render(
      <MemoryRouter initialEntries={['/issues/1042']}>
        <Routes>
          <Route path="/issues/:id" element={<IssueDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('alert')).toHaveTextContent('Issue 1042 was not found')
  })

  it('queues an escalation notification and shows job status', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/issues/10']}>
        <Routes>
          <Route path="/issues/:id" element={<IssueDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await user.click(await screen.findByRole('button', { name: 'Queue escalation notification' }))

    expect(enqueueEscalationNotification).toHaveBeenCalledWith(10)
    expect(await screen.findByText('Pending')).toBeInTheDocument()
    expect(screen.getByText('ESCALATION_NOTIFICATION:10')).toBeInTheDocument()
  })

  it('shows a trigger error without applying retry logic in the page', async () => {
    const user = userEvent.setup()
    vi.mocked(enqueueEscalationNotification).mockRejectedValue(new Error('Cannot queue an escalation notification for a closed issue'))

    render(
      <MemoryRouter initialEntries={['/issues/10']}>
        <Routes>
          <Route path="/issues/:id" element={<IssueDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await user.click(await screen.findByRole('button', { name: 'Queue escalation notification' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Cannot queue an escalation notification for a closed issue',
    )
  })
})
