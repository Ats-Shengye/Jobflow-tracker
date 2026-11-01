import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchCurrentUser } from '../api/auth.js'
import { setUnauthorizedHandler } from '../api/client.js'
import { AuthContext } from './AuthContext.js'

// ログイン状態はメモリにだけ持つ。localStorage などに置くと XSS で読まれるので、再読み込みの時は /api/users/me で確かめ直す
export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [checked, setChecked] = useState(false)

  useEffect(() => {
    let active = true
    fetchCurrentUser().then((result) => {
      if (!active) {
        return
      }
      // 401 以外の失敗（通信できない・想定外の応答）も未ログイン扱いにする。ログインの画面で改めて失敗が分かる
      setUser(result.ok ? result.data : null)
      setChecked(true)
    })
    return () => {
      active = false
    }
  }, [])

  // どの画面の API でも 401 が返ったら状態を消す。画面の出し分けがログイン画面へ移す
  useEffect(() => setUnauthorizedHandler(() => setUser(null)), [])

  const signIn = useCallback((nextUser) => setUser(nextUser), [])
  const signOut = useCallback(() => setUser(null), [])
  const value = useMemo(() => ({ user, signIn, signOut }), [user, signIn, signOut])

  // 確認が終わるまでは何も出さない（未ログインの画面が一瞬見えるのを防ぐ）
  if (!checked) {
    return null
  }
  return <AuthContext value={value}>{children}</AuthContext>
}
