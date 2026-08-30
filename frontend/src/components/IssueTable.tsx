import { Link } from 'react-router-dom'
import { issueDetailPath } from '../constants/routes'
import type { Issue } from '../types/issue'
import { formatDateTime } from '../utils/format'
import { Badge } from './Badge'

interface IssueTableProps {
  issues: Issue[]
}

export function IssueTable({ issues }: IssueTableProps) {
  if (issues.length === 0) {
    return <p className="empty-state">No issues match the current filters.</p>
  }

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Title</th>
            <th>Category</th>
            <th>Severity</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Assignee</th>
            <th>Updated</th>
          </tr>
        </thead>
        <tbody>
          {issues.map((issue) => (
            <tr key={issue.id}>
              <td>{issue.id}</td>
              <td>
                <Link to={issueDetailPath(issue.id)}>{issue.title}</Link>
              </td>
              <td>
                <Badge kind="category" value={issue.category} />
              </td>
              <td>
                <Badge kind="severity" value={issue.severity} />
              </td>
              <td>
                <Badge kind="priority" value={issue.priority} />
              </td>
              <td>
                <Badge kind="status" value={issue.status} />
              </td>
              <td>{issue.assignedUser?.name ?? 'Unassigned'}</td>
              <td>{formatDateTime(issue.updatedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
