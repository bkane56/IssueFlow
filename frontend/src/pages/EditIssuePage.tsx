import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getIssue, updateIssue } from '../api/issuesApi'
import { listUsers } from '../api/usersApi'
import { IssueForm } from '../components/IssueForm'
import { StatusMessage } from '../components/StatusMessage'
import { issueDetailPath } from '../constants/routes'
import { useAsync } from '../hooks/useAsync'
import { useDocumentTitle } from '../hooks/useDocumentTitle'
import type { IssueFormValues } from '../types/issue'
import { validateIssueForm, type FormErrors } from '../utils/formValidation'
import { toIssueFormValues } from '../utils/issueForm'

export function EditIssuePage() {
  useDocumentTitle('Edit Issue - IssueFlow')
  const { id } = useParams()
  const issueId = Number(id)
  const navigate = useNavigate()
  const issue = useAsync(() => getIssue(issueId), [issueId])
  const users = useAsync(listUsers, [])
  const [draft, setDraft] = useState<IssueFormValues | null>(null)
  const [errors, setErrors] = useState<FormErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const values = draft ?? (issue.data ? toIssueFormValues(issue.data) : null)

  async function handleSubmit() {
    if (!values) {
      return
    }
    const nextErrors = validateIssueForm(values)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }
    try {
      await updateIssue(issueId, values)
      navigate(issueDetailPath(issueId))
    } catch (cause) {
      setSubmitError(cause instanceof Error ? cause.message : 'Unable to update the issue')
    }
  }

  return (
    <div>
      <h1>Edit Issue</h1>
      {issue.loading ? <StatusMessage>Loading issue.</StatusMessage> : null}
      {issue.error ? <StatusMessage tone="error">{issue.error}</StatusMessage> : null}
      {submitError ? <StatusMessage tone="error">{submitError}</StatusMessage> : null}
      {values ? (
        <IssueForm
          values={values}
          users={users.data ?? []}
          errors={errors}
          submitLabel="Save changes"
          onChange={setDraft}
          onSubmit={() => {
            void handleSubmit()
          }}
        />
      ) : null}
    </div>
  )
}
