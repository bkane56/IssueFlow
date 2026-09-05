import { API_BASE_URL, HTTP_METHOD_GET } from '../constants/api'
import { ApiError, type ApiErrorBody } from '../types/api'

// Share in-flight GETs so React StrictMode remounts and overlapping callers hit the network once.
const inFlightGets = new Map<string, Promise<unknown>>()

async function parseError(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ApiErrorBody
    if (body.message) {
      return body.message
    }
  } catch {
    // The response was not JSON. Fall through to the status message.
  }
  return `Request failed with status ${response.status}`
}

function requestMethod(options: RequestInit): string {
  return (options.method ?? HTTP_METHOD_GET).toUpperCase()
}

async function executeRequest<T>(path: string, options: RequestInit): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    throw new ApiError(await parseError(response), response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  if (requestMethod(options) !== HTTP_METHOD_GET) {
    return executeRequest<T>(path, options)
  }

  const existing = inFlightGets.get(path)
  if (existing) {
    return existing as Promise<T>
  }

  const pending = executeRequest<T>(path, options).finally(() => {
    inFlightGets.delete(path)
  })
  inFlightGets.set(path, pending)
  return pending
}
