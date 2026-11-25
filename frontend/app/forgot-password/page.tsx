'use client'

import Link from 'next/link'
import { FormEvent, useState } from 'react'
import { ArrowRight } from 'lucide-react'
import { AuthCard } from '../../components/AuthCard'
import { api, ApiError } from '../../lib/api'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const result = await api.post<{ message: string }>('/auth/forgot-password', { email })
      setSent(result.message)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not send the reset link. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard eyebrow="Account Recovery" title="Forgot your password?" subtitle="Enter your account email and we'll send you a link to choose a new one.">
      {sent ? (
        <div className="alert alert-success">{sent} The link expires in 30 minutes.</div>
      ) : (
        <>
          {error && <div className="alert alert-error">{error}</div>}
          <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <label>
              Email Address
              <input required type="email" maxLength={254} placeholder="you@example.com" value={email} onChange={e => setEmail(e.target.value)} />
            </label>
            <button className="btn btn-primary" type="submit" disabled={submitting} style={{ padding: '14px 24px' }}>
              {submitting ? 'Sending…' : 'Email me a reset link'} <ArrowRight size={16} />
            </button>
          </form>
        </>
      )}
      <p style={{ textAlign: 'center', marginTop: 24, fontSize: 14, color: 'var(--text-secondary)' }}>
        Remembered it? <Link href="/login" style={{ color: 'var(--accent-primary)', fontWeight: 700 }}>Back to sign in</Link>
      </p>
    </AuthCard>
  )
}
