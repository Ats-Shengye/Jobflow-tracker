// 応募フォームの値は入力欄のまま文字列で持ち、キーはサーバーの errors と同じ snake_case にする。
// 数値への変換は送る直前（toRequestBody）にだけ行う

export const FIELD_KEYS = [
  'company_name',
  'position',
  'employment_insurance',
  'applied_date',
  'status',
  'interview_round',
  'salary',
  'work_location',
  'memo',
]

export const INTERVIEW_ROUND_INVALID_MESSAGE = '面接回数は1〜5で選択してください'

const REQUIRED_MESSAGES = {
  company_name: '企業名を入力してください',
  position: 'ポジションを入力してください',
  employment_insurance: '雇用保険の有無を入力してください',
  applied_date: '申込日を入力してください',
  status: '進捗ステータスを入力してください',
}

// 「今日」はブラウザの日付。toISOString は UTC なので、日本時間の 0〜9 時に前日になる
function todayLocalDate() {
  const now = new Date()
  const year = String(now.getFullYear()).padStart(4, '0')
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function newApplicationValues() {
  return {
    company_name: '',
    position: '',
    employment_insurance: 'UNKNOWN',
    applied_date: todayLocalDate(),
    status: 'APPLIED',
    interview_round: '',
    salary: '',
    work_location: '',
    memo: '',
  }
}

export function valuesFromApplication(application) {
  const isInterview = application.status === 'INTERVIEW' && application.interviewRound !== null
  return {
    company_name: application.companyName,
    position: application.position,
    employment_insurance: application.employmentInsurance,
    applied_date: application.appliedDate,
    status: application.status,
    interview_round: isInterview ? String(application.interviewRound) : '',
    salary: application.salary ?? '',
    work_location: application.workLocation ?? '',
    memo: application.memo ?? '',
  }
}

// 面接以外に変えたら回数を空に戻す。面接に戻した時も空のままにして選び直してもらう
export function withStatus(values, status) {
  return status === 'INTERVIEW' ? { ...values, status } : { ...values, status, interview_round: '' }
}

// 画面では未入力だけ先に見る。文字数はサーバーの errors に任せる（maxLength で入力も止めている）
export function findMissingFields(values) {
  const errors = {}
  for (const [key, message] of Object.entries(REQUIRED_MESSAGES)) {
    if (values[key].trim() === '') {
      errors[key] = message
    }
  }
  if (values.status === 'INTERVIEW' && values.interview_round === '') {
    errors.interview_round = INTERVIEW_ROUND_INVALID_MESSAGE
  }
  return errors
}

// サーバーの errors のうち、このフォームの項目のものだけ拾う
export function pickFieldErrors(errors) {
  const picked = {}
  for (const key of FIELD_KEYS) {
    if (typeof errors[key] === 'string') {
      picked[key] = errors[key]
    }
  }
  return picked
}

// 前後の空白の削り方と、任意項目の空を null にするのはサーバーに任せ、入力のまま送る
export function toRequestBody(values) {
  const round = Number(values.interview_round)
  return {
    company_name: values.company_name,
    position: values.position,
    employment_insurance: values.employment_insurance,
    applied_date: values.applied_date,
    status: values.status,
    interview_round: values.status === 'INTERVIEW' && Number.isSafeInteger(round) && round > 0 ? round : null,
    salary: values.salary,
    work_location: values.work_location,
    memo: values.memo,
  }
}
