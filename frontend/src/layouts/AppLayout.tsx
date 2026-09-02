import { NavLink, Outlet } from 'react-router-dom'
import { SWAGGER_URL } from '../constants/api'
import { ROUTES } from '../constants/routes'

export function AppLayout() {
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to main content
      </a>
      <header className="app-header">
        <div className="brand">
          <strong>IssueFlow</strong>
          <span>Incident triage</span>
        </div>
        <nav aria-label="Main navigation">
          <NavLink to={ROUTES.dashboard} end>
            Dashboard
          </NavLink>
          <NavLink to={ROUTES.issues} end>
            Issues
          </NavLink>
          <NavLink to={ROUTES.newIssue}>New Issue</NavLink>
          <NavLink to={ROUTES.users}>Users</NavLink>
          <a href={SWAGGER_URL} target="_blank" rel="noreferrer" aria-label="API Docs (opens in new tab)">
            API Docs
          </a>
        </nav>
      </header>
      <main id="main-content" className="app-main" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}
