import { useState } from 'react'
import { listIssues } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { FilterBar } from '../components/FilterBar'
import { IssueTable } from '../components/IssueTable'
import { StatusMessage } from '../components/StatusMessage'
import { EMPTY_ISSUE_FILTERS } from '../constants/filters'
import { useAsync } from '../hooks/useAsync'
import { useDocumentTitle } from '../hooks/useDocumentTitle'
import type { IssueFilters } from '../types/issue'

export function IssueListPage() {
  useDocumentTitle('Issues - IssueFlow')
  const [filters, setFilters] = useState<IssueFilters>(EMPTY_ISSUE_FILTERS)
  const users = useAsync(listUsers, [])
  const issues = useAsync(() => listIssues(filters), [
    filters.search,
    filters.status,
    filters.priority,
    filters.severity,
    filters.category,
    filters.assignedUserId,
  ])

  return (
    <div>
      <h1>Issues</h1>
      <FilterBar
        filters={filters}
        users={users.data ?? []}
        onChange={setFilters}
        onClear={() => setFilters(EMPTY_ISSUE_FILTERS)}
      />
      {issues.loading ? <StatusMessage>Loading issues.</StatusMessage> : null}
      {issues.error ? <StatusMessage tone="error">{issues.error}</StatusMessage> : null}
      {issues.data ? <IssueTable issues={issues.data} /> : null}
    </div>
  )
}
