import { NavLink, Outlet } from 'react-router-dom'
import { SWAGGER_URL } from '../constants/api'
import { ROUTES } from '../constants/routes'

export function AppLayout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">
          <strong>IssueFlow</strong>
          <span>Incident triage</span>
        </div>
        <nav>
          <NavLink to={ROUTES.dashboard} end>
            Dashboard
          </NavLink>
          <NavLink to={ROUTES.issues} end>
            Issues
          </NavLink>
          <NavLink to={ROUTES.newIssue}>New Issue</NavLink>
          <a href={SWAGGER_URL} target="_blank" rel="noreferrer">
            API Docs
          </a>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
