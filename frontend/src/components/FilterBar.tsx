import {
  CATEGORY_LABELS,
  CATEGORY_OPTIONS,
  PRIORITY_OPTIONS,
  SEVERITY_LABELS,
  SEVERITY_OPTIONS,
  STATUS_LABELS,
  STATUS_OPTIONS,
} from '../constants/labels'
import type { IssueFilters } from '../types/issue'
import type { User } from '../types/user'

interface FilterBarProps {
  filters: IssueFilters
  users: User[]
  onChange: (filters: IssueFilters) => void
  onClear: () => void
}

export function FilterBar({ filters, users, onChange, onClear }: FilterBarProps) {
  function update(field: keyof IssueFilters, value: string) {
    onChange({ ...filters, [field]: value })
  }

  return (
    <form className="filter-bar" onSubmit={(event) => event.preventDefault()}>
      <label>
        Search
        <input
          type="search"
          value={filters.search}
          onChange={(event) => update('search', event.target.value)}
          placeholder="Title or description"
        />
      </label>
      <label>
        Status
        <select value={filters.status} onChange={(event) => update('status', event.target.value)}>
          <option value="">All</option>
          {STATUS_OPTIONS.map((status) => (
            <option key={status} value={status}>
              {STATUS_LABELS[status]}
            </option>
          ))}
        </select>
      </label>
      <label>
        Priority
        <select value={filters.priority} onChange={(event) => update('priority', event.target.value)}>
          <option value="">All</option>
          {PRIORITY_OPTIONS.map((priority) => (
            <option key={priority} value={priority}>
              {priority}
            </option>
          ))}
        </select>
      </label>
      <label>
        Severity
        <select value={filters.severity} onChange={(event) => update('severity', event.target.value)}>
          <option value="">All</option>
          {SEVERITY_OPTIONS.map((severity) => (
            <option key={severity} value={severity}>
              {SEVERITY_LABELS[severity]}
            </option>
          ))}
        </select>
      </label>
      <label>
        Category
        <select value={filters.category} onChange={(event) => update('category', event.target.value)}>
          <option value="">All</option>
          {CATEGORY_OPTIONS.map((category) => (
            <option key={category} value={category}>
              {CATEGORY_LABELS[category]}
            </option>
          ))}
        </select>
      </label>
      <label>
        Assignee
        <select
          value={filters.assignedUserId}
          onChange={(event) => update('assignedUserId', event.target.value)}
        >
          <option value="">All</option>
          {users.map((user) => (
            <option key={user.id} value={user.id}>
              {user.name}
            </option>
          ))}
        </select>
      </label>
      <button type="button" className="button-secondary" onClick={onClear}>
        Clear filters
      </button>
    </form>
  )
}
