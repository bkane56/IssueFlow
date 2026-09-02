import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { sampleHistory } from '../test/fixtures'
import { HistoryTimeline } from './HistoryTimeline'

describe('HistoryTimeline', () => {
  it('renders recorded history entries', () => {
    render(<HistoryTimeline history={sampleHistory} />)

    expect(screen.getByText('ISSUE CREATED')).toBeInTheDocument()
    expect(screen.getByText('Issue created')).toBeInTheDocument()
    expect(screen.getByText('STATUS CHANGED')).toBeInTheDocument()
  })

  it('shows an empty state when no history exists', () => {
    render(<HistoryTimeline history={[]} />)

    expect(screen.getByText('No history has been recorded for this issue.')).toBeInTheDocument()
  })
})
