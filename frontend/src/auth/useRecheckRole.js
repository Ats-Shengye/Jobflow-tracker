import { useCallback } from 'react'
import { fetchCurrentUser } from '../api/auth.js'
import { useAuth } from './AuthContext.js'

// API が 403 を返した時に呼ぶ。別のタブで入り直した等で role が変わっていれば状態を入れ替え、画面の出し分けに role のトップへ移させる。
// 画面側の role のまま別のトップへ移すと、ガードに戻されて行き来し続けるので、必ずサーバーの値で決める
export function useRecheckRole() {
  const { user, signIn, signOut } = useAuth()
  const currentRole = user?.role ?? null

  return useCallback(async () => {
    const result = await fetchCurrentUser()
    if (result.ok) {
      if (result.data.role !== currentRole) {
        signIn(result.data)
      }
      return
    }
    if (result.status === 401) {
      signOut()
    }
  }, [currentRole, signIn, signOut])
}
