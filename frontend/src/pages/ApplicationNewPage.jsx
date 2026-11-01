import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { createApplication } from '../api/applications.js'
import ApplicationForm from '../components/ApplicationForm.jsx'
import { newApplicationValues } from '../components/applicationFormValues.js'

const CREATE_FAILED_MESSAGE = '登録できませんでした'

export default function ApplicationNewPage() {
  const navigate = useNavigate()
  // 今日の日付は開いた時に一度だけ決める
  const [initialValues] = useState(newApplicationValues)

  return (
    <main className="page page--narrow">
      <h1 className="page-title">応募登録</h1>
      <ApplicationForm
        initialValues={initialValues}
        submitLabel="登録する"
        submittingLabel="登録中…"
        failedMessage={CREATE_FAILED_MESSAGE}
        submit={createApplication}
        // 戻るボタンで送信済みのフォームに戻らないよう、履歴を置き換える
        onSucceeded={() => navigate('/applications', { replace: true })}
      />
      <p className="page-link">
        <Link to="/applications">応募一覧に戻る</Link>
      </p>
    </main>
  )
}
