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

export function isKnownStatus(status) {
  return Object.hasOwn(STATUS_LABELS, status)
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
    return '—'
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  return match ? `${match[1]}/${match[2]}/${match[3]}` : value
}
