import { CATEGORY_LABELS, PRIORITY_LABELS, SEVERITY_LABELS, STATUS_LABELS } from '../constants/labels'
import type { Category, IssueStatus, Priority, Severity } from '../types/issue'

interface BadgeProps {
  kind: 'priority' | 'severity' | 'status' | 'category'
  value: Priority | Severity | IssueStatus | Category
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
  return CATEGORY_LABELS[value as Category]
}

export function Badge({ kind, value }: BadgeProps) {
  return (
    <span className={`badge badge-${kind} badge-${String(value).toLowerCase().replaceAll('_', '-')}`}>
      {labelFor(kind, value)}
    </span>
  )
}
