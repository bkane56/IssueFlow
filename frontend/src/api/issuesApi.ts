import { API_PATHS } from '../constants/api'
import type {
  Issue,
  IssueFilters,
  IssueFormValues,
  IssueHistory,
  PriorityChangeResult,
} from '../types/issue'
import { request } from './client'

function toQuery(filters: IssueFilters): string {
  const params = new URLSearchParams()
  if (filters.search.trim()) {
    params.set('search', filters.search.trim())
  }
  if (filters.status) {
    params.set('status', filters.status)
  }
  if (filters.priority) {
    params.set('priority', filters.priority)
  }
  if (filters.severity) {
    params.set('severity', filters.severity)
  }
  if (filters.category) {
    params.set('category', filters.category)
  }
  if (filters.assignedUserId) {
    params.set('assignedUserId', filters.assignedUserId)
  }
  const query = params.toString()
  return query ? `?${query}` : ''
}

function toPayload(values: IssueFormValues) {
  return {
    title: values.title.trim(),
    description: values.description.trim(),
    category: values.category,
    severity: values.severity,
    assignedUserId: values.assignedUserId ? Number(values.assignedUserId) : null,
    customerFacing: values.customerFacing,
    productionImpact: values.productionImpact,
    affectedUsers: Number(values.affectedUsers),
  }
}

export function listIssues(filters: IssueFilters): Promise<Issue[]> {
  return request<Issue[]>(`${API_PATHS.issues}${toQuery(filters)}`)
}

export function getIssue(id: number): Promise<Issue> {
  return request<Issue>(`${API_PATHS.issues}/${id}`)
}

export function createIssue(values: IssueFormValues): Promise<Issue> {
  return request<Issue>(API_PATHS.issues, {
    method: 'POST',
    body: JSON.stringify(toPayload(values)),
  })
}

export function updateIssue(id: number, values: IssueFormValues): Promise<Issue> {
  return request<Issue>(`${API_PATHS.issues}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(toPayload(values)),
  })
}

export function changeIssueStatus(id: number, status: string): Promise<Issue> {
  return request<Issue>(`${API_PATHS.issues}/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}

export function assignIssue(id: number, assignedUserId: number | null): Promise<Issue> {
  return request<Issue>(`${API_PATHS.issues}/${id}/assign`, {
    method: 'PATCH',
    body: JSON.stringify({ assignedUserId }),
  })
}

export function recalculateTriage(id: number): Promise<PriorityChangeResult> {
  return request<PriorityChangeResult>(`${API_PATHS.issues}/${id}/triage`, {
    method: 'POST',
  })
}

export function getIssueHistory(id: number): Promise<IssueHistory[]> {
  return request<IssueHistory[]>(`${API_PATHS.issues}/${id}/history`)
}
