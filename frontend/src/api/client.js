// fetch を薄く包み、結果を { ok, status, data } / { ok, status, message, errors } の形で返す。
// 呼び出し側が try/catch を書かずに済むよう、通信の失敗も例外にせず、HTTP の状態コードの代わりに下の値で返す。

export const STATUS_NETWORK_ERROR = 0
export const STATUS_INVALID_RESPONSE = -1

const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'

// サーバーが応答しない時に画面が待ち続けないための上限。ローカルの API なので通常は数百ミリ秒で返る
const REQUEST_TIMEOUT_MS = 15000

let unauthorizedHandler = null

// セッション切れ（401）の時に呼ぶ処理を登録する。戻り値は登録を外す関数
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
  return () => {
    if (unauthorizedHandler === handler) {
      unauthorizedHandler = null
    }
  }
}

// ログイン・ログアウトの応答でトークンが入れ替わるので、覚えておかずに送る直前に読む
function readCookie(name) {
  for (const part of document.cookie.split(';')) {
    const separator = part.indexOf('=')
    if (separator === -1 || part.slice(0, separator).trim() !== name) {
      continue
    }
    try {
      return decodeURIComponent(part.slice(separator + 1).trim())
    } catch {
      return null
    }
  }
  return null
}

async function readJson(response) {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (!contentType.includes('application/json')) {
    return null
  }
  try {
    return await response.json()
  } catch {
    return null
  }
}

// 項目ごとのエラーは文字列の値だけ残す。fromEntries は自分のプロパティとして作るので、__proto__ というキーでも安全
function toFieldErrors(raw) {
  if (raw === null || typeof raw !== 'object' || Array.isArray(raw)) {
    return {}
  }
  return Object.fromEntries(Object.entries(raw).filter(([, value]) => typeof value === 'string'))
}

export function failure(status, message = '', errors = {}) {
  return { ok: false, status, message, errors }
}

export async function apiRequest(method, path, { body, notifyUnauthorized = true } = {}) {
  // 他のオリジンへ Cookie や CSRF トークンを送らないよう、呼べるのは同じオリジンの /api/ だけにする
  if (!path.startsWith('/api/')) {
    throw new Error(`API path must start with /api/: ${path}`)
  }

  const headers = { Accept: 'application/json' }
  const init = {
    method,
    headers,
    credentials: 'same-origin',
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(body)
  }
  if (method !== 'GET') {
    const token = readCookie(CSRF_COOKIE_NAME)
    if (token) {
      headers[CSRF_HEADER_NAME] = token
    }
  }

  let response
  try {
    response = await fetch(path, init)
  } catch {
    return failure(STATUS_NETWORK_ERROR)
  }

  const payload = await readJson(response)
  if (response.ok) {
    return { ok: true, status: response.status, data: payload }
  }
  if (response.status === 401 && notifyUnauthorized && unauthorizedHandler) {
    unauthorizedHandler()
  }
  const message = typeof payload?.message === 'string' ? payload.message : ''
  return failure(response.status, message, toFieldErrors(payload?.errors))
}
