import { Link, useParams } from 'react-router'
import { getSeekerApplications } from '../api/advisor.js'
import { LoadError, LoadingText, NotFound } from '../components/PageStates.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import {
  employmentInsuranceLabel,
  formatDate,
  formatDateTime,
  formatOptionalText,
  interviewRoundLabel,
  NO_VALUE,
} from '../format.js'
import { parseIdParam } from '../routeParams.js'
import { useLoad } from './useLoad.js'
import './AdvisorPages.css'

const LOAD_FAILED_MESSAGE = '応募を取得できませんでした'
const NOT_FOUND_MESSAGE = '求職者が見つかりません'

function interviewRoundCell(application) {
  if (application.status !== 'INTERVIEW') {
    return NO_VALUE
  }
  return interviewRoundLabel(application.interviewRound) ?? NO_VALUE
}

function ApplicationsTable({ applications }) {
  return (
    <div className="table-scroll" role="region" aria-label="応募の一覧" tabIndex={0}>
      <table className="data-table data-table--applications">
        <thead>
          <tr>
            <th scope="col">企業名</th>
            <th scope="col">ポジション</th>
            <th scope="col">雇用保険</th>
            <th scope="col">申込日</th>
            <th scope="col">ステータス</th>
            <th scope="col">面接回数</th>
            <th scope="col">給与</th>
            <th scope="col">勤務地</th>
            <th scope="col">メモ</th>
            <th scope="col">最終更新日時</th>
          </tr>
        </thead>
        <tbody>
          {applications.map((application) => (
            <tr key={application.applicationId}>
              <td className="data-table__text">{application.companyName}</td>
              <td className="data-table__text">{application.position}</td>
              <td className="data-table__nowrap">{employmentInsuranceLabel(application.employmentInsurance)}</td>
              <td className="data-table__nowrap">{formatDate(application.appliedDate)}</td>
              <td className="data-table__nowrap">
                <StatusBadge status={application.status} />
              </td>
              <td className="data-table__nowrap">{interviewRoundCell(application)}</td>
              <td className="data-table__text">{formatOptionalText(application.salary)}</td>
              <td className="data-table__text">{formatOptionalText(application.workLocation)}</td>
              <td className="data-table__memo">{formatOptionalText(application.memo)}</td>
              <td className="data-table__nowrap">{formatDateTime(application.updatedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default function AdvisorSeekerApplicationsPage() {
  const { id } = useParams()
  const userId = parseIdParam(id)
  const { phase, data: seeker, retry } = useLoad(userId, getSeekerApplications)

  return (
    <main className="page page--wide">
      <h1 className="page-title">
        {phase === 'ready' ? `管理No.${seeker.userId} ${seeker.name} さんの応募` : '求職者別応募'}
      </h1>

      {phase === 'loading' && <LoadingText />}
      {phase === 'error' && <LoadError message={LOAD_FAILED_MESSAGE} onRetry={retry} />}
      {phase === 'notFound' && (
        <NotFound message={NOT_FOUND_MESSAGE} backTo="/advisor/seekers" backLabel="求職者一覧に戻る" />
      )}

      {phase === 'ready' && seeker.applications.length === 0 && (
        <p className="status-text">まだ応募が登録されていません</p>
      )}
      {phase === 'ready' && seeker.applications.length > 0 && <ApplicationsTable applications={seeker.applications} />}

      {phase !== 'notFound' && (
        <p className="page-link page-link--start">
          <Link to="/advisor/seekers">求職者一覧に戻る</Link>
        </p>
      )}
    </main>
  )
}
