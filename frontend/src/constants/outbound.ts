import type { OutboundJobStatus } from '../types/outbound'

export const OUTBOUND_POLL_INTERVAL_MS = 2000

export const OUTBOUND_IN_FLIGHT_STATUSES: OutboundJobStatus[] = [
  'PENDING',
  'PROCESSING',
  'RETRY_SCHEDULED',
]
