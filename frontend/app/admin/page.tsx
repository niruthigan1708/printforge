'use client'
import Link from 'next/link'
import { useEffect, useState } from 'react'
import { StatusBadge } from '../../components/StatusBadge'
import { api, ApiError } from '../../lib/api'
import { money, type DashboardSummary } from '../../lib/types'

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<DashboardSummary>('/admin/dashboard').then(setSummary).catch(err => setError(err instanceof ApiError ? err.message : 'Could not load the dashboard.'))
  }, [])

  if (error) return <div className="alert alert-error">{error}</div>
  if (!summary) return <div className="loading-state"><div className="spinner" />Loading dashboard…</div>

  return (
    <>
      <h1 className="admin-title">Dashboard</h1>
      <div className="dashboard-cards">
        <div className="stat-card"><div className="stat-label">Total products</div><div className="stat-value">{summary.totalProducts}</div></div>
        <div className="stat-card"><div className="stat-label">Total orders</div><div className="stat-value">{summary.totalOrders}</div></div>
        <div className="stat-card"><div className="stat-label">Pending orders</div><div className="stat-value">{summary.pendingOrders}</div></div>
        <div className="stat-card"><div className="stat-label">Custom requests</div><div className="stat-value">{summary.customRequests}</div></div>
        <div className="stat-card"><div className="stat-label">Revenue</div><div className="stat-value">{money(summary.revenue)}</div></div>
      </div>

      <div className="admin-section">
        <h3>Recent orders</h3>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Order</th><th>Customer</th><th>Total</th><th>Status</th></tr></thead>
            <tbody>
              {summary.recentOrders.length === 0 && <tr><td colSpan={4}>No orders yet.</td></tr>}
              {summary.recentOrders.map(order => (
                <tr key={order.id}><td><Link href="/admin/orders"><b>{order.orderNumber}</b></Link></td><td>{order.customerName}</td><td>{money(order.totalAmount)}</td><td><StatusBadge status={order.status} /></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="admin-section">
        <h3>Recent custom requests</h3>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Request</th><th>Customer</th><th>Material</th><th>Status</th></tr></thead>
            <tbody>
              {summary.recentCustomRequests.length === 0 && <tr><td colSpan={4}>No custom requests yet.</td></tr>}
              {summary.recentCustomRequests.map(request => (
                <tr key={request.id}><td><Link href="/admin/custom-prints"><b>{request.requestNumber}</b></Link></td><td>{request.customerName}</td><td>{request.material}</td><td><StatusBadge status={request.status} /></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
