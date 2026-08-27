'use client'
import Link from 'next/link'
import { useEffect, useState } from 'react'
import { Header } from '../../components/Header'
import { StatusBadge } from '../../components/StatusBadge'
import { RequireAuth } from '../../lib/guards'
import { api, ApiError } from '../../lib/api'
import { money, type Order } from '../../lib/types'

function OrdersList() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<Order[]>('/orders').then(setOrders).catch(err => setError(err instanceof ApiError ? err.message : 'Could not load your orders.')).finally(() => setLoading(false))
  }, [])

  return (
    <main>
      <Header />
      <section className="shell page-intro compact">
        <div className="eyebrow">Your orders</div>
        <h1 className="page-title">Track what<br /><span style={{ color: 'var(--orange)' }}>you made.</span></h1>
      </section>
      <section className="shell section">
        {loading && <div className="loading-state"><div className="spinner" />Loading your orders…</div>}
        {!loading && error && <div className="alert alert-error">{error}</div>}
        {!loading && !error && orders.length === 0 && <div className="empty"><h2>No orders yet.</h2><p>Once you place an order, it will show up here.</p><Link className="btn" style={{ marginTop: 16 }} href="/products">Browse products</Link></div>}
        {!loading && orders.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Order</th><th>Date</th><th>Items</th><th>Total</th><th>Status</th></tr></thead>
              <tbody>
                {orders.map(order => (
                  <tr key={order.id}>
                    <td><Link href={`/orders/${order.id}`}><b>{order.orderNumber}</b></Link></td>
                    <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                    <td>{order.items.length}</td>
                    <td>{money(order.totalAmount)}</td>
                    <td><StatusBadge status={order.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  )
}

export default function OrdersPage() {
  return <RequireAuth><OrdersList /></RequireAuth>
}
