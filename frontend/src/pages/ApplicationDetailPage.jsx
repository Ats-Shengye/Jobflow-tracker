import { Link, useParams } from 'react-router'
import { getApplication } from '../api/applications.js'
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
import './ApplicationDetailPage.css'

const LOAD_FAILED_MESSAGE = '応募を取得できませんでした'
const NOT_FOUND_MESSAGE = '応募が見つかりません'

function DetailRow({ label, children, multiline = false }) {
  return (
    <div className="detail-list__row">
      <dt className="detail-list__label">{label}</dt>
      <dd className={`detail-list__value${multiline ? ' detail-list__value--multiline' : ''}`}>{children}</dd>
    </div>
  )
}

function interviewRoundText(interviewRound) {
  const label = interviewRoundLabel(interviewRound)
  return label ? `${label}面接` : NO_VALUE
}

export default function ApplicationDetailPage() {
  const { id } = useParams()
  const applicationId = parseIdParam(id)
  const { phase, data: application, retry } = useLoad(applicationId, getApplication)

  return (
    <main className="page page--narrow">
      <h1 className="page-title">応募詳細</h1>

      {phase === 'loading' && <LoadingText />}
      {phase === 'error' && <LoadError message={LOAD_FAILED_MESSAGE} onRetry={retry} />}
      {phase === 'notFound' && (
        <NotFound message={NOT_FOUND_MESSAGE} backTo="/applications" backLabel="応募一覧に戻る" />
      )}

      {phase === 'ready' && (
        <>
          <dl className="detail-list">
            <DetailRow label="企業名">{application.companyName}</DetailRow>
            <DetailRow label="ポジション">{application.position}</DetailRow>
            <DetailRow label="雇用保険">{employmentInsuranceLabel(application.employmentInsurance)}</DetailRow>
            <DetailRow label="申込日">{formatDate(application.appliedDate)}</DetailRow>
            <DetailRow label="進捗ステータス">
              <StatusBadge status={application.status} />
            </DetailRow>
            {application.status === 'INTERVIEW' && (
              <DetailRow label="面接回数">{interviewRoundText(application.interviewRound)}</DetailRow>
            )}
            <DetailRow label="給与">{formatOptionalText(application.salary)}</DetailRow>
            <DetailRow label="勤務地">{formatOptionalText(application.workLocation)}</DetailRow>
            <DetailRow label="メモ" multiline>
              {formatOptionalText(application.memo)}
            </DetailRow>
            <DetailRow label="最終更新日時">{formatDateTime(application.updatedAt)}</DetailRow>
          </dl>
          <Link className="button button--primary button--block" to={`/applications/${application.applicationId}/edit`}>
            編集する
          </Link>
        </>
      )}

      {phase !== 'notFound' && (
        <p className="page-link">
          <Link to="/applications">応募一覧に戻る</Link>
        </p>
      )}
    </main>
  )
}
