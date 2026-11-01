import { Link, useNavigate, useParams } from 'react-router'
import { getApplication, updateApplication } from '../api/applications.js'
import ApplicationForm from '../components/ApplicationForm.jsx'
import { valuesFromApplication } from '../components/applicationFormValues.js'
import { LoadError, LoadingText, NotFound } from '../components/PageStates.jsx'
import { parseIdParam } from '../routeParams.js'
import { useLoad } from './useLoad.js'

const LOAD_FAILED_MESSAGE = '応募を取得できませんでした'
const NOT_FOUND_MESSAGE = '応募が見つかりません'
const UPDATE_FAILED_MESSAGE = '更新できませんでした'

export default function ApplicationEditPage() {
  const navigate = useNavigate()
  const { id } = useParams()
  const applicationId = parseIdParam(id)
  const { phase, data: application, retry, markNotFound } = useLoad(applicationId, getApplication)
  const detailPath = `/applications/${applicationId}`

  return (
    <main className="page page--narrow">
      <h1 className="page-title">応募編集</h1>

      {phase === 'loading' && <LoadingText />}
      {phase === 'error' && <LoadError message={LOAD_FAILED_MESSAGE} onRetry={retry} />}
      {phase === 'notFound' && (
        <NotFound message={NOT_FOUND_MESSAGE} backTo="/applications" backLabel="応募一覧に戻る" />
      )}

      {phase === 'ready' && (
        <>
          <ApplicationForm
            key={application.applicationId}
            initialValues={valuesFromApplication(application)}
            submitLabel="更新する"
            submittingLabel="更新中…"
            failedMessage={UPDATE_FAILED_MESSAGE}
            submit={(body) => updateApplication(application.applicationId, body)}
            // 戻るボタンで送信済みのフォームに戻らないよう、履歴を置き換える
            onSucceeded={() => navigate(detailPath, { replace: true })}
            onNotFound={markNotFound}
          />
          <p className="page-link">
            <Link to={detailPath}>応募詳細に戻る</Link>
          </p>
        </>
      )}

      {phase === 'error' && (
        <p className="page-link">
          <Link to="/applications">応募一覧に戻る</Link>
        </p>
      )}
    </main>
  )
}
