'use client'

import { FormEvent, useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import { Download, ArrowLeft, FileCode, CheckCircle2, DollarSign, Send } from 'lucide-react'
import { StatusBadge } from '../../../../components/StatusBadge'
import { api, ApiError, API_URL, getToken } from '../../../../lib/api'
import { money, type CustomPrintRequest, type RequestStatus } from '../../../../lib/types'

const STATUSES: RequestStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'QUOTED',
  'ACCEPTED',
  'REJECTED',
  'IN_PRODUCTION',
  'COMPLETED',
  'CANCELLED',
]

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
    api.get<CustomPrintRequest>(`/admin/custom-prints/${params.id}`)
      .then(data => {
        setRequest(data)
        if (data.adminNotes) setAdminNotes(data.adminNotes)
        if (data.adminQuote != null) setAmount(String(data.adminQuote))
      })
      .catch(err => setError(err instanceof ApiError ? err.message : 'Request not found.'))
  }

  useEffect(load, [params.id])

  async function sendQuote(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const updated = await api.put<CustomPrintRequest>(`/admin/custom-prints/${params.id}/quote`, {
        amount: Number(amount),
        notes: adminNotes,
      })
      setRequest(updated)
      setNotice('Formal quote has been sent to the customer!')
      setTimeout(() => setNotice(''), 3000)
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
      setNotice(`Request status updated to ${status}`)
      setTimeout(() => setNotice(''), 3000)
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
      const res = await fetch(`${API_URL}/admin/custom-prints/${request.id}/file`, {
        headers: { Authorization: `Bearer ${getToken()}` },
      })
      if (!res.ok) throw new Error('download failed')
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = request.fileName
      link.click()
      URL.revokeObjectURL(url)
    } catch {
      setError('Could not download the 3D file.')
    } finally {
      setDownloading(false)
    }
  }

  if (error && !request) return <div className="alert alert-error">{error}</div>
  if (!request) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        <p>Loading 3D print request…</p>
      </div>
    )
  }

  const locked = request.status === 'COMPLETED' || request.status === 'CANCELLED'

  return (
    <div style={{ maxWidth: 880 }}>
      <div style={{ marginBottom: 20 }}>
        <Link href="/admin/custom-prints" className="link-btn">
          <ArrowLeft size={14} /> Back to Custom Requests
        </Link>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div className="eyebrow">Review 3D Model</div>
          <h1 className="admin-title" style={{ margin: 0 }}>{request.requestNumber}</h1>
        </div>
        <StatusBadge status={request.status} />
      </div>

      {notice && <div className="alert alert-success">{notice}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Request Details Card */}
      <div className="form-card" style={{ marginBottom: 28, maxWidth: 'none' }}>
        <h3 style={{ fontSize: 18, marginBottom: 16 }}>Customer & Model Specifications</h3>

        <div className="detail-grid" style={{ margin: '0 0 20px' }}>
          <div className="detail-item">
            <span>CUSTOMER NAME & EMAIL</span>
            <b>{request.customerName}</b>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{request.customerEmail}</div>
          </div>
          <div className="detail-item">
            <span>3D CAD FILE</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <FileCode size={16} color="var(--accent-primary)" />
              <b>{request.fileName}</b>
            </div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>.{request.fileType.toUpperCase()} Format</div>
          </div>
          <div className="detail-item">
            <span>REQUESTED FILAMENT</span>
            <b>{request.material} ({request.color})</b>
          </div>
          <div className="detail-item">
            <span>REQUESTED QUANTITY</span>
            <b>{request.quantity} pieces</b>
          </div>
        </div>

        {request.notes && (
          <div className="detail-item" style={{ marginBottom: 20 }}>
            <span>CUSTOMER NOTES & SPECIFICATIONS</span>
            <p style={{ marginTop: 4, color: 'var(--text-secondary)' }}>{request.notes}</p>
          </div>
        )}

        <button
          className="btn secondary"
          disabled={downloading}
          onClick={downloadFile}
          style={{ width: 'max-content' }}
        >
          <Download size={16} /> {downloading ? 'Downloading CAD File…' : 'Download Model File (.STL/.3MF/.OBJ)'}
        </button>
      </div>

      {/* Quote Generation Card */}
      <div className="form-card" style={{ marginBottom: 28, maxWidth: 'none' }}>
        <h3 style={{ fontSize: 18, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
          <DollarSign size={18} color="var(--accent-primary)" />
          Send Manufacturing Quote
        </h3>
        <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 20 }}>
          Enter the calculated quotation based on filament weight, machine hours, and support removal.
        </p>

        <form onSubmit={sendQuote} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="form-grid-2">
            <label>
              Total Quotation Amount (Rs.)
              <input
                required
                type="number"
                min="0.01"
                step="0.01"
                placeholder="2500.00"
                value={amount}
                onChange={e => setAmount(e.target.value)}
                disabled={locked}
              />
            </label>
          </div>

          <label>
            Engineer's Notes for Customer
            <textarea
              rows={3}
              placeholder="e.g. Sliced at 0.16mm layer height with 25% gyroid infill. Tree supports will be cleaned prior to dispatch."
              value={adminNotes}
              onChange={e => setAdminNotes(e.target.value)}
              disabled={locked}
            />
          </label>

          <button
            className="btn btn-primary"
            type="submit"
            disabled={submitting || locked}
            style={{ width: 'max-content', padding: '12px 24px' }}
          >
            <Send size={15} /> {submitting ? 'Sending Quote…' : request.adminQuote ? 'Update & Resend Quote' : 'Send Formal Quote'}
          </button>
        </form>
      </div>

      {/* Production Status Management */}
      <div className="form-card" style={{ maxWidth: 'none' }}>
        <h3 style={{ fontSize: 18, marginBottom: 14 }}>Update Production Pipeline Status</h3>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <select
            className="status-select"
            value={request.status}
            disabled={submitting || locked}
            onChange={e => updateStatus(e.target.value as RequestStatus)}
            style={{ minWidth: 180, padding: '10px 14px' }}
          >
            {STATUSES.map(status => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
          {request.adminQuote != null && (
            <span style={{ fontSize: 14, color: 'var(--text-secondary)' }}>
              Current Quote: <b style={{ color: 'var(--accent-primary)', fontFamily: 'var(--font-mono)' }}>{money(request.adminQuote)}</b>
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
