import { Route, Routes } from 'react-router'
import { EntryRedirect, GuestOnly, RequireRole } from './auth/RouteGuards.jsx'
import AdvisorSeekerApplicationsPage from './pages/AdvisorSeekerApplicationsPage.jsx'
import AdvisorSeekersPage from './pages/AdvisorSeekersPage.jsx'
import ApplicationDetailPage from './pages/ApplicationDetailPage.jsx'
import ApplicationEditPage from './pages/ApplicationEditPage.jsx'
import ApplicationListPage from './pages/ApplicationListPage.jsx'
import ApplicationNewPage from './pages/ApplicationNewPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import SignupPage from './pages/SignupPage.jsx'
import { ROLE_ADVISOR, ROLE_SEEKER } from './roles.js'

export default function App() {
  return (
    <Routes>
      <Route element={<GuestOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      <Route element={<RequireRole role={ROLE_SEEKER} />}>
        <Route path="/applications" element={<ApplicationListPage />} />
        <Route path="/applications/new" element={<ApplicationNewPage />} />
        <Route path="/applications/:id" element={<ApplicationDetailPage />} />
        <Route path="/applications/:id/edit" element={<ApplicationEditPage />} />
      </Route>

      <Route element={<RequireRole role={ROLE_ADVISOR} />}>
        <Route path="/advisor/seekers" element={<AdvisorSeekersPage />} />
        <Route path="/advisor/seekers/:id" element={<AdvisorSeekerApplicationsPage />} />
      </Route>

      <Route path="*" element={<EntryRedirect />} />
    </Routes>
  )
}
