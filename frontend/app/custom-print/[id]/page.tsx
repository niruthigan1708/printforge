'use client'
import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
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
    api.get<CustomPrintRequest>(`/custom-prints/${params.id}`).then(setRequest).catch(err => setError(err instanceof ApiError ? err.message : 'Request not found.'))
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

  if (error) return <main><Header /><section className="shell page-intro"><div className="empty"><h2>{error}</h2></div></section></main>
  if (!request) return <main><Header /><div className="shell loading-state"><div className="spinner" />Loading…</div></main>

  return (
    <main>
      <Header />
      <section className="shell page-intro compact">
        <div className="eyebrow">Custom request {request.requestNumber}</div>
        <h1 className="page-title">Here is<br /><span style={{ color: 'var(--orange)' }}>the status.</span></h1>
      </section>
      <section className="shell section" style={{ maxWidth: 700 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span className="file-chip">{request.fileName} · .{request.fileType}</span>
          <StatusBadge status={request.status} />
        </div>
        <div className="detail-grid">
          <div className="detail-item"><span>Material</span>{request.material}</div>
          <div className="detail-item"><span>Color</span>{request.color}</div>
          <div className="detail-item"><span>Quantity</span>{request.quantity}</div>
          <div className="detail-item"><span>Submitted</span>{new Date(request.createdAt).toLocaleDateString()}</div>
        </div>
        {request.notes && <div className="detail-item" style={{ marginBottom: 20 }}><span>Your notes</span>{request.notes}</div>}

        {request.adminQuote != null && (
          <div className="quote-box">
            <h3>Quoted at {money(request.adminQuote)}</h3>
            {request.adminNotes && <p>{request.adminNotes}</p>}
            {request.status === 'QUOTED' && (
              <div className="actions">
                <button className="btn" disabled={busy} onClick={() => respond('accept')}>Accept quote</button>
                <button className="btn secondary" disabled={busy} onClick={() => respond('reject')}>Reject quote</button>
              </div>
            )}
            {actionError && <div className="alert alert-error" style={{ marginTop: 14 }}>{actionError}</div>}
          </div>
        )}

        {request.status === 'SUBMITTED' && <p className="hero-copy">Your request is in the queue and hasn't been reviewed yet.</p>}
        {request.status === 'UNDER_REVIEW' && <p className="hero-copy">Our team is reviewing your file and will send a quote shortly.</p>}
      </section>
    </main>
  )
}

export default function CustomPrintDetailPage() {
  return <RequireAuth><CustomPrintDetail /></RequireAuth>
}
