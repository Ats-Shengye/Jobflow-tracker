import { isKnownEmploymentInsurance, isKnownStatus } from '../format.js'
import { apiRequest, failure, STATUS_INVALID_RESPONSE } from './client.js'

function isOptionalString(value) {
  return value === null || value === undefined || typeof value === 'string'
}

function isOptionalRound(value) {
  return value === null || value === undefined || Number.isSafeInteger(value)
}

// 応募の全項目（詳細・登録・更新・担当者の求職者別応募で共通）。形が違えば null
export function toApplicationDetail(item) {
  if (item === null || typeof item !== 'object') {
    return null
  }
  const {
    application_id: applicationId,
    company_name: companyName,
    position,
    employment_insurance: employmentInsurance,
    applied_date: appliedDate,
    status,
    interview_round: interviewRound,
    salary,
    work_location: workLocation,
    memo,
    updated_at: updatedAt,
  } = item
  if (
    !Number.isSafeInteger(applicationId) ||
    typeof companyName !== 'string' ||
    typeof position !== 'string' ||
    !isKnownEmploymentInsurance(employmentInsurance) ||
    typeof appliedDate !== 'string' ||
    !isKnownStatus(status) ||
    !isOptionalRound(interviewRound) ||
    !isOptionalString(salary) ||
    !isOptionalString(workLocation) ||
    !isOptionalString(memo) ||
    typeof updatedAt !== 'string'
  ) {
    return null
  }
  return {
    applicationId,
    companyName,
    position,
    employmentInsurance,
    appliedDate,
    status,
    interviewRound: interviewRound ?? null,
    salary: salary ?? null,
    workLocation: workLocation ?? null,
    memo: memo ?? null,
    updatedAt,
  }
}

function withApplicationDetail(result) {
  if (!result.ok) {
    return result
  }
  const application = toApplicationDetail(result.data)
  return application ? { ...result, data: application } : failure(STATUS_INVALID_RESPONSE)
}

function requireId(applicationId) {
  if (!Number.isSafeInteger(applicationId)) {
    throw new Error('applicationId must be an integer')
  }
}

function toApplicationSummary(item) {
  if (item === null || typeof item !== 'object') {
    return null
  }
  const {
    application_id: applicationId,
    company_name: companyName,
    position,
    status,
    interview_round: interviewRound,
    applied_date: appliedDate,
  } = item
  const roundIsValid = interviewRound === null || interviewRound === undefined || Number.isSafeInteger(interviewRound)
  if (
    !Number.isSafeInteger(applicationId) ||
    typeof companyName !== 'string' ||
    typeof position !== 'string' ||
    !isKnownStatus(status) ||
    !roundIsValid ||
    typeof appliedDate !== 'string'
  ) {
    return null
  }
  return { applicationId, companyName, position, status, interviewRound: interviewRound ?? null, appliedDate }
}

// 1件でも形が違えば一覧全体を失敗にする（欠けた一覧を正しいものとして見せない）
export async function listApplications() {
  const result = await apiRequest('GET', '/api/applications')
  if (!result.ok) {
    return result
  }
  if (!Array.isArray(result.data)) {
    return failure(STATUS_INVALID_RESPONSE)
  }
  const applications = result.data.map(toApplicationSummary)
  if (applications.includes(null)) {
    return failure(STATUS_INVALID_RESPONSE)
  }
  return { ...result, data: applications }
}

export async function getApplication(applicationId) {
  requireId(applicationId)
  return withApplicationDetail(await apiRequest('GET', `/api/applications/${applicationId}`))
}

// body は送る形（snake_case）に組み立て済みのもの。成功時は画面を移るだけで応答の中身は使わないので、
// 形の確認で失敗扱いにしない（保存済みなのに「できませんでした」と出て二重登録されるのを避ける）
export function createApplication(body) {
  return apiRequest('POST', '/api/applications', { body })
}

export function updateApplication(applicationId, body) {
  requireId(applicationId)
  return apiRequest('PUT', `/api/applications/${applicationId}`, { body })
}

export function deleteApplication(applicationId) {
  requireId(applicationId)
  return apiRequest('DELETE', `/api/applications/${applicationId}`)
}
