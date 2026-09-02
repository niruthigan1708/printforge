'use client'

import Link from 'next/link'
import { ArrowRight, Box, ShieldCheck, User } from 'lucide-react'
import { FormEvent, Suspense, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Header } from '../../components/Header'
import { useAuth } from '../../lib/auth-context'
import { ApiError } from '../../lib/api'

function LoginPageInner() {
  const { login } = useAuth()
  const router = useRouter()
  const searchParams = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(email, password)
      router.push(searchParams.get('redirect') || '/')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Invalid credentials. Please check and try again.')
    } finally {
      setSubmitting(false)
    }
  }

  function fillDemo(demoEmail: string, demoPass: string) {
    setEmail(demoEmail)
    setPassword(demoPass)
  }

  return (
    <main>
      <Header />

      <section className="shell" style={{ padding: '70px 0 100px', display: 'grid', placeItems: 'center' }}>
        <div
          className="form-card"
          style={{
            width: '100%',
            maxWidth: 480,
            padding: '40px 36px',
            boxShadow: 'var(--shadow-lg)',
          }}
        >
          <div style={{ textAlign: 'center', marginBottom: 28 }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 'var(--radius-md)',
                background: 'linear-gradient(135deg, #FF7733 0%, var(--accent-primary) 100%)',
                color: 'white',
                display: 'grid',
                placeItems: 'center',
                margin: '0 auto 16px',
                boxShadow: 'var(--shadow-orange)',
              }}
            >
              <Box size={26} />
            </div>
            <div className="eyebrow" style={{ justifyContent: 'center' }}>PrintForge Access</div>
            <h1 style={{ fontSize: 28, margin: '4px 0 8px' }}>Welcome back</h1>
            <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
              Sign in to manage your 3D orders and custom print quotes.
            </p>
          </div>

          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <label>
              Email Address
              <input
                required
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
              />
            </label>

            <label>
              Password
              <input
                required
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
              />
            </label>

            <button
              className="btn btn-primary"
              type="submit"
              disabled={submitting}
              style={{ padding: '14px 20px', marginTop: 8 }}
            >
              {submitting ? 'Authenticating…' : 'Sign In'} <ArrowRight size={16} />
            </button>
          </form>

          {/* Demo Account Quick Fill Helpers */}
          <div
            style={{
              marginTop: 28,
              paddingTop: 20,
              borderTop: '1px solid var(--border-subtle)',
              fontSize: 13,
            }}
          >
            <div style={{ fontSize: 11, fontFamily: 'var(--font-mono)', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: 10 }}>
              Quick Demo Login:
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <button
                type="button"
                className="btn btn-sm secondary"
                style={{ flex: 1, fontSize: 12 }}
                onClick={() => fillDemo('customer@printforge.com', 'Customer@12345')}
              >
                <User size={13} /> Customer
              </button>
              <button
                type="button"
                className="btn btn-sm secondary"
                style={{ flex: 1, fontSize: 12 }}
                onClick={() => fillDemo('admin@printforge.com', 'Admin@12345')}
              >
                <ShieldCheck size={13} /> Admin
              </button>
            </div>
          </div>

          <p style={{ textAlign: 'center', fontSize: 13, color: 'var(--text-secondary)', marginTop: 20 }}>
            Don't have an account?{' '}
            <Link href="/register" style={{ color: 'var(--accent-primary)', fontWeight: 700 }}>
              Sign up here
            </Link>
          </p>
        </div>
      </section>
    </main>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginPageInner />
    </Suspense>
  )
}
