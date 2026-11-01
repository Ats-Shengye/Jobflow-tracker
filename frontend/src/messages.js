import { STATUS_NETWORK_ERROR } from './api/client.js'

// 複数の画面で使う文言

export const NETWORK_ERROR_MESSAGE = 'サーバーに接続できませんでした。時間をおいてもう一度お試しください'

// サーバーの文言があればそれを出し、無ければ画面ごとの文言にする
export function errorMessageFor(result, fallback) {
  if (result.status === STATUS_NETWORK_ERROR) {
    return NETWORK_ERROR_MESSAGE
  }
  return result.message || fallback
}
