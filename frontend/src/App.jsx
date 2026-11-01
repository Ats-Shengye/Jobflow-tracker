import { Route, Routes } from 'react-router'
import { EntryRedirect, GuestOnly, RequireRole } from './auth/RouteGuards.jsx'
import AdvisorSeekersPage from './pages/AdvisorSeekersPage.jsx'
import ApplicationListPage from './pages/ApplicationListPage.jsx'
import ComingSoonPage from './pages/ComingSoonPage.jsx'
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
        <Route
          path="/applications/new"
          element={<ComingSoonPage title="応募登録" backTo="/applications" backLabel="応募一覧に戻る" />}
        />
        <Route
          path="/applications/:id"
          element={<ComingSoonPage title="応募詳細" backTo="/applications" backLabel="応募一覧に戻る" />}
        />
        <Route
          path="/applications/:id/edit"
          element={<ComingSoonPage title="応募編集" backTo="/applications" backLabel="応募一覧に戻る" />}
        />
      </Route>

      <Route element={<RequireRole role={ROLE_ADVISOR} />}>
        <Route path="/advisor/seekers" element={<AdvisorSeekersPage />} />
        <Route
          path="/advisor/seekers/:id"
          element={
            <ComingSoonPage title="求職者別応募" backTo="/advisor/seekers" backLabel="求職者一覧に戻る" wide />
          }
        />
      </Route>

      <Route path="*" element={<EntryRedirect />} />
    </Routes>
  )
}
