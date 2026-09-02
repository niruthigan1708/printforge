'use client'

import Link from 'next/link'
import { ArrowLeft, CheckCircle2, ShieldCheck, Truck, ArrowRight, Box } from 'lucide-react'
import { FormEvent, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Header } from '../../components/Header'
import { money, type Order } from '../../lib/types'
import { useCart } from '../../lib/cart-context'
import { useAuth } from '../../lib/auth-context'
import { api, ApiError } from '../../lib/api'

export default function CheckoutPage() {
  const { items, subtotal, deliveryFee, total, clear } = useCart()
  const { user, loading } = useAuth()
  const router = useRouter()
  const [order, setOrder] = useState<Order | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    shippingName: '',
    shippingPhone: '',
    shippingAddress: '',
    shippingCity: '',
    shippingPostalCode: '',
  })

  useEffect(() => {
    if (!loading && !user) router.replace('/login?redirect=/checkout')
  }, [loading, user, router])

  useEffect(() => {
    if (user) setForm(current => ({ ...current, shippingName: current.shippingName || user.name }))
  }, [user])

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const created = await api.post<Order>('/orders', {
        items: items.map(item => ({ productId: item.productId, quantity: item.quantity })),
        ...form,
      })
      clear()
      setOrder(created)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not place your order. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  if (order) {
    return (
      <main>
        <Header />
        <section className="shell" style={{ padding: '70px 0 100px' }}>
          <div
            className="form-card"
            style={{
              maxWidth: 680,
              margin: '0 auto',
              padding: '48px 40px',
              textAlign: 'center',
            }}
          >
            <div
              style={{
                width: 68,
                height: 68,
                borderRadius: '50%',
                background: 'var(--accent-emerald-subtle)',
                color: 'var(--accent-emerald)',
                display: 'grid',
                placeItems: 'center',
                margin: '0 auto 20px',
              }}
            >
              <CheckCircle2 size={40} />
            </div>

            <div className="eyebrow" style={{ justifyContent: 'center' }}>
              Order Confirmed · {order.orderNumber}
            </div>
            <h2>Your order is in the studio queue!</h2>
            <p style={{ marginTop: 12, color: 'var(--text-secondary)' }}>
              Thank you for ordering with PrintForge. We have received your order and scheduled the 3D printing pipeline. You will be notified as each stage finishes.
            </p>

            <div
              style={{
                background: 'var(--bg-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '20px',
                margin: '28px 0',
                textAlign: 'left',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontSize: 13 }}>
                <span style={{ color: 'var(--text-muted)' }}>DELIVERY TO:</span>
                <b>{order.shippingName} ({order.shippingPhone})</b>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontSize: 13 }}>
                <span style={{ color: 'var(--text-muted)' }}>ADDRESS:</span>
                <b>{order.shippingAddress}, {order.shippingCity}</b>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14, paddingTop: 10, borderTop: '1px solid var(--border-subtle)' }}>
                <span>Total Payment on Delivery:</span>
                <b style={{ color: 'var(--accent-primary)', fontFamily: 'var(--font-mono)' }}>{money(order.totalAmount)}</b>
              </div>
            </div>

            <div style={{ display: 'flex', gap: 14, justifyContent: 'center' }}>
              <Link className="btn btn-primary" href={`/orders/${order.id}`}>
                Track Live Order Pipeline <ArrowRight size={16} />
              </Link>
              <Link className="btn secondary" href="/products">
                Continue Shopping
              </Link>
            </div>
          </div>
        </section>
      </main>
    )
  }

  if (!loading && items.length === 0) {
    return (
      <main>
        <Header />
        <section className="shell" style={{ padding: '80px 0' }}>
          <div className="empty">
            <h2>Your cart is empty.</h2>
            <p style={{ marginTop: 8 }}>Add objects to your cart before proceeding to checkout.</p>
            <Link className="btn btn-primary" style={{ marginTop: 20 }} href="/products">
              Browse Catalog
            </Link>
          </div>
        </section>
      </main>
    )
  }

  return (
    <main>
      <Header />

      <section className="shell page-intro compact" style={{ padding: '50px 0 30px' }}>
        <div className="eyebrow">Checkout · Delivery & Payment</div>
        <h1 className="page-title">
          Finalize your<br />
          <span style={{ color: 'var(--accent-primary)' }}>delivery details.</span>
        </h1>
      </section>

      <section className="shell" style={{ paddingBottom: 100 }}>
        <div className="checkout-grid" style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: 50 }}>
          {/* Shipping Form */}
          <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {error && <div className="alert alert-error">{error}</div>}

            <div
              style={{
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-lg)',
                padding: '28px',
                boxShadow: 'var(--shadow-sm)',
                display: 'flex',
                flexDirection: 'column',
                gap: 18,
              }}
            >
              <h3 style={{ fontSize: 18, display: 'flex', alignItems: 'center', gap: 8 }}>
                <Truck size={18} color="var(--accent-primary)" />
                Shipping & Contact Information
              </h3>

              <div className="form-two">
                <label>
                  Full Name
                  <input
                    required
                    placeholder="Recipient's name"
                    value={form.shippingName}
                    onChange={e => setForm({ ...form, shippingName: e.target.value })}
                  />
                </label>
                <label>
                  Phone Number
                  <input
                    required
                    placeholder="07X XXX XXXX"
                    value={form.shippingPhone}
                    onChange={e => setForm({ ...form, shippingPhone: e.target.value })}
                  />
                </label>
              </div>

              <label>
                Street Address
                <input
                  required
                  placeholder="Apartment, suite, unit, building, street"
                  value={form.shippingAddress}
                  onChange={e => setForm({ ...form, shippingAddress: e.target.value })}
                />
              </label>

              <div className="form-two">
                <label>
                  City
                  <input
                    required
                    placeholder="e.g. Colombo, Kandy, Galle"
                    value={form.shippingCity}
                    onChange={e => setForm({ ...form, shippingCity: e.target.value })}
                  />
                </label>
                <label>
                  Postal Code
                  <input
                    required
                    placeholder="e.g. 00100"
                    value={form.shippingPostalCode}
                    onChange={e => setForm({ ...form, shippingPostalCode: e.target.value })}
                  />
                </label>
              </div>
            </div>

            {/* Payment Method Option */}
            <div
              style={{
                background: 'var(--bg-surface)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-lg)',
                padding: '24px 28px',
                boxShadow: 'var(--shadow-sm)',
              }}
            >
              <h3 style={{ fontSize: 18, marginBottom: 12 }}>Payment Method</h3>
              <div
                style={{
                  border: '1px solid var(--accent-primary)',
                  background: 'var(--accent-primary-subtle)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px 20px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                }}
              >
                <input type="radio" checked readOnly style={{ width: 'auto' }} />
                <div>
                  <strong style={{ fontSize: 14, color: 'var(--text-primary)' }}>Cash on Delivery (COD) / Bank Transfer</strong>
                  <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>
                    Pay in cash upon physical package delivery or via bank transfer after dispatch confirmation.
                  </div>
                </div>
              </div>
            </div>

            <button
              className="btn btn-primary"
              type="submit"
              disabled={submitting}
              style={{ padding: '16px 28px', fontSize: 16 }}
            >
              {submitting ? 'Confirming Order…' : 'Place Order & Start 3D Print'} <ArrowRight size={18} />
            </button>
          </form>

          {/* Sticky Order Summary */}
          <aside className="summary">
            <div className="eyebrow">Order Items · {items.length}</div>
            <h2 style={{ fontSize: 22, marginBottom: 16 }}>Summary</h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 16 }}>
              {items.map(item => (
                <div
                  key={item.productId}
                  style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13 }}
                >
                  <div>
                    <b>{item.name}</b>
                    <div style={{ color: 'var(--text-muted)', fontSize: 11, fontFamily: 'var(--font-mono)' }}>
                      Qty: {item.quantity} × {money(item.price)}
                    </div>
                  </div>
                  <strong style={{ fontFamily: 'var(--font-mono)' }}>{money(item.price * item.quantity)}</strong>
                </div>
              ))}
            </div>

            <div className="summary-line">
              <span>Subtotal</span>
              <b>{money(subtotal)}</b>
            </div>
            <div className="summary-line">
              <span>Delivery</span>
              <b>{deliveryFee === 0 ? <span style={{ color: 'var(--accent-emerald)' }}>FREE</span> : money(deliveryFee)}</b>
            </div>
            <div className="summary-total">
              <span>Total</span>
              <span style={{ color: 'var(--accent-primary)' }}>{money(total)}</span>
            </div>

            <Link href="/cart" className="section-note" style={{ justifyContent: 'center', display: 'flex' }}>
              <ArrowLeft size={14} /> Back to Edit Cart
            </Link>
          </aside>
        </div>
      </section>
    </main>
  )
}
