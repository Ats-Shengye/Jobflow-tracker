import { useState } from 'react'
import { useNavigate } from 'react-router'
import { logout as logoutRequest } from '../api/auth.js'
import { useAuth } from './AuthContext.js'

const LOGOUT_FAILED_MESSAGE = 'ログアウトできませんでした。時間をおいてもう一度お試しください'

// サーバーのセッションが残ったまま「ログアウト済み」に見せないよう、失敗した時は画面を移さずに知らせる
export function useLogout() {
  const { signOut } = useAuth()
  const navigate = useNavigate()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState('')

  async function logout() {
    setIsLoggingOut(true)
    setLogoutError('')
    const result = await logoutRequest()
    setIsLoggingOut(false)
    if (!result.ok) {
      // 401 はセッションが既に無いので、共通の処理でログイン画面へ移る
      if (result.status !== 401) {
        setLogoutError(LOGOUT_FAILED_MESSAGE)
      }
      return
    }
    signOut()
    navigate('/login', { replace: true })
  }

  return { logout, isLoggingOut, logoutError }
}
