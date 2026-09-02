'use client'

import Link from 'next/link'
import { ArrowRight, Box } from 'lucide-react'
import { FormEvent, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Header } from '../../components/Header'
import { useAuth } from '../../lib/auth-context'
import { ApiError } from '../../lib/api'

export default function RegisterPage() {
  const { register } = useAuth()
  const router = useRouter()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await register(name, email, password)
      router.push('/')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create your account. Please try again.')
    } finally {
      setSubmitting(false)
    }
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
            <div className="eyebrow" style={{ justifyContent: 'center' }}>Join PrintForge</div>
            <h1 style={{ fontSize: 28, margin: '4px 0 8px' }}>Create an account</h1>
            <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
              Join the studio to order catalog creations and request custom 3D fabrications.
            </p>
          </div>

          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <label>
              Full Name
              <input
                required
                placeholder="e.g. Maya Fernando"
                value={name}
                onChange={e => setName(e.target.value)}
              />
            </label>

            <label>
              Email Address
              <input
                required
                type="email"
                placeholder="maya@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
              />
            </label>

            <label>
              Password
              <input
                required
                minLength={8}
                type="password"
                placeholder="At least 8 characters"
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
              {submitting ? 'Creating account…' : 'Create Account'} <ArrowRight size={16} />
            </button>
          </form>

          <p style={{ textAlign: 'center', fontSize: 13, color: 'var(--text-secondary)', marginTop: 24 }}>
            Already registered?{' '}
            <Link href="/login" style={{ color: 'var(--accent-primary)', fontWeight: 700 }}>
              Sign in here
            </Link>
          </p>
        </div>
      </section>
    </main>
  )
}
