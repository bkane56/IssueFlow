import { render, screen } from '@testing-library/react'
import { StrictMode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { request } from '../api/client'
import { API_BASE_URL } from '../constants/api'
import { useAsync } from './useAsync'

function UsersProbe() {
  const users = useAsync(() => request<Array<{ name: string }>>('/api/users'), [])
  if (users.error) {
    return <p>{users.error}</p>
  }
  if (!users.data) {
    return <p>Loading users</p>
  }
  return <p>{users.data[0]?.name}</p>
}

describe('useAsync', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('does not issue a second GET when StrictMode remounts', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([{ name: 'Alex Rivera' }]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    render(
      <StrictMode>
        <UsersProbe />
      </StrictMode>,
    )

    expect(await screen.findByText('Alex Rivera')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/api/users`, expect.any(Object))
  })
})
