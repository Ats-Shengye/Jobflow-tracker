import { Navigate, Outlet } from 'react-router'
import { useAuth } from './AuthContext.js'
import { homePathFor } from '../roles.js'

// 画面の出し分けは見た目のためだけ。権限の確認はサーバーが API ごとに行っている
export function RequireRole({ role }) {
  const { user } = useAuth()
  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (user.role !== role) {
    return <Navigate to={homePathFor(user.role)} replace />
  }
  return <Outlet />
}

// ログイン画面・登録画面は、ログイン済みなら role のトップへ移す
export function GuestOnly() {
  const { user } = useAuth()
  if (user) {
    return <Navigate to={homePathFor(user.role)} replace />
  }
  return <Outlet />
}

// ルートや存在しない URL は、ログイン状態に合わせた入口へ移す
export function EntryRedirect() {
  const { user } = useAuth()
  return <Navigate to={user ? homePathFor(user.role) : '/login'} replace />
}
