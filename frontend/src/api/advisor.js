import { toApplicationDetail } from './applications.js'
import { apiRequest, failure, STATUS_INVALID_RESPONSE } from './client.js'

function toSeekerSummary(item) {
  if (item === null || typeof item !== 'object') {
    return null
  }
  const {
    user_id: userId,
    name,
    application_count: applicationCount,
    last_updated_at: lastUpdatedAt,
  } = item
  if (
    !Number.isSafeInteger(userId) ||
    typeof name !== 'string' ||
    !Number.isSafeInteger(applicationCount) ||
    applicationCount < 0 ||
    !(lastUpdatedAt === null || lastUpdatedAt === undefined || typeof lastUpdatedAt === 'string')
  ) {
    return null
  }
  return { userId, name, applicationCount, lastUpdatedAt: lastUpdatedAt ?? null }
}

// 1件でも形が違えば全体を失敗にする（欠けた一覧を正しいものとして見せない）
export async function listSeekers() {
  const result = await apiRequest('GET', '/api/admin/seekers')
  if (!result.ok) {
    return result
  }
  if (!Array.isArray(result.data)) {
    return failure(STATUS_INVALID_RESPONSE)
  }
  const seekers = result.data.map(toSeekerSummary)
  if (seekers.includes(null)) {
    return failure(STATUS_INVALID_RESPONSE)
  }
  return { ...result, data: seekers }
}

export async function getSeekerApplications(userId) {
  if (!Number.isSafeInteger(userId)) {
    throw new Error('userId must be an integer')
  }
  const result = await apiRequest('GET', `/api/admin/seekers/${userId}/applications`)
  if (!result.ok) {
    return result
  }
  const data = result.data
  if (
    data === null ||
    typeof data !== 'object' ||
    !Number.isSafeInteger(data.user_id) ||
    typeof data.name !== 'string' ||
    !Array.isArray(data.applications)
  ) {
    return failure(STATUS_INVALID_RESPONSE)
  }
  const applications = data.applications.map(toApplicationDetail)
  if (applications.includes(null)) {
    return failure(STATUS_INVALID_RESPONSE)
  }
  return { ...result, data: { userId: data.user_id, name: data.name, applications } }
}
