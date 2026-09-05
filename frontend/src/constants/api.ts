export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
export const HTTP_METHOD_GET = 'GET'

export const API_PATHS = {
  issues: '/api/issues',
  users: '/api/users',
  dashboard: '/api/dashboard',
  outboundJobs: '/api/outbound-jobs',
} as const

export const SWAGGER_URL = `${API_BASE_URL}/swagger-ui/index.html`
