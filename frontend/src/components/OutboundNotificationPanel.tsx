import { OUTBOUND_IN_FLIGHT_STATUSES } from '../constants/outbound'
import type { OutboundJob } from '../types/outbound'
import { formatDateTime } from '../utils/format'
import { Badge } from './Badge'
import { StatusMessage } from './StatusMessage'

interface OutboundNotificationPanelProps {
  jobs: OutboundJob[] | null
  loading: boolean
  error: string | null
  triggering: boolean
  canTrigger: boolean
  closedIssue: boolean
  triggerError: string | null
  onTrigger: () => void
  onRefresh: () => void
}

export function OutboundNotificationPanel({
  jobs,
  loading,
  error,
  triggering,
  canTrigger,
  closedIssue,
  triggerError,
  onTrigger,
  onRefresh,
}: OutboundNotificationPanelProps) {
  const job = jobs?.[0] ?? null
  const inFlight = job ? OUTBOUND_IN_FLIGHT_STATUSES.includes(job.status) : false

  return (
    <section className="panel" aria-labelledby="outbound-notification-heading">
      <div className="panel-header">
        <h2 id="outbound-notification-heading">Escalation notification</h2>
        {job ? (
          <button type="button" className="button-secondary" onClick={onRefresh}>
            Refresh status
          </button>
        ) : null}
      </div>
      {loading && !job ? <StatusMessage>Loading outbound jobs.</StatusMessage> : null}
      {error ? <StatusMessage tone="error">{error}</StatusMessage> : null}
      {triggerError ? <StatusMessage tone="error">{triggerError}</StatusMessage> : null}

      {job ? (
        <>
          <p className="form-hint">
            The backend worker performs retries. This page only displays job status.
          </p>
          {inFlight ? (
            <StatusMessage>Outbound job is in progress. Status will refresh automatically.</StatusMessage>
          ) : null}
          <dl className="detail-grid">
            <div>
              <dt>Status</dt>
              <dd>
                <Badge kind="outbound" value={job.status} />
              </dd>
            </div>
            <div>
              <dt>Attempt count</dt>
              <dd>{job.attemptCount}</dd>
            </div>
            <div>
              <dt>Last HTTP status</dt>
              <dd>{job.lastHttpStatus ?? 'None'}</dd>
            </div>
            <div>
              <dt>Last error</dt>
              <dd>{job.lastError ?? 'None'}</dd>
            </div>
            <div>
              <dt>Next retry</dt>
              <dd>
                {inFlight ? (
                  <time dateTime={job.nextAttemptAt}>{formatDateTime(job.nextAttemptAt)}</time>
                ) : (
                  'Not scheduled'
                )}
              </dd>
            </div>
            <div>
              <dt>Completed</dt>
              <dd>
                {job.completedAt ? (
                  <time dateTime={job.completedAt}>{formatDateTime(job.completedAt)}</time>
                ) : (
                  'Not completed'
                )}
              </dd>
            </div>
            <div>
              <dt>Idempotency key</dt>
              <dd>
                <code>{job.idempotencyKey}</code>
              </dd>
            </div>
          </dl>
        </>
      ) : null}

      {!loading && !error && !job ? (
        <p className="empty-state">No escalation notification has been queued for this issue.</p>
      ) : null}

      {canTrigger ? (
        <button
          type="button"
          className="button-primary"
          onClick={onTrigger}
          disabled={triggering}
          aria-busy={triggering}
        >
          {triggering ? 'Queueing notification' : 'Queue escalation notification'}
        </button>
      ) : null}

      {closedIssue && !job ? (
        <p>Closed issues cannot queue an escalation notification.</p>
      ) : null}
    </section>
  )
}
