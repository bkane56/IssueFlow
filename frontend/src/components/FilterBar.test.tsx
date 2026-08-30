import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { sampleUser } from '../test/fixtures'
import type { IssueFilters } from '../types/issue'
import { FilterBar } from './FilterBar'

const emptyFilters: IssueFilters = {
  search: '',
  status: '',
  priority: '',
  severity: '',
  category: '',
  assignedUserId: '',
}

describe('FilterBar', () => {
  it('updates search and status filters', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    const onClear = vi.fn()

    render(
      <FilterBar filters={emptyFilters} users={[sampleUser]} onChange={onChange} onClear={onClear} />,
    )

    await user.type(screen.getByLabelText('Search'), 'checkout')
    expect(onChange).toHaveBeenCalled()

    await user.selectOptions(screen.getByLabelText('Status'), 'IN_PROGRESS')
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ status: 'IN_PROGRESS' }))

    await user.click(screen.getByRole('button', { name: 'Clear filters' }))
    expect(onClear).toHaveBeenCalled()
  })
})
