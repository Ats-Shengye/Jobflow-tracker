import { isKnownStatus } from '../format.js'
import { apiRequest, failure, STATUS_INVALID_RESPONSE } from './client.js'

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

export function deleteApplication(applicationId) {
  if (!Number.isSafeInteger(applicationId)) {
    throw new Error('applicationId must be an integer')
  }
  return apiRequest('DELETE', `/api/applications/${applicationId}`)
}
