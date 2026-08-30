import type { IssueFormValues } from '../types/issue'

export interface FormErrors {
  title?: string
  description?: string
  affectedUsers?: string
}

export function validateIssueForm(values: IssueFormValues): FormErrors {
  const errors: FormErrors = {}
  if (!values.title.trim()) {
    errors.title = 'Title is required'
  }
  if (!values.description.trim()) {
    errors.description = 'Description is required'
  }
  const affectedUsers = Number(values.affectedUsers)
  if (Number.isNaN(affectedUsers) || affectedUsers < 0) {
    errors.affectedUsers = 'Affected users cannot be negative'
  }
  return errors
}

export interface UserFormErrors {
  name?: string
  email?: string
}

export function validateUserForm(name: string, email: string): UserFormErrors {
  const errors: UserFormErrors = {}
  if (!name.trim()) {
    errors.name = 'Name is required'
  }
  if (!email.trim()) {
    errors.email = 'Email is required'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
    errors.email = 'Email must be a valid address'
  }
  return errors
}

export const EMPTY_FORM: IssueFormValues = {
  title: '',
  description: '',
  category: 'BACKEND',
  severity: 'MEDIUM',
  assignedUserId: '',
  customerFacing: false,
  productionImpact: false,
  affectedUsers: '0',
}
