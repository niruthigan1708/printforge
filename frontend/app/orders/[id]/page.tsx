'use client'
import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
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
    api.get<Order>(`/orders/${params.id}`).then(setOrder).catch(err => setError(err instanceof ApiError ? err.message : 'Order not found.'))
  }, [params.id])

  if (error) return <main><Header /><section className="shell page-intro"><div className="empty"><h2>{error}</h2></div></section></main>
  if (!order) return <main><Header /><div className="shell loading-state"><div className="spinner" />Loading…</div></main>

  return (
    <main>
      <Header />
      <section className="shell page-intro compact">
        <div className="eyebrow">Order {order.orderNumber}</div>
        <h1 className="page-title">Here is<br /><span style={{ color: 'var(--orange)' }}>the status.</span></h1>
      </section>
      <section className="shell section">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <span>Placed on {new Date(order.createdAt).toLocaleString()}</span>
          <StatusBadge status={order.status} />
        </div>
        <OrderTracker status={order.status} />

        <div className="checkout-grid" style={{ marginTop: 30 }}>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Item</th><th>Qty</th><th>Unit price</th><th>Subtotal</th></tr></thead>
              <tbody>{order.items.map((item, index) => <tr key={index}><td>{item.name}</td><td>{item.quantity}</td><td>{money(item.unitPrice)}</td><td>{money(item.subtotal)}</td></tr>)}</tbody>
            </table>
          </div>
          <aside className="summary">
            <div className="eyebrow">Shipping to</div>
            <h2 style={{ fontSize: 20 }}>{order.shippingName}</h2>
            <p className="hero-copy">{order.shippingAddress}<br />{order.shippingCity}, {order.shippingPostalCode}<br />{order.shippingPhone}</p>
            <div className="summary-line"><span>Subtotal</span><b>{money(order.subtotal)}</b></div>
            <div className="summary-line"><span>Delivery</span><b>{money(order.deliveryFee)}</b></div>
            <div className="summary-total"><span>Total</span><b>{money(order.totalAmount)}</b></div>
          </aside>
        </div>
      </section>
    </main>
  )
}

export default function OrderDetailPage() {
  return <RequireAuth><OrderDetail /></RequireAuth>
}
