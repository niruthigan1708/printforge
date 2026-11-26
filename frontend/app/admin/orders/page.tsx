'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { AlertTriangle } from 'lucide-react'
import { StatusBadge } from '../../../components/StatusBadge'
import { api, ApiError } from '../../../lib/api'
import { money, type Order, type OrderStatus, type Page } from '../../../lib/types'
import { Pagination } from '../../../components/Pagination'


export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [updatingId, setUpdatingId] = useState<number | null>(null)

  const [pageNumber, setPageNumber] = useState(0)
  const [pageInfo, setPageInfo] = useState<Page<Order> | null>(null)

  // Loading is derived from which page last finished, so the effect never sets it synchronously.
  const [finishedPage, setFinishedPage] = useState<number | null>(null)
  const loading = finishedPage !== pageNumber

  useEffect(() => {
    let stale = false
    const page = pageNumber
    api.get<Page<Order>>(`/admin/orders?page=${page}&size=20`)
      .then(result => { if (!stale) { setPageInfo(result); setOrders(result.content) } })
      .catch(err => { if (!stale) setError(err instanceof ApiError ? err.message : 'Could not load orders.') })
      .finally(() => { if (!stale) setFinishedPage(page) })
    return () => { stale = true }
  }, [pageNumber])

  async function updateStatus(order: Order, status: OrderStatus) {
    // Orders only move forward, so a mistaken change cannot be reverted.
    const prompt = status === 'CANCELLED'
      ? `Cancel order ${order.orderNumber}? Its items will be returned to stock. This cannot be undone.`
      : `Move order ${order.orderNumber} to ${status}? Orders cannot be moved back to an earlier status.`
    let trackingNumber: string | undefined
    if (status === 'SHIPPED') {
      // An order can only be delivered after it ships, and shipping needs a tracking number.
      const entered = window.prompt(`Tracking number for order ${order.orderNumber}:`)
      if (entered === null) return
      if (!entered.trim()) { setError('A tracking number is required to mark an order as shipped.'); return }
      trackingNumber = entered.trim()
    } else if (!confirm(prompt)) return
    setUpdatingId(order.id)
    setError('')
    try {
      const updated = await api.put<Order>(`/admin/orders/${order.id}/status`, { status, trackingNumber })
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
                    <Link href={`/admin/orders/${order.id}`} style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--accent-primary)' }}>
                      {order.orderNumber}
                    </Link>
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
                    {order.issue?.open && (
                      <div><span className="issue-flag"><AlertTriangle size={11} /> Not received</span></div>
                    )}
                  </td>
                  <td>
                    <select
                      className="status-select"
                      value={order.status}
                      disabled={updatingId === order.id || order.nextStatuses.length === 0}
                      onChange={e => updateStatus(order, e.target.value as OrderStatus)}
                      style={{ minWidth: 140, padding: '8px 12px' }}
                    >
                      {/* Only the current status plus the moves the server allows. */}
                      {[order.status, ...order.nextStatuses].map(status => (
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
      <Pagination page={pageInfo} onChange={setPageNumber} />
    </>
  )
}
