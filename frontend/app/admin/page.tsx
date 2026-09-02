'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { Box, ShoppingBag, Clock, Layers, DollarSign, ArrowRight, TrendingUp } from 'lucide-react'
import { StatusBadge } from '../../components/StatusBadge'
import { api, ApiError } from '../../lib/api'
import { money, type DashboardSummary } from '../../lib/types'

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<DashboardSummary>('/admin/dashboard')
      .then(setSummary)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Could not load the dashboard.'))
  }, [])

  if (error) return <div className="alert alert-error">{error}</div>
  if (!summary) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        <p>Loading studio analytics…</p>
      </div>
    )
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 28 }}>
        <div>
          <div className="eyebrow">Studio Overview</div>
          <h1 className="admin-title" style={{ margin: 0 }}>Dashboard</h1>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="dashboard-cards">
        <div className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="stat-label">Total Products</span>
            <Box size={16} color="var(--accent-primary)" />
          </div>
          <div className="stat-value">{summary.totalProducts}</div>
        </div>

        <div className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="stat-label">Total Orders</span>
            <ShoppingBag size={16} color="var(--accent-blue)" />
          </div>
          <div className="stat-value">{summary.totalOrders}</div>
        </div>

        <div className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="stat-label">Pending Orders</span>
            <Clock size={16} color="var(--accent-amber)" />
          </div>
          <div className="stat-value">{summary.pendingOrders}</div>
        </div>

        <div className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="stat-label">Custom STL Queue</span>
            <Layers size={16} color="var(--accent-primary)" />
          </div>
          <div className="stat-value">{summary.customRequests}</div>
        </div>

        <div className="stat-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="stat-label">Total Revenue</span>
            <DollarSign size={16} color="var(--accent-emerald)" />
          </div>
          <div className="stat-value" style={{ color: 'var(--accent-emerald)', fontSize: 22 }}>
            {money(summary.revenue)}
          </div>
        </div>
      </div>

      {/* Recent Orders Section */}
      <div className="admin-section">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3>Recent Orders</h3>
          <Link href="/admin/orders" className="link-btn">
            View all orders <ArrowRight size={14} />
          </Link>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Order Ref</th>
                <th>Customer</th>
                <th>Total</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {summary.recentOrders.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '32px' }}>
                    No orders placed yet.
                  </td>
                </tr>
              )}
              {summary.recentOrders.map(order => (
                <tr key={order.id}>
                  <td>
                    <Link
                      href="/admin/orders"
                      style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--accent-primary)' }}
                    >
                      {order.orderNumber}
                    </Link>
                  </td>
                  <td>{order.customerName}</td>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700 }}>{money(order.totalAmount)}</td>
                  <td>
                    <StatusBadge status={order.status} />
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <Link className="link-btn" href="/admin/orders">
                      Manage <ArrowRight size={13} />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent Custom Prints Section */}
      <div className="admin-section">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3>Recent Custom 3D Print Requests</h3>
          <Link href="/admin/custom-prints" className="link-btn">
            View all requests <ArrowRight size={14} />
          </Link>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Request Ref</th>
                <th>Customer</th>
                <th>Material</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {summary.recentCustomRequests.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '32px' }}>
                    No custom print requests in queue.
                  </td>
                </tr>
              )}
              {summary.recentCustomRequests.map(request => (
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
                  <td>{request.material}</td>
                  <td>
                    <StatusBadge status={request.status} />
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <Link className="link-btn" href={`/admin/custom-prints/${request.id}`}>
                      Review Model <ArrowRight size={13} />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
