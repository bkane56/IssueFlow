import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { outboundJobWith, sampleOutboundJob } from '../test/fixtures'
import { OutboundNotificationPanel } from './OutboundNotificationPanel'

describe('OutboundNotificationPanel', () => {
  it('renders pending retry information from the job payload', () => {
    render(
      <OutboundNotificationPanel
        jobs={[
          outboundJobWith({
            status: 'RETRY_SCHEDULED',
            attemptCount: 1,
            lastHttpStatus: 503,
            lastError: 'Simulated HTTP 503',
            nextAttemptAt: '2026-09-04T12:00:05Z',
          }),
        ]}
        loading={false}
        error={null}
        triggering={false}
        canTrigger
        closedIssue={false}
        triggerError={null}
        onTrigger={() => undefined}
        onRefresh={() => undefined}
      />,
    )

    expect(screen.getByText('Retry scheduled')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('503')).toBeInTheDocument()
    expect(screen.getByText('Simulated HTTP 503')).toBeInTheDocument()
    expect(screen.getByText('ESCALATION_NOTIFICATION:10')).toBeInTheDocument()
    expect(screen.getByText('The backend worker performs retries. This page only displays job status.')).toBeInTheDocument()
  })

  it('renders a successful outbound job', () => {
    render(
      <OutboundNotificationPanel
        jobs={[
          outboundJobWith({
            status: 'SUCCEEDED',
            attemptCount: 2,
            lastHttpStatus: 200,
            lastError: null,
            completedAt: '2026-09-04T12:00:20Z',
          }),
        ]}
        loading={false}
        error={null}
        triggering={false}
        canTrigger
        closedIssue={false}
        triggerError={null}
        onTrigger={() => undefined}
        onRefresh={() => undefined}
      />,
    )

    expect(screen.getByText('Succeeded')).toBeInTheDocument()
    expect(screen.getByText('Not scheduled')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('renders a permanent failure state', () => {
    render(
      <OutboundNotificationPanel
        jobs={[
          outboundJobWith({
            status: 'FAILED',
            attemptCount: 1,
            lastHttpStatus: 400,
            lastError: 'Simulated HTTP 400',
            completedAt: '2026-09-04T12:00:02Z',
          }),
        ]}
        loading={false}
        error={null}
        triggering={false}
        canTrigger
        closedIssue={false}
        triggerError={null}
        onTrigger={() => undefined}
        onRefresh={() => undefined}
      />,
    )

    expect(screen.getByText('Failed')).toBeInTheDocument()
    expect(screen.getByText('Simulated HTTP 400')).toBeInTheDocument()
    expect(screen.getByText('400')).toBeInTheDocument()
  })

  it('shows trigger loading and error states', async () => {
    const user = userEvent.setup()
    const onTrigger = vi.fn()
    render(
      <OutboundNotificationPanel
        jobs={[]}
        loading={false}
        error={null}
        triggering
        canTrigger
        closedIssue={false}
        triggerError="Unable to queue escalation notification"
        onTrigger={onTrigger}
        onRefresh={() => undefined}
      />,
    )

    const trigger = screen.getByRole('button', { name: 'Queueing notification' })
    expect(trigger).toBeDisabled()
    expect(trigger).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Unable to queue escalation notification')
    await user.click(trigger)
    expect(onTrigger).not.toHaveBeenCalled()
  })

  it('shows an empty state when no job exists', () => {
    render(
      <OutboundNotificationPanel
        jobs={[]}
        loading={false}
        error={null}
        triggering={false}
        canTrigger
        closedIssue={false}
        triggerError={null}
        onTrigger={() => undefined}
        onRefresh={() => undefined}
      />,
    )

    expect(screen.getByText('No escalation notification has been queued for this issue.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Queue escalation notification' })).toBeInTheDocument()
    expect(sampleOutboundJob.status).toBe('PENDING')
  })
})
