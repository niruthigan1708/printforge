'use client'
import Link from 'next/link'
import { useEffect, useState } from 'react'
import { StatusBadge } from '../../../components/StatusBadge'
import { api, ApiError } from '../../../lib/api'
import type { CustomPrintRequest } from '../../../lib/types'

export default function AdminCustomPrintsPage() {
  const [requests, setRequests] = useState<CustomPrintRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<CustomPrintRequest[]>('/admin/custom-prints').then(setRequests).catch(err => setError(err instanceof ApiError ? err.message : 'Could not load custom requests.')).finally(() => setLoading(false))
  }, [])

  return (
    <>
      <h1 className="admin-title">Custom print requests</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? <div className="loading-state"><div className="spinner" />Loading requests…</div> : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Request</th><th>Customer</th><th>File</th><th>Material</th><th>Qty</th><th>Status</th><th>Date</th></tr></thead>
            <tbody>
              {requests.map(request => (
                <tr key={request.id}>
                  <td><Link href={`/admin/custom-prints/${request.id}`}><b>{request.requestNumber}</b></Link></td>
                  <td>{request.customerName}</td>
                  <td>{request.fileName}</td>
                  <td>{request.material} · {request.color}</td>
                  <td>{request.quantity}</td>
                  <td><StatusBadge status={request.status} /></td>
                  <td>{new Date(request.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
              {requests.length === 0 && <tr><td colSpan={7}>No custom requests yet.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
