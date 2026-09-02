'use client'

import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, Clock, MapPin, PackageCheck, Truck } from 'lucide-react'
import { Header } from '../../../components/Header'
import { StatusBadge } from '../../../components/StatusBadge'
import { OrderTracker } from '../../../components/OrderTracker'
import { RequireAuth } from '../../../lib/guards'
import { api, ApiError } from '../../../lib/api'
import { money, type Order } from '../../../lib/types'

function OrderDetail() {
  const params = useParams<{ id: string }>()
  const [order, setOrder] = useState<Order | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<Order>(`/orders/${params.id}`)
      .then(setOrder)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Order not found.'))
  }, [params.id])

  if (error) {
    return (
      <main>
        <Header />
        <section className="shell" style={{ padding: '80px 0' }}>
          <div className="empty">
            <div className="eyebrow" style={{ justifyContent: 'center' }}>Error</div>
            <h2>{error}</h2>
            <Link className="btn btn-primary" style={{ marginTop: 20 }} href="/orders">
              <ArrowLeft size={16} /> Back to Orders
            </Link>
          </div>
        </section>
      </main>
    )
  }

  if (!order) {
    return (
      <main>
        <Header />
        <div className="shell loading-state" style={{ margin: '80px auto' }}>
          <div className="spinner" />
          <p>Loading order status…</p>
        </div>
      </main>
    )
  }

  return (
    <main>
      <Header />

      <section className="shell page-intro compact" style={{ padding: '50px 0 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
          <Link href="/orders" className="link-btn">
            <ArrowLeft size={14} /> Back to Orders
          </Link>
        </div>
        <div className="eyebrow">Order Reference · {order.orderNumber}</div>
        <h1 className="page-title">
          Live Production<br />
          <span style={{ color: 'var(--accent-primary)' }}>& Delivery Status.</span>
        </h1>
      </section>

      <section className="shell" style={{ paddingBottom: 100 }}>
        {/* Tracker Card */}
        <div
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-lg)',
            padding: '28px 32px',
            boxShadow: 'var(--shadow-sm)',
            marginBottom: 36,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 12 }}>
            <div>
              <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>Ordered on </span>
              <strong style={{ fontSize: 14 }}>
                {new Date(order.createdAt).toLocaleString(undefined, {
                  dateStyle: 'medium',
                  timeStyle: 'short',
                })}
              </strong>
            </div>
            <StatusBadge status={order.status} />
          </div>

          <OrderTracker status={order.status} />
        </div>

        {/* Order Details Grid */}
        <div className="checkout-grid" style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: 40 }}>
          <div>
            <h3 style={{ fontSize: 18, marginBottom: 16 }}>Items in Production ({order.items.length})</h3>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Item Description</th>
                    <th>Qty</th>
                    <th>Unit Price</th>
                    <th style={{ textAlign: 'right' }}>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {order.items.map((item, index) => (
                    <tr key={index}>
                      <td>
                        <b>{item.name}</b>
                      </td>
                      <td style={{ fontFamily: 'var(--font-mono)' }}>{item.quantity}</td>
                      <td style={{ fontFamily: 'var(--font-mono)' }}>{money(item.unitPrice)}</td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, textAlign: 'right' }}>
                        {money(item.subtotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Shipping & Payment Card */}
          <aside className="summary">
            <div className="eyebrow">Delivery & Payment</div>
            <h3 style={{ fontSize: 18, margin: '8px 0 16px', display: 'flex', alignItems: 'center', gap: 8 }}>
              <MapPin size={18} color="var(--accent-primary)" />
              {order.shippingName}
            </h3>

            <p style={{ fontSize: 14, lineHeight: 1.6, color: 'var(--text-secondary)', marginBottom: 20 }}>
              {order.shippingAddress}<br />
              {order.shippingCity}, {order.shippingPostalCode}<br />
              Phone: <b>{order.shippingPhone}</b>
            </p>

            <div className="summary-line">
              <span>Items Subtotal</span>
              <b>{money(order.subtotal)}</b>
            </div>
            <div className="summary-line">
              <span>Delivery Fee</span>
              <b>{order.deliveryFee === 0 ? <span style={{ color: 'var(--accent-emerald)' }}>FREE</span> : money(order.deliveryFee)}</b>
            </div>
            <div className="summary-total">
              <span>Total Amount</span>
              <span style={{ color: 'var(--accent-primary)', fontFamily: 'var(--font-mono)' }}>{money(order.totalAmount)}</span>
            </div>

            <div style={{ background: 'var(--bg-subtle)', padding: '12px 16px', borderRadius: 'var(--radius-sm)', fontSize: 12, color: 'var(--text-muted)' }}>
              Payment Mode: <b>Cash on Delivery / Bank Transfer</b>
            </div>
          </aside>
        </div>
      </section>
    </main>
  )
}

export default function OrderDetailPage() {
  return (
    <RequireAuth>
      <OrderDetail />
    </RequireAuth>
  )
}
