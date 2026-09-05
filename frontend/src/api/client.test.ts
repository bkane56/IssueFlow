import { afterEach, describe, expect, it, vi } from 'vitest'
import { API_BASE_URL } from '../constants/api'
import { ApiError } from '../types/api'
import { request } from './client'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('request', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('shares one in-flight GET for the same path', async () => {
    let resolveFetch: ((value: Response) => void) | undefined
    const fetchMock = vi.fn(
      () =>
        new Promise<Response>((resolve) => {
          resolveFetch = resolve
        }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const first = request<{ id: number }>('/api/users')
    const second = request<{ id: number }>('/api/users')

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/api/users`, expect.any(Object))

    resolveFetch?.(jsonResponse({ id: 4 }))
    await expect(Promise.all([first, second])).resolves.toEqual([{ id: 4 }, { id: 4 }])
  })

  it('issues a new GET after the previous one finishes', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ attempt: 1 }))
      .mockResolvedValueOnce(jsonResponse({ attempt: 2 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(request('/api/dashboard')).resolves.toEqual({ attempt: 1 })
    await expect(request('/api/dashboard')).resolves.toEqual({ attempt: 2 })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('does not share in-flight POST requests', async () => {
    const fetchMock = vi.fn().mockImplementation(() => jsonResponse({ created: true }, 201))
    vi.stubGlobal('fetch', fetchMock)

    const first = request('/api/users', {
      method: 'POST',
      body: JSON.stringify({ name: 'Jordan Lee', email: 'jordan.lee@issueflow.local' }),
    })
    const second = request('/api/users', {
      method: 'POST',
      body: JSON.stringify({ name: 'Riley Chen', email: 'riley.chen@issueflow.local' }),
    })

    await Promise.all([first, second])
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('does not reuse a failed GET for the next attempt', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ message: 'Users are unavailable' }, 500))
      .mockResolvedValueOnce(jsonResponse([{ id: 1 }]))
    vi.stubGlobal('fetch', fetchMock)

    await expect(request('/api/users')).rejects.toBeInstanceOf(ApiError)
    await expect(request('/api/users')).resolves.toEqual([{ id: 1 }])
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
