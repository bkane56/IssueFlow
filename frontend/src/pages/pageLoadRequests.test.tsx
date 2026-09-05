import { render, screen } from '@testing-library/react'
import { StrictMode, type ReactElement } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { API_BASE_URL } from '../constants/api'
import { outboundJobWith, sampleHistory, sampleIssue, sampleUser } from '../test/fixtures'
import { CreateIssuePage } from './CreateIssuePage'
import { DashboardPage } from './DashboardPage'
import { EditIssuePage } from './EditIssuePage'
import { IssueDetailPage } from './IssueDetailPage'
import { IssueListPage } from './IssueListPage'
import { UsersPage } from './UsersPage'

function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

function pathFromUrl(input: RequestInfo | URL): string {
  return String(input).replace(API_BASE_URL, '')
}

function countByPath(fetchMock: ReturnType<typeof vi.fn>): Record<string, number> {
  const counts: Record<string, number> = {}
  for (const [url] of fetchMock.mock.calls) {
    const path = pathFromUrl(url as RequestInfo)
    counts[path] = (counts[path] ?? 0) + 1
  }
  return counts
}

function stubApi() {
  const fetchMock = vi.fn((input: RequestInfo | URL) => {
    const path = pathFromUrl(input)
    if (path === '/api/dashboard') {
      return Promise.resolve(jsonResponse({
        open: 1,
        critical: 1,
        inProgress: 1,
        resolved: 0,
      }))
    }
    if (path === '/api/issues') {
      return Promise.resolve(jsonResponse([sampleIssue]))
    }
    if (path === '/api/users') {
      return Promise.resolve(jsonResponse([sampleUser]))
    }
    if (path === '/api/issues/10') {
      return Promise.resolve(jsonResponse(sampleIssue))
    }
    if (path === '/api/issues/10/history') {
      return Promise.resolve(jsonResponse(sampleHistory))
    }
    if (path === '/api/issues/10/outbound-jobs') {
      return Promise.resolve(jsonResponse([outboundJobWith({ status: 'SUCCEEDED' })]))
    }
    return Promise.reject(new Error(`Unexpected request: ${path}`))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function renderPage(ui: ReactElement, route = '/') {
  return render(
    <StrictMode>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </StrictMode>,
  )
}

describe('page load requests under StrictMode', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('Dashboard fetches dashboard and issues once', async () => {
    const fetchMock = stubApi()
    renderPage(<DashboardPage />)

    expect(await screen.findByRole('heading', { name: 'Open Issues' })).toBeInTheDocument()
    expect(await screen.findByText(sampleIssue.title)).toBeInTheDocument()
    expect(countByPath(fetchMock)).toEqual({
      '/api/dashboard': 1,
      '/api/issues': 1,
    })
  })

  it('Issues fetches users and issues once', async () => {
    const fetchMock = stubApi()
    renderPage(<IssueListPage />, '/issues')

    expect(await screen.findByText(sampleIssue.title)).toBeInTheDocument()
    expect(await screen.findByRole('option', { name: sampleUser.name })).toBeInTheDocument()
    expect(countByPath(fetchMock)).toEqual({
      '/api/users': 1,
      '/api/issues': 1,
    })
  })

  it('New Issue fetches users once', async () => {
    const fetchMock = stubApi()
    renderPage(<CreateIssuePage />, '/issues/new')

    expect(await screen.findByRole('heading', { name: 'New Issue' })).toBeInTheDocument()
    expect(await screen.findByRole('option', { name: sampleUser.name })).toBeInTheDocument()
    expect(countByPath(fetchMock)).toEqual({
      '/api/users': 1,
    })
  })

  it('Users fetches users once', async () => {
    const fetchMock = stubApi()
    renderPage(<UsersPage />, '/users')

    expect(await screen.findByRole('heading', { name: 'Users' })).toBeInTheDocument()
    expect(await screen.findByText(sampleUser.name)).toBeInTheDocument()
    expect(countByPath(fetchMock)).toEqual({
      '/api/users': 1,
    })
  })

  it('Issue detail fetches issue, history, users, and outbound jobs once', async () => {
    const fetchMock = stubApi()
    renderPage(
      <Routes>
        <Route path="/issues/:id" element={<IssueDetailPage />} />
      </Routes>,
      '/issues/10',
    )

    expect(await screen.findByRole('heading', { name: sampleIssue.title })).toBeInTheDocument()
    expect(await screen.findByRole('option', { name: sampleUser.name })).toBeInTheDocument()
    expect(countByPath(fetchMock)).toEqual({
      '/api/issues/10': 1,
      '/api/issues/10/history': 1,
      '/api/users': 1,
      '/api/issues/10/outbound-jobs': 1,
    })
  })

  it('Edit issue fetches the issue and users once', async () => {
    const fetchMock = stubApi()
    renderPage(
      <Routes>
        <Route path="/issues/:id/edit" element={<EditIssuePage />} />
      </Routes>,
      '/issues/10/edit',
    )

    expect(await screen.findByRole('heading', { name: 'Edit Issue' })).toBeInTheDocument()
    expect(await screen.findByDisplayValue(sampleIssue.title)).toBeInTheDocument()
    expect(countByPath(fetchMock)).toEqual({
      '/api/issues/10': 1,
      '/api/users': 1,
    })
  })
})
