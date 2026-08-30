import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createIssue } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { IssueForm } from '../components/IssueForm'
import { StatusMessage } from '../components/StatusMessage'
import { issueDetailPath } from '../constants/routes'
import { useAsync } from '../hooks/useAsync'
import type { IssueFormValues } from '../types/issue'
import { EMPTY_FORM, validateIssueForm, type FormErrors } from '../utils/formValidation'

export function CreateIssuePage() {
  const navigate = useNavigate()
  const users = useAsync(listUsers, [])
  const [values, setValues] = useState<IssueFormValues>(EMPTY_FORM)
  const [errors, setErrors] = useState<FormErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  async function handleSubmit() {
    const nextErrors = validateIssueForm(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }
    try {
      const created = await createIssue(values)
      navigate(issueDetailPath(created.id))
    } catch (cause) {
      setSubmitError(cause instanceof Error ? cause.message : 'Unable to create the issue')
    }
  }

  return (
    <div>
      <h1>New Issue</h1>
      {submitError ? <StatusMessage tone="error">{submitError}</StatusMessage> : null}
      <IssueForm
        values={values}
        users={users.data ?? []}
        errors={errors}
        submitLabel="Create issue"
        onChange={setValues}
        onSubmit={() => {
          void handleSubmit()
        }}
      />
    </div>
  )
}
