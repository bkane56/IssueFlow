import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ROUTES } from './constants/routes'
import { AppLayout } from './layouts/AppLayout'
import { CreateIssuePage } from './pages/CreateIssuePage'
import { DashboardPage } from './pages/DashboardPage'
import { EditIssuePage } from './pages/EditIssuePage'
import { IssueDetailPage } from './pages/IssueDetailPage'
import { IssueListPage } from './pages/IssueListPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path={ROUTES.dashboard} element={<DashboardPage />} />
          <Route path={ROUTES.issues} element={<IssueListPage />} />
          <Route path={ROUTES.newIssue} element={<CreateIssuePage />} />
          <Route path={ROUTES.editIssue} element={<EditIssuePage />} />
          <Route path={ROUTES.issueDetail} element={<IssueDetailPage />} />
          <Route path="*" element={<Navigate to={ROUTES.dashboard} replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
