'use client'
import { FormEvent, useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import { Download } from 'lucide-react'
import { StatusBadge } from '../../../../components/StatusBadge'
import { api, ApiError, API_URL, getToken } from '../../../../lib/api'
import { money, type CustomPrintRequest, type RequestStatus } from '../../../../lib/types'

const STATUSES: RequestStatus[] = ['SUBMITTED', 'UNDER_REVIEW', 'QUOTED', 'ACCEPTED', 'REJECTED', 'IN_PRODUCTION', 'COMPLETED', 'CANCELLED']

export default function AdminCustomPrintDetailPage() {
  const params = useParams<{ id: string }>()
  const [request, setRequest] = useState<CustomPrintRequest | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [amount, setAmount] = useState('')
  const [adminNotes, setAdminNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [downloading, setDownloading] = useState(false)

  function load() {
    api.get<CustomPrintRequest>(`/admin/custom-prints/${params.id}`).then(data => {
      setRequest(data)
      if (data.adminNotes) setAdminNotes(data.adminNotes)
      if (data.adminQuote != null) setAmount(String(data.adminQuote))
    }).catch(err => setError(err instanceof ApiError ? err.message : 'Request not found.'))
  }

  useEffect(load, [params.id])

  async function sendQuote(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const updated = await api.put<CustomPrintRequest>(`/admin/custom-prints/${params.id}/quote`, { amount: Number(amount), notes: adminNotes })
      setRequest(updated)
      setNotice('Quote sent to the customer')
      setTimeout(() => setNotice(''), 2500)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not send this quote.')
    } finally {
      setSubmitting(false)
    }
  }

  async function updateStatus(status: RequestStatus) {
    setSubmitting(true)
    setError('')
    try {
      const updated = await api.put<CustomPrintRequest>(`/admin/custom-prints/${params.id}/status`, { status })
      setRequest(updated)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update the status.')
    } finally {
      setSubmitting(false)
    }
  }

  async function downloadFile() {
    if (!request) return
    setDownloading(true)
    try {
      const res = await fetch(`${API_URL}/admin/custom-prints/${request.id}/file`, { headers: { Authorization: `Bearer ${getToken()}` } })
      if (!res.ok) throw new Error('download failed')
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = request.fileName
      link.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('Could not download the file.')
    } finally {
      setDownloading(false)
    }
  }

  if (error && !request) return <div className="empty"><h2>{error}</h2></div>
  if (!request) return <div className="loading-state"><div className="spinner" />Loading…</div>

  const locked = request.status === 'COMPLETED' || request.status === 'CANCELLED'

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h1 className="admin-title" style={{ margin: 0 }}>{request.requestNumber}</h1>
        <StatusBadge status={request.status} />
      </div>
      {notice && <div className="alert alert-success">{notice}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <div className="detail-grid">
        <div className="detail-item"><span>Customer</span>{request.customerName} ({request.customerEmail})</div>
        <div className="detail-item"><span>File</span>{request.fileName} (.{request.fileType})</div>
        <div className="detail-item"><span>Material</span>{request.material}</div>
        <div className="detail-item"><span>Color</span>{request.color}</div>
        <div className="detail-item"><span>Quantity</span>{request.quantity}</div>
        <div className="detail-item"><span>Submitted</span>{new Date(request.createdAt).toLocaleString()}</div>
      </div>
      {request.notes && <div className="detail-item" style={{ marginBottom: 20 }}><span>Customer notes</span>{request.notes}</div>}

      <button className="btn btn-sm" disabled={downloading} onClick={downloadFile} style={{ marginBottom: 30 }}><Download size={14} style={{ verticalAlign: 'middle', marginRight: 6 }} />{downloading ? 'Downloading…' : 'Download file'}</button>

      <div className="admin-section">
        <h3>Quote</h3>
        <form className="form-card" onSubmit={sendQuote}>
          <div className="form-grid-2">
            <label>Quote amount (Rs.)<input required type="number" min="0.01" step="0.01" value={amount} onChange={e => setAmount(e.target.value)} disabled={locked} /></label>
          </div>
          <label>Admin notes<textarea rows={3} value={adminNotes} onChange={e => setAdminNotes(e.target.value)} disabled={locked} /></label>
          <button className="btn orange" type="submit" disabled={submitting || locked}>{submitting ? 'Sending…' : 'Send quote'}</button>
        </form>
      </div>

      <div className="admin-section">
        <h3>Production status</h3>
        <select className="status-select" value={request.status} disabled={submitting || locked} onChange={e => updateStatus(e.target.value as RequestStatus)}>
          {STATUSES.map(status => <option key={status} value={status}>{status}</option>)}
        </select>
        {request.adminQuote != null && <p style={{ marginTop: 14 }}>Current quote: <b>{money(request.adminQuote)}</b></p>}
      </div>
    </>
  )
}
