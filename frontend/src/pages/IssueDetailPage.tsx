import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { assignIssue, changeIssueStatus, getIssue, getIssueHistory, recalculateTriage } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { Badge } from '../components/Badge'
import { HistoryTimeline } from '../components/HistoryTimeline'
import { StatusMessage } from '../components/StatusMessage'
import { TriageExplanation } from '../components/TriageExplanation'
import { NEXT_STATUS, STATUS_LABELS } from '../constants/labels'
import { issueEditPath } from '../constants/routes'
import { useAsync } from '../hooks/useAsync'
import { useDocumentTitle } from '../hooks/useDocumentTitle'
import type { Priority } from '../types/issue'
import { formatBoolean, formatDateTime } from '../utils/format'

export function IssueDetailPage() {
  const { id } = useParams()
  const issueId = Number(id)
  const issueQuery = useAsync(() => getIssue(issueId), [issueId])
  const historyQuery = useAsync(() => getIssueHistory(issueId), [issueId])
  const usersQuery = useAsync(listUsers, [])
  const [actionError, setActionError] = useState<string | null>(null)
  const [previousPriority, setPreviousPriority] = useState<Priority | null>(null)

  const issue = issueQuery.data
  const nextStatus = issue ? NEXT_STATUS[issue.status] : undefined
  const pageTitle = issue ? `${issue.title} - IssueFlow` : 'Issue detail - IssueFlow'
  useDocumentTitle(pageTitle)

  async function handleStatusChange() {
    if (!issue || !nextStatus) {
      return
    }
    try {
      setActionError(null)
      issueQuery.setData(await changeIssueStatus(issue.id, nextStatus))
      await historyQuery.reload()
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Unable to change status')
    }
  }

  async function handleAssigneeChange(assignedUserId: string) {
    if (!issue) {
      return
    }
    try {
      setActionError(null)
      issueQuery.setData(await assignIssue(issue.id, assignedUserId ? Number(assignedUserId) : null))
      await historyQuery.reload()
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Unable to change assignee')
    }
  }

  async function handleRecalculate() {
    if (!issue) {
      return
    }
    try {
      setActionError(null)
      const result = await recalculateTriage(issue.id)
      setPreviousPriority(result.changed ? result.previousPriority : null)
      issueQuery.setData(result.issue)
      await historyQuery.reload()
    } catch (cause) {
      setActionError(cause instanceof Error ? cause.message : 'Unable to recalculate triage')
    }
  }

  return (
    <div>
      {issue ? (
        <>
          <div className="page-header">
            <div>
              <p className="eyebrow">Issue {issue.id}</p>
              <h1>{issue.title}</h1>
            </div>
            <Link className="button-secondary" to={issueEditPath(issue.id)}>
              Edit issue
            </Link>
          </div>
          <p className="issue-description">{issue.description}</p>
          <dl className="detail-grid">
            <div>
              <dt>Category</dt>
              <dd>
                <Badge kind="category" value={issue.category} />
              </dd>
            </div>
            <div>
              <dt>Severity</dt>
              <dd>
                <Badge kind="severity" value={issue.severity} />
              </dd>
            </div>
            <div>
              <dt>Priority</dt>
              <dd>
                <Badge kind="priority" value={issue.priority} />
              </dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>
                <Badge kind="status" value={issue.status} />
              </dd>
            </div>
            <div>
              <dt>Assignee</dt>
              <dd>{issue.assignedUser?.name ?? 'Unassigned'}</dd>
            </div>
            <div>
              <dt>Customer facing</dt>
              <dd>{formatBoolean(issue.customerFacing)}</dd>
            </div>
            <div>
              <dt>Production impact</dt>
              <dd>{formatBoolean(issue.productionImpact)}</dd>
            </div>
            <div>
              <dt>Affected users</dt>
              <dd>{issue.affectedUsers}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{formatDateTime(issue.createdAt)}</dd>
            </div>
            <div>
              <dt>Updated</dt>
              <dd>{formatDateTime(issue.updatedAt)}</dd>
            </div>
          </dl>

          <section className="action-row">
            {nextStatus ? (
              <button type="button" className="button-primary" onClick={() => void handleStatusChange()}>
                Move to {STATUS_LABELS[nextStatus]}
              </button>
            ) : (
              <p>This issue is closed and cannot be reopened in the current workflow.</p>
            )}
            <label>
              Change assignee
              <select
                value={issue.assignedUser?.id ?? ''}
                onChange={(event) => void handleAssigneeChange(event.target.value)}
              >
                <option value="">Unassigned</option>
                {(usersQuery.data ?? []).map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.name}
                  </option>
                ))}
              </select>
            </label>
            <button type="button" className="button-secondary" onClick={() => void handleRecalculate()}>
              Recalculate Priority
            </button>
          </section>

          <TriageExplanation triage={issue.triage} previousPriority={previousPriority} />

          <section className="panel">
            <h2>Issue history</h2>
            {historyQuery.loading ? <StatusMessage>Loading history.</StatusMessage> : null}
            {historyQuery.error ? <StatusMessage tone="error">{historyQuery.error}</StatusMessage> : null}
            {historyQuery.data ? <HistoryTimeline history={historyQuery.data} /> : null}
          </section>
        </>
      ) : (
        <>
          <h1>Issue detail</h1>
          {issueQuery.loading ? <StatusMessage>Loading issue.</StatusMessage> : null}
          {issueQuery.error ? <StatusMessage tone="error">{issueQuery.error}</StatusMessage> : null}
        </>
      )}
      {actionError ? <StatusMessage tone="error">{actionError}</StatusMessage> : null}
    </div>
  )
}
