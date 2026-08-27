'use client'
import Link from 'next/link'
import { ArrowLeft, Minus, Plus, X } from 'lucide-react'
import { Header } from '../../components/Header'
import { money } from '../../lib/types'
import { useCart } from '../../lib/cart-context'

export default function CartPage() {
  const { items, updateQuantity, removeItem, subtotal, deliveryFee, total } = useCart()

  return (
    <main>
      <Header />
      <section className="shell page-intro compact">
        <div className="eyebrow">Your selection / {items.length} pieces</div>
        <h1 className="page-title">The good<br /><span style={{ color: 'var(--orange)' }}>stuff.</span></h1>
      </section>
      <section className="shell checkout-grid">
        <div>
          {items.length === 0 ? (
            <div className="empty">
              <h2>Your cart is empty.</h2>
              <p>There is always room for one more useful object.</p>
              <Link className="btn" href="/products">Browse products <ArrowLeft size={15} /></Link>
            </div>
          ) : items.map(item => (
            <div className="cart-row" key={item.productId}>
              <div className="cart-thumb"><img src={item.imageUrl || 'https://placehold.co/200x200/e8e5df/171717?text=PF'} alt="" /></div>
              <div className="cart-info">
                <Link href={`/products/${item.productId}`}><b>{item.name}</b></Link>
                <span>{item.category} · {item.material}</span>
                <strong>{money(item.price)}</strong>
              </div>
              <div className="quantity">
                <button aria-label="Decrease quantity" onClick={() => updateQuantity(item.productId, item.quantity - 1)}><Minus size={14} /></button>
                <span>{item.quantity}</span>
                <button aria-label="Increase quantity" disabled={item.quantity >= item.stockQuantity} onClick={() => updateQuantity(item.productId, item.quantity + 1)}><Plus size={14} /></button>
              </div>
              <button className="icon-btn" aria-label={`Remove ${item.name}`} onClick={() => removeItem(item.productId)}><X size={16} /></button>
            </div>
          ))}
        </div>
        {items.length > 0 && (
          <aside className="summary">
            <div className="eyebrow">Order summary</div>
            <h2>Ready when you are.</h2>
            <div className="summary-line"><span>Subtotal</span><b>{money(subtotal)}</b></div>
            <div className="summary-line"><span>Delivery</span><b>{money(deliveryFee)}</b></div>
            <div className="summary-total"><span>Total</span><b>{money(total)}</b></div>
            <Link className="btn orange" href="/checkout">Proceed to checkout</Link>
            <p className="fine-print">Demo payment / Cash on delivery</p>
          </aside>
        )}
      </section>
    </main>
  )
}
