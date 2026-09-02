import { getDashboard } from '../api/dashboardApi'
import { listIssues } from '../api/issuesApi'
import { IssueTable } from '../components/IssueTable'
import { StatusMessage } from '../components/StatusMessage'
import { EMPTY_ISSUE_FILTERS } from '../constants/filters'
import { useAsync } from '../hooks/useAsync'
import { useDocumentTitle } from '../hooks/useDocumentTitle'

export function DashboardPage() {
  useDocumentTitle('Dashboard - IssueFlow')
  const dashboard = useAsync(getDashboard, [])
  const issues = useAsync(() => listIssues(EMPTY_ISSUE_FILTERS), [])

  const highPriority = (issues.data ?? [])
    .filter((issue) => issue.status !== 'RESOLVED' && issue.status !== 'CLOSED')
    .sort((left, right) => right.priorityScore - left.priorityScore)
    .slice(0, 6)

  return (
    <div>
      <h1>Dashboard</h1>
      {dashboard.loading ? <StatusMessage>Loading dashboard statistics.</StatusMessage> : null}
      {dashboard.error ? <StatusMessage tone="error">{dashboard.error}</StatusMessage> : null}
      {dashboard.data ? (
        <section className="stat-grid">
          <article className="stat-card">
            <h2>Open Issues</h2>
            <p>{dashboard.data.open}</p>
          </article>
          <article className="stat-card">
            <h2>Critical Issues</h2>
            <p>{dashboard.data.critical}</p>
          </article>
          <article className="stat-card">
            <h2>In Progress</h2>
            <p>{dashboard.data.inProgress}</p>
          </article>
          <article className="stat-card">
            <h2>Resolved</h2>
            <p>{dashboard.data.resolved}</p>
          </article>
        </section>
      ) : null}

      <section>
        <h2>Highest priority open issues</h2>
        {issues.loading ? <StatusMessage>Loading issues.</StatusMessage> : null}
        {issues.error ? <StatusMessage tone="error">{issues.error}</StatusMessage> : null}
        {issues.data ? <IssueTable issues={highPriority} /> : null}
      </section>
    </div>
  )
}
