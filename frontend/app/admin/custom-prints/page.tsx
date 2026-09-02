'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { ArrowRight, FileCode, Layers } from 'lucide-react'
import { StatusBadge } from '../../../components/StatusBadge'
import { api, ApiError } from '../../../lib/api'
import type { CustomPrintRequest } from '../../../lib/types'

export default function AdminCustomPrintsPage() {
  const [requests, setRequests] = useState<CustomPrintRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<CustomPrintRequest[]>('/admin/custom-prints')
      .then(setRequests)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Could not load custom requests.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <>
      <div style={{ marginBottom: 28 }}>
        <div className="eyebrow">On-Demand Slicing & Prototyping</div>
        <h1 className="admin-title" style={{ margin: 0 }}>Custom 3D Print Requests</h1>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? (
        <div className="loading-state">
          <div className="spinner" />
          <p>Loading 3D print queue…</p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Request Ref</th>
                <th>Customer</th>
                <th>3D File Name</th>
                <th>Filament & Color</th>
                <th>Qty</th>
                <th>Status</th>
                <th>Submitted</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {requests.map(request => (
                <tr key={request.id}>
                  <td>
                    <Link
                      href={`/admin/custom-prints/${request.id}`}
                      style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--accent-primary)' }}
                    >
                      {request.requestNumber}
                    </Link>
                  </td>
                  <td>{request.customerName}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <FileCode size={14} color="var(--accent-primary)" />
                      <span>{request.fileName}</span>
                    </div>
                  </td>
                  <td>{request.material} · {request.color}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{request.quantity}</td>
                  <td>
                    <StatusBadge status={request.status} />
                  </td>
                  <td style={{ color: 'var(--text-secondary)' }}>
                    {new Date(request.createdAt).toLocaleDateString()}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <Link
                      className="btn btn-sm secondary"
                      href={`/admin/custom-prints/${request.id}`}
                    >
                      Review & Quote <ArrowRight size={13} />
                    </Link>
                  </td>
                </tr>
              ))}
              {requests.length === 0 && (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '36px' }}>
                    No custom print requests in queue.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
