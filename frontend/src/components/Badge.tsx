import { CATEGORY_LABELS, OUTBOUND_JOB_STATUS_LABELS, PRIORITY_LABELS, SEVERITY_LABELS, STATUS_LABELS } from '../constants/labels'
import type { Category, IssueStatus, Priority, Severity } from '../types/issue'
import type { OutboundJobStatus } from '../types/outbound'

interface BadgeProps {
  kind: 'priority' | 'severity' | 'status' | 'category' | 'outbound'
  value: Priority | Severity | IssueStatus | Category | OutboundJobStatus
}

function labelFor(kind: BadgeProps['kind'], value: BadgeProps['value']): string {
  if (kind === 'priority') {
    return PRIORITY_LABELS[value as Priority]
  }
  if (kind === 'severity') {
    return SEVERITY_LABELS[value as Severity]
  }
  if (kind === 'status') {
    return STATUS_LABELS[value as IssueStatus]
  }
  if (kind === 'outbound') {
    return OUTBOUND_JOB_STATUS_LABELS[value as OutboundJobStatus]
  }
  return CATEGORY_LABELS[value as Category]
}

export function Badge({ kind, value }: BadgeProps) {
  return (
    <span className={`badge badge-${kind} badge-${String(value).toLowerCase().replaceAll('_', '-')}`}>
      {labelFor(kind, value)}
    </span>
  )
}
