// API は選択肢を英字のコードで返すので、表示用の日本語はここでまとめて変換する

export const STATUS_LABELS = {
  APPLIED: '応募済',
  DOCUMENT_SCREENING: '書類選考中',
  INTERVIEW: '面接',
  OFFERED: '内定',
  REJECTED: '不採用',
  DECLINED: '辞退',
}

export const EMPLOYMENT_INSURANCE_LABELS = {
  YES: '有',
  NO: '無',
  UNKNOWN: '不明',
}

const INTERVIEW_ROUND_LABELS = { 1: '一次', 2: '二次', 3: '三次', 4: '四次', 5: '五次' }

// 面接回数のセレクトの選択肢（値は送る時に数値へ変える）
export const INTERVIEW_ROUND_OPTIONS = Object.entries(INTERVIEW_ROUND_LABELS).map(([value, label]) => ({
  value,
  label: `${label}面接`,
}))

export const EMPTY_TEXT = '---'
export const NO_VALUE = '—'

export function isKnownStatus(status) {
  return Object.hasOwn(STATUS_LABELS, status)
}

export function isKnownEmploymentInsurance(value) {
  return Object.hasOwn(EMPLOYMENT_INSURANCE_LABELS, value)
}

export function employmentInsuranceLabel(value) {
  return isKnownEmploymentInsurance(value) ? EMPLOYMENT_INSURANCE_LABELS[value] : '不明'
}

// 表では「二次」、詳細では「二次面接」と出す。範囲外・未設定は null
export function interviewRoundLabel(interviewRound) {
  return Object.hasOwn(INTERVIEW_ROUND_LABELS, interviewRound) ? INTERVIEW_ROUND_LABELS[interviewRound] : null
}

// 給与・勤務地・メモの空（null・空白だけ）は --- にする
export function formatOptionalText(value) {
  return typeof value === 'string' && value.trim() !== '' ? value : EMPTY_TEXT
}

// 面接は「二次面接」のように回数を付ける。回数が範囲外・未設定なら「面接」だけ出す
export function statusLabel(status, interviewRound) {
  if (!isKnownStatus(status)) {
    return '不明'
  }
  if (status === 'INTERVIEW' && Object.hasOwn(INTERVIEW_ROUND_LABELS, interviewRound)) {
    return `${INTERVIEW_ROUND_LABELS[interviewRound]}面接`
  }
  return STATUS_LABELS[status]
}

// Date に通すとタイムゾーンで日がずれることがあるので、文字列のまま 2026-10-30 → 2026/10/30 にする
export function formatDate(value) {
  if (typeof value !== 'string') {
    return NO_VALUE
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  return match ? `${match[1]}/${match[2]}/${match[3]}` : value
}

// サーバーは +09:00 付きで返すので、Date に通さず書かれた時刻のまま 2026-10-30T18:30:00+09:00 → 2026/10/30 18:30 にする
const DATE_TIME_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/

export function formatDateTime(value) {
  if (typeof value !== 'string') {
    return NO_VALUE
  }
  const match = DATE_TIME_PATTERN.exec(value)
  return match ? `${match[1]}/${match[2]}/${match[3]} ${match[4]}:${match[5]}` : value
}

// 日時から日付だけを出す（担当者用一覧の最終更新日）
export function formatDatePart(value) {
  if (typeof value !== 'string') {
    return NO_VALUE
  }
  const match = DATE_TIME_PATTERN.exec(value)
  return match ? `${match[1]}/${match[2]}/${match[3]}` : value
}
