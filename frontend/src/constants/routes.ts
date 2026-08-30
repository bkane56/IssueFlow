export const ROUTES = {
  dashboard: '/',
  issues: '/issues',
  newIssue: '/issues/new',
  issueDetail: '/issues/:id',
  editIssue: '/issues/:id/edit',
  users: '/users',
} as const

export function issueDetailPath(id: number): string {
  return `/issues/${id}`
}

export function issueEditPath(id: number): string {
  return `/issues/${id}/edit`
}
