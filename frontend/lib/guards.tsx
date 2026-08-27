'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuth } from './auth-context'

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!loading && !user) router.replace('/login')
  }, [loading, user, router])

  if (loading || !user) return <main className="shell page-intro"><p className="hero-copy">Loading…</p></main>
  return <>{children}</>
}

export function RequireAdmin({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  const router = useRouter()

  useEffect(() => {
    if (!loading && (!user || user.role !== 'ADMIN')) router.replace('/login')
  }, [loading, user, router])

  if (loading || !user || user.role !== 'ADMIN') return <main className="shell page-intro"><p className="hero-copy">Loading…</p></main>
  return <>{children}</>
}
