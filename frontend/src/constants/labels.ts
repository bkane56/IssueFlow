import type { Category, IssueStatus, Priority, Severity } from '../types/issue'
import type { OutboundJobStatus } from '../types/outbound'

export const STATUS_OPTIONS: IssueStatus[] = ['NEW', 'TRIAGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']
export const PRIORITY_OPTIONS: Priority[] = ['P1', 'P2', 'P3', 'P4']
export const SEVERITY_OPTIONS: Severity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
export const CATEGORY_OPTIONS: Category[] = [
  'FRONTEND',
  'BACKEND',
  'DATABASE',
  'INFRASTRUCTURE',
  'SECURITY',
  'INTEGRATION',
  'OTHER',
]

export const STATUS_LABELS: Record<IssueStatus, string> = {
  NEW: 'New',
  TRIAGED: 'Triaged',
  IN_PROGRESS: 'In Progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
}

export const PRIORITY_LABELS: Record<Priority, string> = {
  P1: 'P1',
  P2: 'P2',
  P3: 'P3',
  P4: 'P4',
}

export const SEVERITY_LABELS: Record<Severity, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
}

export const CATEGORY_LABELS: Record<Category, string> = {
  FRONTEND: 'Frontend',
  BACKEND: 'Backend',
  DATABASE: 'Database',
  INFRASTRUCTURE: 'Infrastructure',
  SECURITY: 'Security',
  INTEGRATION: 'Integration',
  OTHER: 'Other',
}

export const NEXT_STATUS: Partial<Record<IssueStatus, IssueStatus>> = {
  NEW: 'TRIAGED',
  TRIAGED: 'IN_PROGRESS',
  IN_PROGRESS: 'RESOLVED',
  RESOLVED: 'CLOSED',
}

export const OUTBOUND_JOB_STATUS_LABELS: Record<OutboundJobStatus, string> = {
  PENDING: 'Pending',
  PROCESSING: 'Processing',
  RETRY_SCHEDULED: 'Retry scheduled',
  SUCCEEDED: 'Succeeded',
  FAILED: 'Failed',
}
