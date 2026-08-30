import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { sampleIssue } from '../test/fixtures'
import { IssueTable } from './IssueTable'

describe('IssueTable', () => {
  it('renders issue rows and an empty state', () => {
    const { rerender } = render(
      <MemoryRouter>
        <IssueTable issues={[sampleIssue]} />
      </MemoryRouter>,
    )

    expect(screen.getByText('ID')).toBeInTheDocument()
    expect(screen.getByText(sampleIssue.title)).toBeInTheDocument()
    expect(screen.getByText('Alex Chen')).toBeInTheDocument()
    expect(screen.getByText('P1')).toBeInTheDocument()

    rerender(
      <MemoryRouter>
        <IssueTable issues={[]} />
      </MemoryRouter>,
    )
    expect(screen.getByText('No issues match the current filters.')).toBeInTheDocument()
  })
})
