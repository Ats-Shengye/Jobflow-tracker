import { useLogout } from '../auth/useLogout.js'

// 担当者のトップ。表は次の工程で作るので、今はログアウトだけ置く
export default function AdvisorSeekersPage() {
  const { logout, isLoggingOut, logoutError } = useLogout()

  return (
    <main className="page page--wide">
      <header className="page-header">
        <h1 className="page-title">求職者一覧（担当者）</h1>
        <button className="button button--secondary button--small" type="button" onClick={logout} disabled={isLoggingOut}>
          ログアウト
        </button>
      </header>
      {logoutError && (
        <p className="notice notice--error" role="alert">
          {logoutError}
        </p>
      )}
      <p className="status-text">準備中</p>
    </main>
  )
}
