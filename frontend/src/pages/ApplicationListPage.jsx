import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { deleteApplication, listApplications } from '../api/applications.js'
import { useLogout } from '../auth/useLogout.js'
import StatusBadge from '../components/StatusBadge.jsx'
import { formatDate } from '../format.js'
import './ApplicationListPage.css'

const LOAD_FAILED_MESSAGE = '応募一覧を取得できませんでした'
const DELETE_FAILED_MESSAGE = '削除できませんでした'

export default function ApplicationListPage() {
  const { logout, isLoggingOut, logoutError } = useLogout()
  const [list, setList] = useState({ phase: 'loading', items: [] })
  const [reloadKey, setReloadKey] = useState(0)
  const [deletingId, setDeletingId] = useState(null)
  const [deleteError, setDeleteError] = useState('')

  useEffect(() => {
    let active = true
    listApplications().then((result) => {
      if (active) {
        setList(result.ok ? { phase: 'ready', items: result.data } : { phase: 'error', items: [] })
      }
    })
    return () => {
      active = false
    }
  }, [reloadKey])

  function reload() {
    setReloadKey((key) => key + 1)
  }

  function retry() {
    setList({ phase: 'loading', items: [] })
    reload()
  }

  async function handleDelete(application) {
    if (deletingId !== null) {
      return
    }
    if (!window.confirm(`${application.companyName}への応募を削除しますか？`)) {
      return
    }
    setDeletingId(application.applicationId)
    setDeleteError('')
    const result = await deleteApplication(application.applicationId)
    setDeletingId(null)
    if (result.ok) {
      reload()
      return
    }
    // 401 は共通の処理がログイン画面へ移す。404（別の画面で消した等）は一覧を今の状態に合わせる
    if (result.status !== 401) {
      setDeleteError(DELETE_FAILED_MESSAGE)
    }
    if (result.status === 404) {
      reload()
    }
  }

  return (
    <main className="page page--narrow">
      <header className="page-header">
        <h1 className="page-title">応募一覧</h1>
        <button className="button button--secondary button--small" type="button" onClick={logout} disabled={isLoggingOut}>
          ログアウト
        </button>
      </header>
      {logoutError && (
        <p className="notice notice--error" role="alert">
          {logoutError}
        </p>
      )}

      <Link className="button button--primary button--block" to="/applications/new">
        ＋ 新規登録
      </Link>

      {deleteError && (
        <p className="notice notice--error" role="alert">
          {deleteError}
        </p>
      )}

      {list.phase === 'loading' && (
        <p className="status-text" role="status">
          読み込み中…
        </p>
      )}

      {list.phase === 'error' && (
        <div className="notice notice--error" role="alert">
          <p>{LOAD_FAILED_MESSAGE}</p>
          <button className="button button--secondary button--small" type="button" onClick={retry}>
            再読み込み
          </button>
        </div>
      )}

      {list.phase === 'ready' && list.items.length === 0 && (
        <p className="status-text">まだ応募が登録されていません</p>
      )}

      {list.phase === 'ready' && list.items.length > 0 && (
        <ul className="card-list">
          {list.items.map((application) => (
            <li className="application-card" key={application.applicationId}>
              <Link className="application-card__link" to={`/applications/${application.applicationId}`}>
                <span className="application-card__head">
                  <span className="application-card__company">{application.companyName}</span>
                  <StatusBadge status={application.status} interviewRound={application.interviewRound} />
                </span>
                <span className="application-card__position">{application.position}</span>
                <span className="application-card__date">申込日 {formatDate(application.appliedDate)}</span>
              </Link>
              <button
                className="button button--danger button--small application-card__delete"
                type="button"
                onClick={() => handleDelete(application)}
                disabled={deletingId !== null}
                aria-label={`${application.companyName}への応募を削除`}
              >
                削除
              </button>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
