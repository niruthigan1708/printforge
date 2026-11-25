'use client'

import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api, ApiError, isTokenExpired, SESSION_EXPIRED_EVENT, TOKEN_KEY } from './api'
import type { AuthResponse, Role } from './types'

const USER_KEY = 'printforge-user'

export type AuthUser = { id: number; name: string; email: string; role: Role }

type AuthContextValue = {
  user: AuthUser | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  loading: true,
  login: async () => {},
  register: async () => {},
  logout: () => {},
})

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)

  const router = useRouter()

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }, [])

  useEffect(() => {
    try {
      const savedToken = localStorage.getItem(TOKEN_KEY)
      const savedUser = localStorage.getItem(USER_KEY)
      // Don't restore a session whose token has already expired: every request would fail.
      // Restoring client-only state after hydration: reading localStorage during render would
      // make the first client render differ from the server HTML.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (savedToken && savedUser && !isTokenExpired(savedToken)) setUser(JSON.parse(savedUser))
      else if (savedToken || savedUser) clearSession()
    } catch {
      // ignore corrupted local storage
    }
    setLoading(false)
  }, [clearSession])

  // The API client fires this when the server rejects our token mid-session.
  useEffect(() => {
    function onExpired() {
      clearSession()
      const here = window.location.pathname + window.location.search
      if (!window.location.pathname.startsWith('/login')) router.replace(`/login?expired=1&redirect=${encodeURIComponent(here)}`)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired)
  }, [clearSession, router])

  const persist = useCallback((auth: AuthResponse) => {
    const nextUser: AuthUser = { id: auth.id, name: auth.name, email: auth.email, role: auth.role }
    localStorage.setItem(TOKEN_KEY, auth.token)
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
    setUser(nextUser)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const auth = await api.post<AuthResponse>('/auth/login', { email, password })
    persist(auth)
  }, [persist])

  const register = useCallback(async (name: string, email: string, password: string) => {
    const auth = await api.post<AuthResponse>('/auth/register', { name, email, password })
    persist(auth)
  }, [persist])

  const logout = clearSession

  return <AuthContext.Provider value={{ user, loading, login, register, logout }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
export { ApiError }
