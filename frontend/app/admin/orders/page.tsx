'use client'
import { useEffect, useState } from 'react'
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
    api.get<Order[]>('/admin/orders').then(setOrders).catch(err => setError(err instanceof ApiError ? err.message : 'Could not load orders.')).finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function updateStatus(order: Order, status: OrderStatus) {
    setUpdatingId(order.id)
    setError('')
    try {
      const updated = await api.put<Order>(`/admin/orders/${order.id}/status`, { status })
      setOrders(current => current.map(item => (item.id === order.id ? updated : item)))
      setNotice(`${order.orderNumber} updated to ${status}`)
      setTimeout(() => setNotice(''), 2500)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update this order.')
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <>
      <h1 className="admin-title">Orders</h1>
      {notice && <div className="alert alert-success">{notice}</div>}
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? <div className="loading-state"><div className="spinner" />Loading orders…</div> : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Order</th><th>Customer</th><th>Date</th><th>Total</th><th>Status</th><th>Update</th></tr></thead>
            <tbody>
              {orders.map(order => (
                <tr key={order.id}>
                  <td><b>{order.orderNumber}</b></td>
                  <td>{order.customerName}<br /><span style={{ color: 'var(--muted)', fontSize: 12 }}>{order.customerEmail}</span></td>
                  <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                  <td>{money(order.totalAmount)}</td>
                  <td><StatusBadge status={order.status} /></td>
                  <td>
                    <select className="status-select" value={order.status} disabled={updatingId === order.id || order.status === 'DELIVERED' || order.status === 'CANCELLED'} onChange={e => updateStatus(order, e.target.value as OrderStatus)}>
                      {STATUSES.map(status => <option key={status} value={status}>{status}</option>)}
                    </select>
                  </td>
                </tr>
              ))}
              {orders.length === 0 && <tr><td colSpan={6}>No orders yet.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
