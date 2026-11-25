'use client'

import { FormEvent, useEffect, useState } from 'react'
import { KeyRound, UserRound } from 'lucide-react'
import { Header } from '../../components/Header'
import { RequireAuth } from '../../lib/guards'
import { useAuth } from '../../lib/auth-context'
import { api, ApiError } from '../../lib/api'
import type { AuthResponse } from '../../lib/types'

type Account = { id: number; name: string; email: string; role: string; memberSince: string | null }

function AccountSettings() {
  const { updateUser } = useAuth()
  const [account, setAccount] = useState<Account | null>(null)
  const [name, setName] = useState('')
  const [profileMessage, setProfileMessage] = useState<{ ok: boolean; text: string } | null>(null)
  const [savingProfile, setSavingProfile] = useState(false)

  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [passwordMessage, setPasswordMessage] = useState<{ ok: boolean; text: string } | null>(null)
  const [savingPassword, setSavingPassword] = useState(false)

  useEffect(() => {
    api.get<Account>('/account')
      .then(loaded => { setAccount(loaded); setName(loaded.name) })
      .catch(err => setProfileMessage({ ok: false, text: err instanceof ApiError ? err.message : 'Could not load your account.' }))
  }, [])

  async function saveProfile(event: FormEvent) {
    event.preventDefault()
    setProfileMessage(null)
    setSavingProfile(true)
    try {
      const updated = await api.put<AuthResponse>('/account', { name })
      updateUser(updated)
      setAccount(current => (current ? { ...current, name: updated.name } : current))
      setProfileMessage({ ok: true, text: 'Your name has been updated.' })
    } catch (err) {
      setProfileMessage({ ok: false, text: err instanceof ApiError ? err.message : 'Could not save your details.' })
    } finally {
      setSavingProfile(false)
    }
  }

  async function changePassword(event: FormEvent) {
    event.preventDefault()
    setPasswordMessage(null)
    if (next !== confirm) {
      setPasswordMessage({ ok: false, text: 'The two new passwords do not match.' })
      return
    }
    setSavingPassword(true)
    try {
      updateUser(await api.put<AuthResponse>('/account/password', { currentPassword: current, newPassword: next }))
      setCurrent(''); setNext(''); setConfirm('')
      setPasswordMessage({ ok: true, text: 'Password changed. You have been signed out on your other devices.' })
    } catch (err) {
      setPasswordMessage({ ok: false, text: err instanceof ApiError ? err.message : 'Could not change your password.' })
    } finally {
      setSavingPassword(false)
    }
  }

  const memberSince = account?.memberSince
    ? new Date(account.memberSince).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
    : null

  return (
    <main>
      <Header />
      <section className="shell page-intro compact" style={{ padding: '50px 0 24px' }}>
        <div className="eyebrow">Your Account</div>
        <h1 className="page-title">Account settings</h1>
      </section>

      <section className="shell" style={{ paddingBottom: 100, display: 'grid', gap: 28, maxWidth: 640 }}>
        <form className="form-card" onSubmit={saveProfile} style={{ display: 'flex', flexDirection: 'column', gap: 16, maxWidth: 'none' }}>
          <h3 style={{ fontSize: 18, display: 'flex', alignItems: 'center', gap: 8 }}><UserRound size={18} color="var(--accent-primary)" /> Profile</h3>
          {profileMessage && <div className={`alert ${profileMessage.ok ? 'alert-success' : 'alert-error'}`}>{profileMessage.text}</div>}
          <label>
            Name
            <input required maxLength={100} value={name} onChange={e => setName(e.target.value)} disabled={!account} />
          </label>
          <label>
            Email
            <input value={account?.email ?? ''} disabled readOnly />
          </label>
          {memberSince && <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>Member since {memberSince}</p>}
          <button className="btn btn-primary" type="submit" disabled={savingProfile || !account || name.trim() === account.name} style={{ width: 'max-content' }}>
            {savingProfile ? 'Saving…' : 'Save changes'}
          </button>
        </form>

        <form className="form-card" onSubmit={changePassword} style={{ display: 'flex', flexDirection: 'column', gap: 16, maxWidth: 'none' }}>
          <h3 style={{ fontSize: 18, display: 'flex', alignItems: 'center', gap: 8 }}><KeyRound size={18} color="var(--accent-primary)" /> Change password</h3>
          {passwordMessage && <div className={`alert ${passwordMessage.ok ? 'alert-success' : 'alert-error'}`}>{passwordMessage.text}</div>}
          <label>
            Current password
            <input required type="password" maxLength={200} autoComplete="current-password" value={current} onChange={e => setCurrent(e.target.value)} />
          </label>
          <label>
            New password
            <input required type="password" minLength={8} maxLength={72} autoComplete="new-password" placeholder="At least 8 characters" value={next} onChange={e => setNext(e.target.value)} />
          </label>
          <label>
            Confirm new password
            <input required type="password" minLength={8} maxLength={72} autoComplete="new-password" value={confirm} onChange={e => setConfirm(e.target.value)} />
          </label>
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>Changing your password signs you out on every other device.</p>
          <button className="btn btn-primary" type="submit" disabled={savingPassword} style={{ width: 'max-content' }}>
            {savingPassword ? 'Changing…' : 'Change password'}
          </button>
        </form>
      </section>
    </main>
  )
}

export default function AccountPage() {
  return (
    <RequireAuth>
      <AccountSettings />
    </RequireAuth>
  )
}
