'use client'

import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, FileCode, CheckCircle2, XCircle, Clock, Sparkles } from 'lucide-react'
import { Header } from '../../../components/Header'
import { StatusBadge } from '../../../components/StatusBadge'
import { RequireAuth } from '../../../lib/guards'
import { api, ApiError } from '../../../lib/api'
import { money, type CustomPrintRequest } from '../../../lib/types'

function CustomPrintDetail() {
  const params = useParams<{ id: string }>()
  const [request, setRequest] = useState<CustomPrintRequest | null>(null)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)

  function load() {
    api.get<CustomPrintRequest>(`/custom-prints/${params.id}`)
      .then(setRequest)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Request not found.'))
  }

  useEffect(load, [params.id])

  async function respond(action: 'accept' | 'reject') {
    setBusy(true)
    setActionError('')
    try {
      const updated = await api.put<CustomPrintRequest>(`/custom-prints/${params.id}/${action}`)
      setRequest(updated)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Could not update this request.')
    } finally {
      setBusy(false)
    }
  }

  if (error) {
    return (
      <main>
        <Header />
        <section className="shell" style={{ padding: '80px 0' }}>
          <div className="empty">
            <div className="eyebrow" style={{ justifyContent: 'center' }}>Error</div>
            <h2>{error}</h2>
            <Link className="btn btn-primary" style={{ marginTop: 20 }} href="/orders">
              <ArrowLeft size={16} /> Back to Dashboard
            </Link>
          </div>
        </section>
      </main>
    )
  }

  if (!request) {
    return (
      <main>
        <Header />
        <div className="shell loading-state" style={{ margin: '80px auto' }}>
          <div className="spinner" />
          <p>Loading custom request details…</p>
        </div>
      </main>
    )
  }

  return (
    <main>
      <Header />

      <section className="shell page-intro compact" style={{ padding: '50px 0 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
          <Link href="/orders" className="link-btn">
            <ArrowLeft size={14} /> Back to Dashboard
          </Link>
        </div>
        <div className="eyebrow">Custom Print Request · {request.requestNumber}</div>
        <h1 className="page-title">
          Manufacturing<br />
          <span style={{ color: 'var(--accent-primary)' }}>Quote & Status.</span>
        </h1>
      </section>

      <section className="shell" style={{ paddingBottom: 100, maxWidth: 860 }}>
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-lg)',
            padding: '32px',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, paddingBottom: 20, borderBottom: '1px solid var(--border-subtle)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div
                style={{
                  width: 44,
                  height: 44,
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--accent-primary-subtle)',
                  color: 'var(--accent-primary)',
                  display: 'grid',
                  placeItems: 'center',
                }}
              >
                <FileCode size={24} />
              </div>
              <div>
                <strong style={{ fontSize: 16 }}>{request.fileName}</strong>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-muted)' }}>
                  .{request.fileType.toUpperCase()} Format
                </div>
              </div>
            </div>
            <StatusBadge status={request.status} />
          </div>

          <div className="detail-grid">
            <div className="detail-item">
              <span>FILAMENT MATERIAL</span>
              <b>{request.material}</b>
            </div>
            <div className="detail-item">
              <span>COLOR FINISH</span>
              <b>{request.color}</b>
            </div>
            <div className="detail-item">
              <span>QUANTITY</span>
              <b>{request.quantity} units</b>
            </div>
            <div className="detail-item">
              <span>SUBMITTED ON</span>
              <b>{new Date(request.createdAt).toLocaleDateString()}</b>
            </div>
          </div>

          {request.notes && (
            <div className="detail-item" style={{ marginBottom: 24 }}>
              <span>CUSTOMER SPECIFICATIONS</span>
              <p style={{ marginTop: 4, color: 'var(--text-secondary)' }}>{request.notes}</p>
            </div>
          )}

          {/* Admin Quote Box */}
          {request.adminQuote != null && (
            <div className="quote-box">
              <div className="eyebrow" style={{ color: 'var(--accent-primary)' }}>Official Studio Quote</div>
              <h3 style={{ fontSize: 24, margin: '6px 0 10px', color: 'var(--text-primary)' }}>
                {money(request.adminQuote)}
              </h3>
              {request.adminNotes && (
                <p style={{ color: 'var(--text-secondary)', fontSize: 14, marginBottom: 16 }}>
                  <b>Engineer's Notes:</b> {request.adminNotes}
                </p>
              )}

              {request.status === 'QUOTED' && (
                <div style={{ display: 'flex', gap: 12, marginTop: 20 }}>
                  <button
                    className="btn btn-primary"
                    disabled={busy}
                    onClick={() => respond('accept')}
                  >
                    <CheckCircle2 size={16} /> Accept Quote & Proceed
                  </button>
                  <button
                    className="btn secondary"
                    disabled={busy}
                    onClick={() => respond('reject')}
                  >
                    <XCircle size={16} /> Decline
                  </button>
                </div>
              )}

              {request.status === 'ACCEPTED' && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--accent-emerald)', fontWeight: 700, marginTop: 12 }}>
                  <CheckCircle2 size={18} /> Quote Accepted! This piece is scheduled for production.
                </div>
              )}

              {actionError && <div className="alert alert-error" style={{ marginTop: 16 }}>{actionError}</div>}
            </div>
          )}

          {request.status === 'SUBMITTED' && (
            <div style={{ background: 'var(--bg-subtle)', padding: '20px', borderRadius: 'var(--radius-md)', display: 'flex', alignItems: 'center', gap: 12 }}>
              <Clock size={20} color="var(--accent-primary)" />
              <span style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
                Your 3D model is in the review queue. Our engineer is slicing the mesh and calculating tolerances.
              </span>
            </div>
          )}
        </div>
      </section>
    </main>
  )
}

export default function CustomPrintDetailPage() {
  return (
    <RequireAuth>
      <CustomPrintDetail />
    </RequireAuth>
  )
}
