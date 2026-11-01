import { isKnownRole } from '../roles.js'
import { apiRequest, failure, STATUS_INVALID_RESPONSE } from './client.js'

// 想定外の形の応答はログイン扱いにしない（role が読めないまま画面を出さない）
function toUser(data) {
  if (data === null || typeof data !== 'object') {
    return null
  }
  const { user_id: userId, name, role } = data
  if (!Number.isSafeInteger(userId) || typeof name !== 'string' || !isKnownRole(role)) {
    return null
  }
  return { userId, name, role }
}

function withUser(result) {
  if (!result.ok) {
    return result
  }
  const user = toUser(result.data)
  return user ? { ...result, data: user } : failure(STATUS_INVALID_RESPONSE)
}

// 確認そのものなので、401 でもセッション切れの処理は呼ばない
export async function fetchCurrentUser() {
  return withUser(await apiRequest('GET', '/api/users/me', { notifyUnauthorized: false }))
}

// ログイン失敗の 401 は「正しくありません」の表示であって、セッション切れではない
export async function login(loginId, password) {
  return withUser(
    await apiRequest('POST', '/api/login', {
      body: { login_id: loginId, password },
      notifyUnauthorized: false,
    }),
  )
}

export function logout() {
  return apiRequest('POST', '/api/logout')
}

export function registerUser({ loginId, password, name }) {
  return apiRequest('POST', '/api/users', {
    body: { login_id: loginId, password, name },
    notifyUnauthorized: false,
  })
}
