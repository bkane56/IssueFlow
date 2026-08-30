import type { Issue, IssueFormValues } from '../types/issue'

export function toIssueFormValues(issue: Issue): IssueFormValues {
  return {
    title: issue.title,
    description: issue.description,
    category: issue.category,
    severity: issue.severity,
    assignedUserId: issue.assignedUser ? String(issue.assignedUser.id) : '',
    customerFacing: issue.customerFacing,
    productionImpact: issue.productionImpact,
    affectedUsers: String(issue.affectedUsers),
  }
}
