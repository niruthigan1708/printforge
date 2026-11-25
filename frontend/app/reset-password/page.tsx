'use client'

import Link from 'next/link'
import { FormEvent, Suspense, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { ArrowRight } from 'lucide-react'
import { AuthCard } from '../../components/AuthCard'
import { api, ApiError } from '../../lib/api'

function ResetPasswordForm() {
  const token = useSearchParams().get('token') || ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    if (password !== confirm) {
      setError('The two passwords do not match.')
      return
    }
    setSubmitting(true)
    try {
      await api.post('/auth/reset-password', { token, password })
      setDone(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not reset the password. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  if (!token) {
    return (
      <div className="alert alert-error">
        This page needs the link from your reset email. <Link href="/forgot-password">Request a new link</Link>.
      </div>
    )
  }

  if (done) {
    return (
      <>
        <div className="alert alert-success">Your password has been changed and you have been signed out everywhere.</div>
        <Link className="btn btn-primary" href="/login" style={{ width: '100%', padding: '14px 24px' }}>
          Sign in with your new password <ArrowRight size={16} />
        </Link>
      </>
    )
  }

  return (
    <>
      {error && (
        <div className="alert alert-error">
          {error} {error.includes('expired') && <Link href="/forgot-password">Request a new link</Link>}
        </div>
      )}
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <label>
          New Password
          <input required type="password" minLength={8} maxLength={72} placeholder="At least 8 characters" value={password} onChange={e => setPassword(e.target.value)} />
        </label>
        <label>
          Confirm New Password
          <input required type="password" minLength={8} maxLength={72} value={confirm} onChange={e => setConfirm(e.target.value)} />
        </label>
        <button className="btn btn-primary" type="submit" disabled={submitting} style={{ padding: '14px 24px' }}>
          {submitting ? 'Saving…' : 'Set new password'} <ArrowRight size={16} />
        </button>
      </form>
    </>
  )
}

export default function ResetPasswordPage() {
  return (
    <AuthCard eyebrow="Account Recovery" title="Choose a new password" subtitle="Reset links work once and expire after 30 minutes.">
      <Suspense fallback={null}>
        <ResetPasswordForm />
      </Suspense>
    </AuthCard>
  )
}
