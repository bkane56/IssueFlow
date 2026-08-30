import type { FormEvent } from 'react'
import {
  CATEGORY_LABELS,
  CATEGORY_OPTIONS,
  SEVERITY_LABELS,
  SEVERITY_OPTIONS,
} from '../constants/labels'
import type { IssueFormValues } from '../types/issue'
import type { User } from '../types/user'
import type { FormErrors } from '../utils/formValidation'

interface IssueFormProps {
  values: IssueFormValues
  users: User[]
  errors: FormErrors
  submitLabel: string
  onChange: (values: IssueFormValues) => void
  onSubmit: () => void
}

export function IssueForm({ values, users, errors, submitLabel, onChange, onSubmit }: IssueFormProps) {
  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit()
  }

  return (
    <form className="issue-form" onSubmit={handleSubmit}>
      <label>
        Title
        <input
          value={values.title}
          onChange={(event) => onChange({ ...values, title: event.target.value })}
        />
        {errors.title ? <span className="field-error">{errors.title}</span> : null}
      </label>
      <label>
        Description
        <textarea
          rows={6}
          value={values.description}
          onChange={(event) => onChange({ ...values, description: event.target.value })}
        />
        {errors.description ? <span className="field-error">{errors.description}</span> : null}
      </label>
      <div className="form-grid">
        <label>
          Category
          <select
            value={values.category}
            onChange={(event) => onChange({ ...values, category: event.target.value as IssueFormValues['category'] })}
          >
            {CATEGORY_OPTIONS.map((category) => (
              <option key={category} value={category}>
                {CATEGORY_LABELS[category]}
              </option>
            ))}
          </select>
        </label>
        <label>
          Severity
          <select
            value={values.severity}
            onChange={(event) => onChange({ ...values, severity: event.target.value as IssueFormValues['severity'] })}
          >
            {SEVERITY_OPTIONS.map((severity) => (
              <option key={severity} value={severity}>
                {SEVERITY_LABELS[severity]}
              </option>
            ))}
          </select>
        </label>
        <label>
          Assignee
          <select
            value={values.assignedUserId}
            onChange={(event) => onChange({ ...values, assignedUserId: event.target.value })}
          >
            <option value="">Unassigned</option>
            {users.map((user) => (
              <option key={user.id} value={user.id}>
                {user.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Affected users
          <input
            type="number"
            min={0}
            value={values.affectedUsers}
            onChange={(event) => onChange({ ...values, affectedUsers: event.target.value })}
          />
          {errors.affectedUsers ? <span className="field-error">{errors.affectedUsers}</span> : null}
        </label>
      </div>
      <label className="checkbox-label">
        <input
          type="checkbox"
          checked={values.customerFacing}
          onChange={(event) => onChange({ ...values, customerFacing: event.target.checked })}
        />
        Customer facing
      </label>
      <label className="checkbox-label">
        <input
          type="checkbox"
          checked={values.productionImpact}
          onChange={(event) => onChange({ ...values, productionImpact: event.target.checked })}
        />
        Production impact
      </label>
      <p className="form-hint">Priority is calculated by the backend triage engine after you save.</p>
      <button type="submit" className="button-primary">
        {submitLabel}
      </button>
    </form>
  )
}
