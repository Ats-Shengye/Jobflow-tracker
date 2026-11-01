import { useCallback, useEffect, useState } from 'react'
import { useRecheckRole } from '../auth/useRecheckRole.js'

const LOADING = { phase: 'loading', data: null }
const NOT_FOUND = { phase: 'notFound', data: null }

// 画面を開いた時の取得。key を load に渡し、phase を loading / ready / notFound / error で返す。
// key が null（URL の id が数字でない等）は呼ばずに notFound。load は再描画で変わらない関数を渡す
export function useLoad(key, load) {
  const recheckRole = useRecheckRole()
  const [state, setState] = useState({ key: null, ...LOADING })
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (key === null) {
      return undefined
    }
    let active = true
    load(key).then((result) => {
      // 401 は共通の処理がログイン画面へ移すので、失敗の表示を一瞬出さない
      if (!active || result.status === 401) {
        return
      }
      if (result.ok) {
        setState({ key, phase: 'ready', data: result.data })
        return
      }
      setState({ key, phase: result.status === 404 ? 'notFound' : 'error', data: null })
      if (result.status === 403) {
        recheckRole()
      }
    })
    return () => {
      active = false
    }
  }, [key, load, reloadKey, recheckRole])

  const retry = useCallback(() => {
    setState({ key: null, ...LOADING })
    setReloadKey((current) => current + 1)
  }, [])

  // 更新の 404（別の画面で消された等）を、開いた時の 404 と同じ表示にする
  const markNotFound = useCallback(() => setState({ key, ...NOT_FOUND }), [key])

  let view = LOADING
  if (key === null) {
    view = NOT_FOUND
  } else if (state.key === key) {
    view = state
  }
  return { phase: view.phase, data: view.data, retry, markNotFound }
}
