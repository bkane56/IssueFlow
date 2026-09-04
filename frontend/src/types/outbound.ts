export type OutboundJobStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'RETRY_SCHEDULED'
  | 'SUCCEEDED'
  | 'FAILED'

export type OutboundOperationType = 'ESCALATION_NOTIFICATION'

export interface OutboundJob {
  jobId: number
  operationType: OutboundOperationType
  idempotencyKey: string
  status: OutboundJobStatus
  attemptCount: number
  nextAttemptAt: string
  lastHttpStatus: number | null
  lastError: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
}
