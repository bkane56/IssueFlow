import type { User } from './user'

export type IssueStatus = 'NEW' | 'TRIAGED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type Priority = 'P1' | 'P2' | 'P3' | 'P4'
export type Category =
  | 'FRONTEND'
  | 'BACKEND'
  | 'DATABASE'
  | 'INFRASTRUCTURE'
  | 'SECURITY'
  | 'INTEGRATION'
  | 'OTHER'

export type HistoryEventType =
  | 'ISSUE_CREATED'
  | 'STATUS_CHANGED'
  | 'PRIORITY_CHANGED'
  | 'ASSIGNEE_CHANGED'
  | 'TRIAGE_RECALCULATED'
  | 'ISSUE_UPDATED'
  | 'ESCALATION_NOTIFICATION_QUEUED'
  | 'ESCALATION_NOTIFICATION_ATTEMPT_FAILED'
  | 'ESCALATION_NOTIFICATION_RETRY_SCHEDULED'
  | 'ESCALATION_NOTIFICATION_SUCCEEDED'
  | 'ESCALATION_NOTIFICATION_FAILED'

export interface TriageFactor {
  name: string
  score: number
}

export interface TriageResult {
  score: number
  priority: Priority
  factors: TriageFactor[]
}

export interface Issue {
  id: number
  title: string
  description: string
  category: Category
  severity: Severity
  priority: Priority
  priorityScore: number
  status: IssueStatus
  assignedUser: User | null
  customerFacing: boolean
  productionImpact: boolean
  affectedUsers: number
  createdAt: string
  updatedAt: string
  triage: TriageResult
}

export interface IssueHistory {
  id: number
  eventType: HistoryEventType
  oldValue: string | null
  newValue: string | null
  description: string
  createdAt: string
}

export interface IssueFilters {
  search: string
  status: string
  priority: string
  severity: string
  category: string
  assignedUserId: string
}

export interface IssueFormValues {
  title: string
  description: string
  category: Category
  severity: Severity
  assignedUserId: string
  customerFacing: boolean
  productionImpact: boolean
  affectedUsers: string
}

export interface PriorityChangeResult {
  previousPriority: Priority
  currentPriority: Priority
  changed: boolean
  issue: Issue
}
