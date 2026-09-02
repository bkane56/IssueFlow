import { describe, expect, it } from 'vitest'
import { EMPTY_FORM, validateIssueForm, validateUserForm } from './formValidation'

describe('validateIssueForm', () => {
  it('rejects a negative affected-user count', () => {
    expect(
      validateIssueForm({
        ...EMPTY_FORM,
        title: 'Checkout API returning 500 responses',
        description: 'Payment confirmation fails during peak traffic.',
        affectedUsers: '-1',
      }),
    ).toEqual({ affectedUsers: 'Affected users cannot be negative' })
  })

  it('accepts a complete issue form', () => {
    expect(
      validateIssueForm({
        ...EMPTY_FORM,
        title: 'Checkout API returning 500 responses',
        description: 'Payment confirmation fails during peak traffic.',
        affectedUsers: '0',
      }),
    ).toEqual({})
  })
})

describe('validateUserForm', () => {
  it('rejects an invalid email address', () => {
    expect(validateUserForm('Casey Nguyen', 'not-an-email')).toEqual({
      email: 'Email must be a valid address',
    })
  })

  it('accepts a complete user form', () => {
    expect(validateUserForm('Casey Nguyen', 'casey.nguyen@issueflow.local')).toEqual({})
  })
})
