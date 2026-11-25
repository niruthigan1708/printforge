'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useAuth } from './auth-context'

/** Sends signed-out visitors to login, remembering where they were going. */
function useLoginRedirect(shouldRedirect: boolean) {
  const router = useRouter()
  const pathname = usePathname()
  useEffect(() => {
    if (shouldRedirect) router.replace(`/login?redirect=${encodeURIComponent(pathname)}`)
  }, [shouldRedirect, router, pathname])
}

const Loading = () => <main className="shell page-intro"><p className="hero-copy">Loading…</p></main>

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  useLoginRedirect(!loading && !user)

  if (loading || !user) return <Loading />
  return <>{children}</>
}

export function RequireAdmin({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  useLoginRedirect(!loading && !user)

  if (loading || !user) return <Loading />
  // Signed in, but not an admin: say so instead of bouncing them to a login page they're past.
  if (user.role !== 'ADMIN') {
    return (
      <main className="shell page-intro">
        <p className="hero-copy">You don&apos;t have access to the admin studio.</p>
        <Link href="/" className="link-btn">Back to the shop</Link>
      </main>
    )
  }
  return <>{children}</>
}
