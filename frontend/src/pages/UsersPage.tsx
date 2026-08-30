import { useState } from 'react'
import { createUser, listUsers } from '../api/usersApi'
import { StatusMessage } from '../components/StatusMessage'
import { useAsync } from '../hooks/useAsync'
import { validateUserForm, type UserFormErrors } from '../utils/formValidation'

export function UsersPage() {
  const users = useAsync(listUsers, [])
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [errors, setErrors] = useState<UserFormErrors>({})
  const [submitError, setSubmitError] = useState<string | null>(null)

  async function handleSubmit() {
    const nextErrors = validateUserForm(name, email)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) {
      return
    }
    try {
      setSubmitError(null)
      await createUser(name.trim(), email.trim())
      setName('')
      setEmail('')
      await users.reload()
    } catch (cause) {
      setSubmitError(cause instanceof Error ? cause.message : 'Unable to add the user')
    }
  }

  return (
    <div>
      <h1>Users</h1>
      <p>Add team members so they can be assigned to issues.</p>
      {submitError ? <StatusMessage tone="error">{submitError}</StatusMessage> : null}
      <form
        className="issue-form"
        onSubmit={(event) => {
          event.preventDefault()
          void handleSubmit()
        }}
      >
        <div className="form-grid">
          <label>
            Name
            <input value={name} onChange={(event) => setName(event.target.value)} />
            {errors.name ? <span className="field-error">{errors.name}</span> : null}
          </label>
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            {errors.email ? <span className="field-error">{errors.email}</span> : null}
          </label>
        </div>
        <button type="submit" className="button-primary">
          Add user
        </button>
      </form>

      {users.loading ? <StatusMessage>Loading users.</StatusMessage> : null}
      {users.error ? <StatusMessage tone="error">{users.error}</StatusMessage> : null}
      {users.data ? (
        users.data.length === 0 ? (
          <p className="empty-state">No users have been added yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {users.data.map((user) => (
                  <tr key={user.id}>
                    <td>{user.name}</td>
                    <td>{user.email}</td>
                    <td>{user.active ? 'Active' : 'Inactive'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}
    </div>
  )
}
