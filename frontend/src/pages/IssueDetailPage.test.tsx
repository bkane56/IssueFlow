import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getIssue, getIssueHistory, recalculateTriage } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { sampleHistory, sampleIssue, sampleUser } from '../test/fixtures'
import { IssueDetailPage } from './IssueDetailPage'

vi.mock('../api/issuesApi', () => ({
  getIssue: vi.fn(),
  getIssueHistory: vi.fn(),
  assignIssue: vi.fn(),
  changeIssueStatus: vi.fn(),
  recalculateTriage: vi.fn(),
}))

vi.mock('../api/usersApi', () => ({
  listUsers: vi.fn(),
}))

describe('IssueDetailPage', () => {
  beforeEach(() => {
    vi.mocked(getIssue).mockResolvedValue(sampleIssue)
    vi.mocked(getIssueHistory).mockResolvedValue(sampleHistory)
    vi.mocked(listUsers).mockResolvedValue([sampleUser])
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
})
