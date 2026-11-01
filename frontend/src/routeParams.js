// URL の :id は API のパスに埋め込むので、正の整数の数字だけを通す（../ や ? を含む値を送らない）。
// 数字でない ID はサーバーも 404 を返すので、呼ばずに「見つかりません」と同じ扱いにする
const ID_PATTERN = /^[1-9][0-9]{0,15}$/

export function parseIdParam(raw) {
  if (typeof raw !== 'string' || !ID_PATTERN.test(raw)) {
    return null
  }
  const id = Number(raw)
  return Number.isSafeInteger(id) ? id : null
}
