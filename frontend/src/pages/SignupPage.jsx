import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { registerUser } from '../api/auth.js'
import TextField from '../components/TextField.jsx'
import { errorMessageFor } from '../messages.js'

// 文字数・文字種はサーバーで確かめ、その errors を各項目の下に出す。画面では未入力だけ先に見る
const FIELDS = [
  {
    key: 'login_id',
    label: 'ログインID',
    hint: '半角英数字と記号（空白なし）4〜64文字',
    required: 'ログインIDを入力してください',
    inputProps: { type: 'text', autoComplete: 'username', autoCapitalize: 'none', spellCheck: false },
  },
  {
    key: 'password',
    label: 'パスワード',
    hint: '半角英数字・記号・半角スペース 15〜64文字',
    required: 'パスワードを入力してください',
    inputProps: { type: 'password', autoComplete: 'new-password' },
  },
  {
    key: 'name',
    label: '氏名',
    required: '氏名を入力してください',
    inputProps: { type: 'text', autoComplete: 'name' },
  },
]

const INVALID_INPUT_MESSAGE = '入力内容を確認してください'
const DUPLICATE_LOGIN_ID_MESSAGE = 'このログインIDは使用できません'
const FALLBACK_MESSAGE = '登録できませんでした。時間をおいてもう一度お試しください'

const EMPTY_VALUES = { login_id: '', password: '', name: '' }

function findMissingFields(values) {
  const errors = {}
  for (const field of FIELDS) {
    if (values[field.key].trim() === '') {
      errors[field.key] = field.required
    }
  }
  return errors
}

// サーバーの errors のうち、この画面の項目のものだけ拾う
function pickFieldErrors(errors) {
  const picked = {}
  for (const field of FIELDS) {
    if (typeof errors[field.key] === 'string') {
      picked[field.key] = errors[field.key]
    }
  }
  return picked
}

export default function SignupPage() {
  const navigate = useNavigate()
  const [values, setValues] = useState(EMPTY_VALUES)
  const [fieldErrors, setFieldErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function handleChange(key, value) {
    setValues((current) => ({ ...current, [key]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (submitting) {
      return
    }
    const missing = findMissingFields(values)
    if (Object.keys(missing).length > 0) {
      setFieldErrors(missing)
      setFormError('')
      return
    }
    setSubmitting(true)
    setFieldErrors({})
    setFormError('')

    const result = await registerUser({ loginId: values.login_id, password: values.password, name: values.name })
    setSubmitting(false)
    if (result.ok) {
      navigate('/login', { state: { registered: true } })
      return
    }
    if (result.status === 400) {
      const picked = pickFieldErrors(result.errors)
      setFieldErrors(picked)
      setFormError(result.message || INVALID_INPUT_MESSAGE)
      return
    }
    if (result.status === 409) {
      setFieldErrors({ login_id: DUPLICATE_LOGIN_ID_MESSAGE })
      return
    }
    setFormError(errorMessageFor(result, FALLBACK_MESSAGE))
  }

  return (
    <main className="page page--narrow">
      <h1 className="page-title">ユーザー登録</h1>
      <form className="form" onSubmit={handleSubmit} noValidate>
        {FIELDS.map((field) => (
          <TextField
            key={field.key}
            label={field.label}
            hint={field.hint}
            error={fieldErrors[field.key]}
            name={field.key}
            value={values[field.key]}
            onChange={(event) => handleChange(field.key, event.target.value)}
            {...field.inputProps}
          />
        ))}
        {formError && (
          <p className="notice notice--error" role="alert">
            {formError}
          </p>
        )}
        <button className="button button--primary button--block" type="submit" disabled={submitting}>
          {submitting ? '登録中…' : '登録する'}
        </button>
      </form>
      <p className="page-link">
        <Link to="/login">ログイン画面に戻る</Link>
      </p>
    </main>
  )
}
