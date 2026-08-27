'use client'
import Link from 'next/link'
import { ArrowLeft, Check } from 'lucide-react'
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
  const [form, setForm] = useState({ shippingName: '', shippingPhone: '', shippingAddress: '', shippingCity: '', shippingPostalCode: '' })

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
        <section className="shell success-page">
          <Check size={32} />
          <div className="eyebrow">Order confirmed / {order.orderNumber}</div>
          <h1 className="page-title">It is on<br /><span style={{ color: 'var(--orange)' }}>its way.</span></h1>
          <p className="hero-copy">Order placed successfully! We will send a confirmation when your pieces move into production.</p>
          <Link className="btn" href={`/orders/${order.id}`}>View order</Link>
        </section>
      </main>
    )
  }

  if (!loading && items.length === 0) {
    return (
      <main>
        <Header />
        <section className="shell page-intro"><div className="empty"><h2>Your cart is empty.</h2><p>Add something to the cart before checking out.</p><Link className="btn" style={{ marginTop: 16 }} href="/products">Browse products</Link></div></section>
      </main>
    )
  }

  return (
    <main>
      <Header />
      <section className="shell page-intro compact">
        <div className="eyebrow">Checkout / cash on delivery</div>
        <h1 className="page-title">Nearly<br /><span style={{ color: 'var(--orange)' }}>there.</span></h1>
      </section>
      <section className="shell checkout-grid">
        <form onSubmit={submit}>
          {error && <div className="alert alert-error">{error}</div>}
          <div className="form-two">
            <label>Full name<input required placeholder="Your name" value={form.shippingName} onChange={e => setForm({ ...form, shippingName: e.target.value })} /></label>
            <label>Phone<input required placeholder="07X XXX XXXX" value={form.shippingPhone} onChange={e => setForm({ ...form, shippingPhone: e.target.value })} /></label>
          </div>
          <label>Address<input required placeholder="Street address" value={form.shippingAddress} onChange={e => setForm({ ...form, shippingAddress: e.target.value })} /></label>
          <div className="form-two">
            <label>City<input required placeholder="Colombo" value={form.shippingCity} onChange={e => setForm({ ...form, shippingCity: e.target.value })} /></label>
            <label>Postal code<input required placeholder="00100" value={form.shippingPostalCode} onChange={e => setForm({ ...form, shippingPostalCode: e.target.value })} /></label>
          </div>
          <button className="btn orange" type="submit" disabled={submitting}>{submitting ? 'Placing order…' : 'Place order'}</button>
        </form>
        <aside className="summary">
          <div className="eyebrow">Order summary</div>
          <h2>{items.length} pieces</h2>
          {items.map(item => <div className="summary-line" key={item.productId}><span>{item.name} × {item.quantity}</span><b>{money(item.price * item.quantity)}</b></div>)}
          <div className="summary-line"><span>Subtotal</span><b>{money(subtotal)}</b></div>
          <div className="summary-line"><span>Delivery</span><b>{money(deliveryFee)}</b></div>
          <div className="summary-total"><span>Total</span><b>{money(total)}</b></div>
          <Link href="/cart" className="section-note"><ArrowLeft size={14} /> Edit cart</Link>
        </aside>
      </section>
    </main>
  )
}
