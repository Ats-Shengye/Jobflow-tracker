import { useState } from 'react'
import { Link, useLocation } from 'react-router'
import { fetchCurrentUser, login } from '../api/auth.js'
import { useAuth } from '../auth/AuthContext.js'
import TextField from '../components/TextField.jsx'
import { errorMessageFor } from '../messages.js'

const REQUIRED_MESSAGE = 'ログインIDとパスワードを入力してください'
// どちらが違うかは出さない（登録済みの ID を推測されないため）
const LOGIN_FAILED_MESSAGE = 'ログインIDまたはパスワードが正しくありません'
const FALLBACK_MESSAGE = 'ログインできませんでした。時間をおいてもう一度お試しください'

function loginErrorMessage(result) {
  if (result.status === 401) {
    return LOGIN_FAILED_MESSAGE
  }
  if (result.status === 400) {
    return result.errors.login_id || result.errors.password || result.message || REQUIRED_MESSAGE
  }
  return errorMessageFor(result, FALLBACK_MESSAGE)
}

export default function LoginPage() {
  const { signIn } = useAuth()
  const location = useLocation()
  const justRegistered = location.state?.registered === true
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    if (submitting) {
      return
    }
    // 未入力の確認だけで、値は削らずに送る（パスワードは前後の空白も値のうち）
    if (loginId.trim() === '' || password.trim() === '') {
      setError(REQUIRED_MESSAGE)
      return
    }
    setSubmitting(true)
    setError('')

    const loginResult = await login(loginId, password)
    if (!loginResult.ok) {
      setSubmitting(false)
      setError(loginErrorMessage(loginResult))
      return
    }
    // 遷移先は /api/users/me の結果で決める。新しいセッションで実際に通ることもここで確かめる
    const meResult = await fetchCurrentUser()
    setSubmitting(false)
    if (!meResult.ok) {
      setError(errorMessageFor(meResult, FALLBACK_MESSAGE))
      return
    }
    // ログイン済みになると、画面の出し分けが role のトップへ移す
    signIn(meResult.data)
  }

  return (
    <main className="page page--narrow">
      <h1 className="app-title">就活管理アプリ</h1>
      {justRegistered && (
        <p className="notice notice--success" role="status">
          登録しました。ログインしてください
        </p>
      )}
      <form className="form" onSubmit={handleSubmit} noValidate>
        <TextField
          label="ログインID"
          type="text"
          name="login_id"
          autoComplete="username"
          autoCapitalize="none"
          spellCheck={false}
          value={loginId}
          onChange={(event) => setLoginId(event.target.value)}
        />
        <TextField
          label="パスワード"
          type="password"
          name="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        {error && (
          <p className="notice notice--error" role="alert">
            {error}
          </p>
        )}
        <button className="button button--primary button--block" type="submit" disabled={submitting}>
          {submitting ? 'ログイン中…' : 'ログイン'}
        </button>
      </form>
      <p className="page-link">
        <Link to="/signup">新規登録はこちら</Link>
      </p>
    </main>
  )
}
