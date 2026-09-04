import { API_PATHS } from '../constants/api'
import type { OutboundJob } from '../types/outbound'
import { request } from './client'

export function listOutboundJobs(issueId: number): Promise<OutboundJob[]> {
  return request<OutboundJob[]>(`${API_PATHS.issues}/${issueId}/outbound-jobs`)
}

export function enqueueEscalationNotification(issueId: number): Promise<OutboundJob> {
  return request<OutboundJob>(`${API_PATHS.issues}/${issueId}/escalation-notification`, {
    method: 'POST',
  })
}

export function getOutboundJob(jobId: number): Promise<OutboundJob> {
  return request<OutboundJob>(`${API_PATHS.outboundJobs}/${jobId}`)
}
