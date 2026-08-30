import type { IssueHistory } from '../types/issue'
import { formatDateTime } from '../utils/format'

interface HistoryTimelineProps {
  history: IssueHistory[]
}

export function HistoryTimeline({ history }: HistoryTimelineProps) {
  if (history.length === 0) {
    return <p className="empty-state">No history has been recorded for this issue.</p>
  }

  return (
    <ol className="history-timeline">
      {history.map((entry) => (
        <li key={entry.id}>
          <div className="history-meta">
            <strong>{entry.eventType.replaceAll('_', ' ')}</strong>
            <time dateTime={entry.createdAt}>{formatDateTime(entry.createdAt)}</time>
          </div>
          <p>{entry.description}</p>
          {entry.oldValue || entry.newValue ? (
            <p className="history-values">
              {entry.oldValue ?? 'None'} {'->'} {entry.newValue ?? 'None'}
            </p>
          ) : null}
        </li>
      ))}
    </ol>
  )
}
