import type { Priority, TriageResult } from '../types/issue'
import { Badge } from './Badge'

interface TriageExplanationProps {
  triage: TriageResult
  previousPriority?: Priority | null
}

export function TriageExplanation({ triage, previousPriority }: TriageExplanationProps) {
  return (
    <section className="panel">
      <h2>Triage Explanation</h2>
      {previousPriority && previousPriority !== triage.priority ? (
        <p className="priority-change" role="status">
          {previousPriority} {'->'} {triage.priority}
        </p>
      ) : null}
      {triage.factors.length === 0 ? (
        <p>No scoring factors applied. This issue remains at the lowest priority band.</p>
      ) : (
        <ul className="triage-factors">
          {triage.factors.map((factor) => (
            <li key={factor.name}>
              <span>{factor.name}</span>
              <span>+{factor.score}</span>
            </li>
          ))}
        </ul>
      )}
      <div className="triage-total">
        <span>Priority score</span>
        <strong>{triage.score}</strong>
      </div>
      <div className="triage-assigned">
        <span>Assigned priority</span>
        <Badge kind="priority" value={triage.priority} />
      </div>
    </section>
  )
}
