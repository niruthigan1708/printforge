'use client'
import Link from 'next/link'
import { ArrowRight } from 'lucide-react'
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
      <section className="shell auth">
        <div className="eyebrow">Join the workshop</div>
        <h1 className="page-title">Make room<br />for <span style={{ color: 'var(--orange)' }}>better.</span></h1>
        <form onSubmit={submit}>
          {error && <div className="alert alert-error">{error}</div>}
          <label>Name<input required placeholder="Your name" value={name} onChange={e => setName(e.target.value)} /></label>
          <label>Email<input required type="email" placeholder="you@example.com" value={email} onChange={e => setEmail(e.target.value)} /></label>
          <label>Password<input required minLength={8} type="password" placeholder="At least 8 characters" value={password} onChange={e => setPassword(e.target.value)} /></label>
          <button className="btn orange" type="submit" disabled={submitting}>{submitting ? 'Creating account…' : 'Create account'} <ArrowRight size={15} /></button>
        </form>
        <p className="fine-print">Already have an account? <Link href="/login">Log in</Link></p>
      </section>
    </main>
  )
}
