'use client'
import Link from 'next/link'
import { ArrowRight } from 'lucide-react'
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
      setError(err instanceof ApiError ? err.message : 'Could not log in. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main>
      <Header />
      <section className="shell auth">
        <div className="eyebrow">Welcome back</div>
        <h1 className="page-title">Good to<br /><span style={{ color: 'var(--orange)' }}>see you.</span></h1>
        <form onSubmit={submit}>
          {error && <div className="alert alert-error">{error}</div>}
          <label>Email<input required type="email" placeholder="you@example.com" value={email} onChange={e => setEmail(e.target.value)} /></label>
          <label>Password<input required type="password" placeholder="Your password" value={password} onChange={e => setPassword(e.target.value)} /></label>
          <button className="btn orange" type="submit" disabled={submitting}>{submitting ? 'Logging in…' : 'Log in'} <ArrowRight size={15} /></button>
        </form>
        <p className="fine-print">New to PrintForge? <Link href="/register">Create an account</Link></p>
        <p className="fine-print">Demo accounts: admin@printforge.com / customer@printforge.com</p>
      </section>
    </main>
  )
}

export default function LoginPage() {
  return <Suspense fallback={null}><LoginPageInner /></Suspense>
}
