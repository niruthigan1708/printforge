'use client'

import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api, ApiError, LEGACY_TOKEN_KEY, resetCsrfToken, SESSION_EXPIRED_EVENT, SESSION_USER_KEY } from './api'
import type { AuthResponse, Role } from './types'

export type AuthUser = { id: number; name: string; email: string; role: Role }

type AuthContextValue = {
  user: AuthUser | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  /** Replaces the cached user details (e.g. after the account page renames the user). */
  updateUser: (user: AuthResponse) => void
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  loading: true,
  login: async () => {},
  register: async () => {},
  logout: async () => {},
  updateUser: () => {},
})

const toUser = (auth: AuthResponse): AuthUser => ({ id: auth.id, name: auth.name, email: auth.email, role: auth.role })

/**
 * The session itself is an httpOnly cookie the browser sends automatically; page scripts never
 * see the token. This context only remembers who is signed in (for the UI) and asks the API to
 * confirm it on load.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)
  const router = useRouter()

  const clearSession = useCallback(() => {
    localStorage.removeItem(SESSION_USER_KEY)
    resetCsrfToken()
    setUser(null)
  }, [])

  const remember = useCallback((auth: AuthResponse) => {
    const next = toUser(auth)
    localStorage.setItem(SESSION_USER_KEY, JSON.stringify(next))
    setUser(next)
  }, [])

  useEffect(() => {
    // Tokens used to live in localStorage; never leave one behind.
    localStorage.removeItem(LEGACY_TOKEN_KEY)
    let saved: string | null = null
    try { saved = localStorage.getItem(SESSION_USER_KEY) } catch { /* storage unavailable */ }
    if (!saved) {
      // Restoring client-only state after hydration (reading storage during render would make
      // the first client render differ from the server HTML).
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setLoading(false)
      return
    }
    // Confirm the cookie is still valid and refresh the cached details.
    api.get<AuthResponse>('/auth/me')
      .then(remember)
      .catch(err => {
        if (err instanceof ApiError && err.status === 401) clearSession()
        else {
          // Server unreachable: keep showing the remembered user; requests will tell us if the session is gone.
          try { setUser(JSON.parse(saved as string)) } catch { clearSession() }
        }
      })
      .finally(() => setLoading(false))
  }, [clearSession, remember])

  // The API client fires this when the server rejects our session mid-visit.
  useEffect(() => {
    function onExpired() {
      clearSession()
      const here = window.location.pathname + window.location.search
      if (!window.location.pathname.startsWith('/login')) router.replace(`/login?expired=1&redirect=${encodeURIComponent(here)}`)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired)
  }, [clearSession, router])

  const login = useCallback(async (email: string, password: string) => {
    resetCsrfToken()
    remember(await api.post<AuthResponse>('/auth/login', { email, password }))
  }, [remember])

  const register = useCallback(async (name: string, email: string, password: string) => {
    resetCsrfToken()
    remember(await api.post<AuthResponse>('/auth/register', { name, email, password }))
  }, [remember])

  const logout = useCallback(async () => {
    // Revokes every session of this user server-side and clears the cookie. Awaited, so a quick
    // re-login cannot be revoked by a logout that was still in flight.
    try { await api.post('/auth/logout') } catch { /* already signed out, or offline */ }
    clearSession()
  }, [clearSession])

  return <AuthContext.Provider value={{ user, loading, login, register, logout, updateUser: remember }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
export { ApiError }
