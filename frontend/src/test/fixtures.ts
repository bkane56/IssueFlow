import type { Issue, IssueHistory, TriageResult } from '../types/issue'
import type { User } from '../types/user'

export const sampleUser: User = {
  id: 1,
  name: 'Alex Chen',
  email: 'alex.chen@issueflow.local',
  active: true,
}

export const sampleTriage: TriageResult = {
  score: 110,
  priority: 'P1',
  factors: [
    { name: 'Production impact', score: 50 },
    { name: 'Critical severity', score: 40 },
    { name: 'Customer facing', score: 20 },
  ],
}

export const sampleIssue: Issue = {
  id: 10,
  title: 'Checkout API returning intermittent 500 responses',
  description: 'Customers receive an internal server error during payment confirmation.',
  category: 'BACKEND',
  severity: 'CRITICAL',
  priority: 'P1',
  priorityScore: 110,
  status: 'IN_PROGRESS',
  assignedUser: sampleUser,
  customerFacing: true,
  productionImpact: true,
  affectedUsers: 240,
  createdAt: '2026-08-29T10:00:00Z',
  updatedAt: '2026-08-30T12:00:00Z',
  triage: sampleTriage,
}

export function issueWith(overrides: Partial<Issue>): Issue {
  return { ...sampleIssue, ...overrides }
}

export const sampleHistory: IssueHistory[] = [
  {
    id: 1,
    eventType: 'ISSUE_CREATED',
    oldValue: null,
    newValue: 'NEW',
    description: 'Issue created',
    createdAt: '2026-08-29T10:00:00Z',
  },
  {
    id: 2,
    eventType: 'STATUS_CHANGED',
    oldValue: 'NEW',
    newValue: 'IN_PROGRESS',
    description: 'Status changed from NEW to IN_PROGRESS',
    createdAt: '2026-08-29T11:00:00Z',
  },
]
