import { createContext, useContext } from 'react'

export const AuthContext = createContext(null)

// user は { userId, name, role }、未ログインなら null
export function useAuth() {
  const value = useContext(AuthContext)
  if (value === null) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return value
}
