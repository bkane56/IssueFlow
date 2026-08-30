import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { sampleTriage } from '../test/fixtures'
import { TriageExplanation } from './TriageExplanation'

describe('TriageExplanation', () => {
  it('renders factors, score, and a priority change', () => {
    render(<TriageExplanation triage={sampleTriage} previousPriority="P3" />)

    expect(screen.getByText('Production impact')).toBeInTheDocument()
    expect(screen.getByText('+50')).toBeInTheDocument()
    expect(screen.getByText('Priority score')).toBeInTheDocument()
    expect(screen.getByText('110')).toBeInTheDocument()
    expect(screen.getByText('Assigned priority')).toBeInTheDocument()
    expect(screen.getByText('P3 -> P1')).toBeInTheDocument()
  })
})
