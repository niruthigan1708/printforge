'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { Package, ArrowRight, Clock } from 'lucide-react'
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
    api.get<Order[]>('/orders')
      .then(setOrders)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Could not load your orders.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <main>
      <Header />

      <section className="shell page-intro compact" style={{ padding: '50px 0 30px' }}>
        <div className="eyebrow">Customer Dashboard · Orders</div>
        <h1 className="page-title">
          Track what you've<br />
          <span style={{ color: 'var(--accent-primary)' }}>ordered & printed.</span>
        </h1>
      </section>

      <section className="shell" style={{ paddingBottom: 100 }}>
        {loading && (
          <div className="loading-state">
            <div className="spinner" />
            <p>Loading your orders…</p>
          </div>
        )}

        {!loading && error && <div className="alert alert-error">{error}</div>}

        {!loading && !error && orders.length === 0 && (
          <div className="empty" style={{ padding: '60px 40px' }}>
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: '50%',
                background: 'var(--bg-subtle)',
                color: 'var(--text-muted)',
                display: 'grid',
                placeItems: 'center',
                margin: '0 auto 16px',
              }}
            >
              <Package size={26} />
            </div>
            <h2>No orders placed yet.</h2>
            <p style={{ marginTop: 8 }}>Once you order an object or custom print, you can track fabrication here.</p>
            <Link className="btn btn-primary" style={{ marginTop: 24 }} href="/products">
              Explore Collection
            </Link>
          </div>
        )}

        {!loading && orders.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Order Reference</th>
                  <th>Placed Date</th>
                  <th>Items Count</th>
                  <th>Total Amount</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.map(order => (
                  <tr key={order.id}>
                    <td>
                      <Link
                        href={`/orders/${order.id}`}
                        style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--accent-primary)' }}
                      >
                        {order.orderNumber}
                      </Link>
                    </td>
                    <td style={{ color: 'var(--text-secondary)' }}>
                      {new Date(order.createdAt).toLocaleDateString(undefined, {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                      })}
                    </td>
                    <td>{order.items.length} item{order.items.length > 1 ? 's' : ''}</td>
                    <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700 }}>
                      {money(order.totalAmount)}
                    </td>
                    <td>
                      <StatusBadge status={order.status} />
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <Link className="link-btn" href={`/orders/${order.id}`} style={{ color: 'var(--accent-primary)' }}>
                        Track <ArrowRight size={14} />
                      </Link>
                    </td>
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
  return (
    <RequireAuth>
      <OrdersList />
    </RequireAuth>
  )
}
