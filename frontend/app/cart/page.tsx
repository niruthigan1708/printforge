'use client'

import Link from 'next/link'
import { ArrowLeft, Minus, Plus, Trash2, ShoppingBag, ShieldCheck, ArrowRight, Truck } from 'lucide-react'
import { Header } from '../../components/Header'
import { money } from '../../lib/types'
import { useCart } from '../../lib/cart-context'
import { placeholderImage } from '../../lib/placeholder'
import { productImageUrl } from '../../lib/api'

const FREE_SHIPPING_THRESHOLD = 5000

export default function CartPage() {
  const { items, updateQuantity, removeItem, subtotal, deliveryFee, total } = useCart()

  const freeShippingProgress = Math.min(100, Math.round((subtotal / FREE_SHIPPING_THRESHOLD) * 100))
  const remainingForFreeShipping = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal)

  return (
    <main>
      <Header />

      <section className="shell page-intro compact" style={{ padding: '50px 0 30px' }}>
        <div className="eyebrow">Shopping Cart · {items.length} Unique Items</div>
        <h1 className="page-title">
          Review your<br />
          <span style={{ color: 'var(--accent-primary)' }}>selection.</span>
        </h1>
      </section>

      <section className="shell" style={{ paddingBottom: 100 }}>
        <div className="checkout-grid" style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: 50 }}>
          {/* Cart Items List */}
          <div>
            {items.length === 0 ? (
              <div className="empty" style={{ padding: '60px 40px' }}>
                <div
                  style={{
                    width: 60,
                    height: 60,
                    borderRadius: '50%',
                    background: 'var(--bg-subtle)',
                    display: 'grid',
                    placeItems: 'center',
                    margin: '0 auto 16px',
                    color: 'var(--text-muted)',
                  }}
                >
                  <ShoppingBag size={28} />
                </div>
                <h2>Your cart is currently empty.</h2>
                <p style={{ marginTop: 8 }}>Discover our collection of precision 3D printed desk & living objects.</p>
                <Link className="btn btn-primary" href="/products" style={{ marginTop: 24 }}>
                  <ArrowLeft size={16} /> Explore Catalog
                </Link>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                {/* Free Delivery Bar */}
                <div
                  style={{
                    background: 'var(--bg-surface)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: 'var(--radius-lg)',
                    padding: '20px 24px',
                    marginBottom: 24,
                    boxShadow: 'var(--shadow-sm)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13, fontWeight: 600 }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <Truck size={16} color="var(--accent-primary)" />
                      {remainingForFreeShipping > 0
                        ? `Add ${money(remainingForFreeShipping)} more to qualify for Free Delivery`
                        : '🎉 You have unlocked Free Delivery!'}
                    </span>
                    <span style={{ fontFamily: 'var(--font-mono)' }}>{freeShippingProgress}%</span>
                  </div>
                  <div className="shipping-bar-wrap">
                    <div className="shipping-bar-fill" style={{ width: `${freeShippingProgress}%` }} />
                  </div>
                </div>

                {/* Items */}
                <div
                  style={{
                    background: 'var(--bg-surface)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: 'var(--radius-lg)',
                    padding: '0 24px',
                    boxShadow: 'var(--shadow-sm)',
                  }}
                >
                  {items.map(item => (
                    <div className="cart-row" key={item.productId}>
                      <div className="cart-thumb">
                        <img
                          src={productImageUrl(item.productId)}
                          alt={item.name}
                          onError={e => {
                            e.currentTarget.onerror = null
                            e.currentTarget.src = placeholderImage(item.name, item.productId)
                          }}
                        />
                      </div>

                      <div className="cart-info">
                        <Link href={`/products/${item.productId}`} style={{ fontWeight: 700, fontSize: 16 }}>
                          {item.name}
                        </Link>
                        <span style={{ fontSize: 12, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                          {item.category} · {item.material}
                        </span>
                        <strong style={{ fontFamily: 'var(--font-mono)', fontSize: 15, color: 'var(--accent-primary)', marginTop: 4 }}>
                          {money(item.price)}
                        </strong>
                      </div>

                      <div className="quantity">
                        <button
                          aria-label="Decrease quantity"
                          onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                        >
                          <Minus size={14} />
                        </button>
                        <span>{item.quantity}</span>
                        <button
                          aria-label="Increase quantity"
                          disabled={item.quantity >= item.stockQuantity}
                          onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                        >
                          <Plus size={14} />
                        </button>
                      </div>

                      <button
                        className="icon-btn"
                        aria-label={`Remove ${item.name}`}
                        onClick={() => removeItem(item.productId)}
                        title="Remove item"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Sticky Summary Card */}
          {items.length > 0 && (
            <aside className="summary">
              <div className="eyebrow">Order Summary</div>
              <h2 style={{ fontSize: 24, marginBottom: 20 }}>Ready to fabricate.</h2>

              <div className="summary-line">
                <span style={{ color: 'var(--text-secondary)' }}>Items Subtotal</span>
                <b>{money(subtotal)}</b>
              </div>
              <div className="summary-line">
                <span style={{ color: 'var(--text-secondary)' }}>Standard Courier Delivery</span>
                <b>{deliveryFee === 0 ? <span style={{ color: 'var(--accent-emerald)' }}>FREE</span> : money(deliveryFee)}</b>
              </div>
              <div className="summary-total">
                <span>Total Amount</span>
                <span style={{ color: 'var(--accent-primary)', fontFamily: 'var(--font-mono)' }}>{money(total)}</span>
              </div>

              <Link
                className="btn btn-primary"
                href="/checkout"
                style={{ width: '100%', padding: '15px 24px', fontSize: 16 }}
              >
                Proceed to Checkout <ArrowRight size={18} />
              </Link>

              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 18, color: 'var(--text-muted)', fontSize: 12, justifyContent: 'center' }}>
                <ShieldCheck size={16} color="var(--accent-emerald)" />
                <span>Cash on Delivery / Bank Transfer available</span>
              </div>
            </aside>
          )}
        </div>
      </section>
    </main>
  )
}
