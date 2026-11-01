import { useState } from 'react'
import { EMPLOYMENT_INSURANCE_LABELS, INTERVIEW_ROUND_OPTIONS, STATUS_LABELS } from '../format.js'
import { errorMessageFor } from '../messages.js'
import { findMissingFields, pickFieldErrors, toRequestBody, withStatus } from './applicationFormValues.js'
import { RadioGroup, SelectField } from './ChoiceFields.jsx'
import TextField from './TextField.jsx'
import './ApplicationForm.css'

const INVALID_INPUT_MESSAGE = '入力内容を確認してください'

// 文字数の上限はサーバーの入力チェックと同じ値。超えた分は入力できないようにする
const TITLE_MAX_LENGTH = 100
const FREE_TEXT_MAX_LENGTH = 100
const MEMO_MAX_LENGTH = 1000

const STATUS_OPTIONS = Object.entries(STATUS_LABELS).map(([value, label]) => ({ value, label }))
const EMPLOYMENT_INSURANCE_OPTIONS = Object.entries(EMPLOYMENT_INSURANCE_LABELS).map(([value, label]) => ({
  value,
  label,
}))

function RequiredLabel({ children }) {
  return (
    <>
      {children}
      <span className="field__required" aria-hidden="true">
        {' '}
        *
      </span>
    </>
  )
}

// 応募登録・応募編集の共通フォーム。送信は submit(body) に任せ、成功・404 の後の画面の移り方は呼び出し側が決める
export default function ApplicationForm({
  initialValues,
  submitLabel,
  submittingLabel,
  failedMessage,
  submit,
  onSucceeded,
  onNotFound,
}) {
  const [values, setValues] = useState(initialValues)
  const [fieldErrors, setFieldErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function handleChange(key, value) {
    setValues((current) => ({ ...current, [key]: value }))
  }

  function handleStatusChange(status) {
    setValues((current) => withStatus(current, status))
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

    const result = await submit(toRequestBody(values))
    setSubmitting(false)
    if (result.ok) {
      onSucceeded()
      return
    }
    // 401 は共通の処理がログイン画面へ移すので、ここでは何も出さない
    if (result.status === 401) {
      return
    }
    if (result.status === 400) {
      setFieldErrors(pickFieldErrors(result.errors))
      setFormError(result.message || INVALID_INPUT_MESSAGE)
      return
    }
    if (result.status === 404 && onNotFound) {
      onNotFound()
      return
    }
    setFormError(errorMessageFor(result, failedMessage))
  }

  const isInterview = values.status === 'INTERVIEW'

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <TextField
        label={<RequiredLabel>企業名</RequiredLabel>}
        error={fieldErrors.company_name}
        name="company_name"
        type="text"
        placeholder="例）株式会社〇〇"
        maxLength={TITLE_MAX_LENGTH}
        aria-required="true"
        value={values.company_name}
        onChange={(event) => handleChange('company_name', event.target.value)}
      />
      <TextField
        label={<RequiredLabel>ポジション</RequiredLabel>}
        error={fieldErrors.position}
        name="position"
        type="text"
        placeholder="例）Webエンジニア"
        maxLength={TITLE_MAX_LENGTH}
        aria-required="true"
        value={values.position}
        onChange={(event) => handleChange('position', event.target.value)}
      />
      <RadioGroup
        legend={<RequiredLabel>雇用保険の有無</RequiredLabel>}
        error={fieldErrors.employment_insurance}
        name="employment_insurance"
        options={EMPLOYMENT_INSURANCE_OPTIONS}
        value={values.employment_insurance}
        onChange={(value) => handleChange('employment_insurance', value)}
        required
      />
      <TextField
        label={<RequiredLabel>申込日</RequiredLabel>}
        error={fieldErrors.applied_date}
        name="applied_date"
        type="date"
        aria-required="true"
        value={values.applied_date}
        onChange={(event) => handleChange('applied_date', event.target.value)}
      />
      <SelectField
        label={<RequiredLabel>進捗ステータス</RequiredLabel>}
        error={fieldErrors.status}
        name="status"
        options={STATUS_OPTIONS}
        aria-required="true"
        value={values.status}
        onChange={(event) => handleStatusChange(event.target.value)}
      />
      {isInterview && (
        <SelectField
          label={<RequiredLabel>面接回数</RequiredLabel>}
          error={fieldErrors.interview_round}
          name="interview_round"
          options={INTERVIEW_ROUND_OPTIONS}
          placeholder="選択してください"
          aria-required="true"
          value={values.interview_round}
          onChange={(event) => handleChange('interview_round', event.target.value)}
        />
      )}
      <TextField
        label="給与"
        error={fieldErrors.salary}
        name="salary"
        type="text"
        placeholder="例）年収400〜500万円"
        maxLength={FREE_TEXT_MAX_LENGTH}
        value={values.salary}
        onChange={(event) => handleChange('salary', event.target.value)}
      />
      <TextField
        label="勤務地"
        error={fieldErrors.work_location}
        name="work_location"
        type="text"
        placeholder="例）福岡市"
        maxLength={FREE_TEXT_MAX_LENGTH}
        value={values.work_location}
        onChange={(event) => handleChange('work_location', event.target.value)}
      />
      <TextField
        label="メモ"
        hint="担当者も閲覧できます"
        error={fieldErrors.memo}
        name="memo"
        multiline
        rows={5}
        maxLength={MEMO_MAX_LENGTH}
        value={values.memo}
        onChange={(event) => handleChange('memo', event.target.value)}
      />
      {formError && (
        <p className="notice notice--error" role="alert">
          {formError}
        </p>
      )}
      <button className="button button--primary button--block" type="submit" disabled={submitting}>
        {submitting ? submittingLabel : submitLabel}
      </button>
    </form>
  )
}
