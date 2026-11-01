import { useId, useState } from 'react'
import { Link } from 'react-router'
import { listSeekers } from '../api/advisor.js'
import { useLogout } from '../auth/useLogout.js'
import { LoadError, LoadingText } from '../components/PageStates.jsx'
import { formatDatePart } from '../format.js'
import { useLoad } from './useLoad.js'
import './AdvisorPages.css'

const LOAD_FAILED_MESSAGE = '求職者一覧を取得できませんでした'
const SORT_BY_ID = 'id'
const SORT_BY_OLDEST_UPDATE = 'oldest'
// 一覧は id を取らないので、useLoad には固定の key を渡す
const LIST_KEY = 'seekers'

function updatedAtValue(seeker) {
  if (seeker.lastUpdatedAt === null) {
    return Number.NEGATIVE_INFINITY
  }
  const time = Date.parse(seeker.lastUpdatedAt)
  return Number.isNaN(time) ? Number.NEGATIVE_INFINITY : time
}

// 応募0件（null）の人は一番古い扱いで先頭。同じ時刻は管理No順にして、並びを毎回同じにする
function sortByOldestUpdate(seekers) {
  return [...seekers].sort((a, b) => updatedAtValue(a) - updatedAtValue(b) || a.userId - b.userId)
}

export default function AdvisorSeekersPage() {
  const { logout, isLoggingOut, logoutError } = useLogout()
  const { phase, data: seekers, retry } = useLoad(LIST_KEY, listSeekers)
  const [sortOrder, setSortOrder] = useState(SORT_BY_ID)
  const sortId = useId()

  const rows = phase === 'ready' && sortOrder === SORT_BY_OLDEST_UPDATE ? sortByOldestUpdate(seekers) : seekers

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

      {phase === 'loading' && <LoadingText />}
      {/* 一覧の API に 404 は無いが、返ってきた時も取得失敗として扱う */}
      {(phase === 'error' || phase === 'notFound') && <LoadError message={LOAD_FAILED_MESSAGE} onRetry={retry} />}

      {phase === 'ready' && seekers.length === 0 && <p className="status-text">登録されている求職者はいません</p>}

      {phase === 'ready' && seekers.length > 0 && (
        <>
          <div className="table-toolbar">
            <label htmlFor={sortId}>並び順</label>
            <select
              className="table-toolbar__select"
              id={sortId}
              value={sortOrder}
              onChange={(event) => setSortOrder(event.target.value)}
            >
              <option value={SORT_BY_ID}>管理No順</option>
              <option value={SORT_BY_OLDEST_UPDATE}>最終更新日の古い順</option>
            </select>
          </div>
          <div className="table-scroll" role="region" aria-label="求職者の一覧" tabIndex={0}>
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col" className="data-table__number">
                    管理No
                  </th>
                  <th scope="col">氏名</th>
                  <th scope="col" className="data-table__number">
                    応募件数
                  </th>
                  <th scope="col">最終更新日</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((seeker) => (
                  <tr key={seeker.userId}>
                    <td className="data-table__number">{seeker.userId}</td>
                    <td>
                      <Link to={`/advisor/seekers/${seeker.userId}`}>{seeker.name}</Link>
                    </td>
                    <td className="data-table__number">{seeker.applicationCount}</td>
                    <td className="data-table__nowrap">{formatDatePart(seeker.lastUpdatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </main>
  )
}
