'use client'

import { useEffect, useState } from 'react'
import { CheckCircle2, Clock } from 'lucide-react'
import { StatusBadge } from '../../../components/StatusBadge'
import { api, ApiError } from '../../../lib/api'
import { money, type Order, type OrderStatus } from '../../../lib/types'

const STATUSES: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PRINTING', 'READY', 'SHIPPED', 'DELIVERED', 'CANCELLED']

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  function load() {
    setLoading(true)
    api.get<Order[]>('/admin/orders')
      .then(setOrders)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Could not load orders.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function updateStatus(order: Order, status: OrderStatus) {
    setUpdatingId(order.id)
    setError('')
    try {
      const updated = await api.put<Order>(`/admin/orders/${order.id}/status`, { status })
      setOrders(current => current.map(item => (item.id === order.id ? updated : item)))
      setNotice(`Order ${order.orderNumber} updated to ${status}`)
      setTimeout(() => setNotice(''), 3000)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update this order status.')
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <>
      <div style={{ marginBottom: 28 }}>
        <div className="eyebrow">Studio Production Line</div>
        <h1 className="admin-title" style={{ margin: 0 }}>Orders Management</h1>
      </div>

      {notice && <div className="alert alert-success">{notice}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {loading ? (
        <div className="loading-state">
          <div className="spinner" />
          <p>Loading studio orders…</p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Order Ref</th>
                <th>Customer Details</th>
                <th>Ordered Date</th>
                <th>Total Value</th>
                <th>Current Status</th>
                <th>Update Pipeline</th>
              </tr>
            </thead>
            <tbody>
              {orders.map(order => (
                <tr key={order.id}>
                  <td>
                    <b style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-primary)' }}>
                      {order.orderNumber}
                    </b>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                      {order.items.length} item{order.items.length > 1 ? 's' : ''}
                    </div>
                  </td>
                  <td>
                    <b>{order.customerName}</b>
                    <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>{order.customerEmail}</div>
                    <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>{order.shippingPhone}</div>
                  </td>
                  <td style={{ color: 'var(--text-secondary)' }}>
                    {new Date(order.createdAt).toLocaleDateString(undefined, {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </td>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700 }}>
                    {money(order.totalAmount)}
                  </td>
                  <td>
                    <StatusBadge status={order.status} />
                  </td>
                  <td>
                    <select
                      className="status-select"
                      value={order.status}
                      disabled={updatingId === order.id || order.status === 'DELIVERED' || order.status === 'CANCELLED'}
                      onChange={e => updateStatus(order, e.target.value as OrderStatus)}
                      style={{ minWidth: 140, padding: '8px 12px' }}
                    >
                      {STATUSES.map(status => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
              {orders.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '36px' }}>
                    No orders in database.
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
