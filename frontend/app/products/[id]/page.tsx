'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, ShoppingBag, Layers, ShieldCheck, Sparkles, Box, Clock, Truck, ChevronRight } from 'lucide-react'
import { Header } from '../../../components/Header'
import { ProductCard } from '../../../components/ProductCard'
import { useCart } from '../../../lib/cart-context'
import { api, ApiError, productImageUrl } from '../../../lib/api'
import { money, type Product } from '../../../lib/types'
import { placeholderImage } from '../../../lib/placeholder'

export default function ProductDetailPage() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const { addItem } = useCart()
  const [product, setProduct] = useState<Product | null>(null)
  const [related, setRelated] = useState<Product[]>([])
  const [quantity, setQuantity] = useState(1)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    api.get<Product>(`/products/${params.id}`)
      .then(data => {
        setProduct(data)
        setQuantity(1)
        api.get<Product[]>(`/products?category=${encodeURIComponent(data.category)}`)
          .then(list => setRelated(list.filter(item => item.id !== data.id).slice(0, 4)))
          .catch(() => {})
      })
      .catch(err => setError(err instanceof ApiError ? err.message : 'Product not found.'))
  }, [params.id])

  function addToCart() {
    if (!product) return
    addItem(product, quantity)
    setNotice(`${quantity} × ${product.name} added to cart`)
    setTimeout(() => setNotice(''), 2400)
  }

  if (error) {
    return (
      <main>
        <Header />
        <section className="shell" style={{ padding: '80px 0' }}>
          <div className="empty">
            <div className="eyebrow" style={{ justifyContent: 'center' }}>Error</div>
            <h2>{error}</h2>
            <button className="btn btn-primary" style={{ marginTop: 20 }} onClick={() => router.push('/products')}>
              <ArrowLeft size={16} /> Back to Catalog
            </button>
          </div>
        </section>
      </main>
    )
  }

  if (!product) {
    return (
      <main>
        <Header />
        <div className="shell loading-state" style={{ margin: '80px auto' }}>
          <div className="spinner" />
          <p>Loading product details…</p>
        </div>
      </main>
    )
  }

  const outOfStock = product.stockQuantity <= 0
  const isLowStock = !outOfStock && product.stockQuantity <= 5

  return (
    <main>
      <Header />

      {/* Breadcrumb Navigation */}
      <div className="shell" style={{ paddingTop: 28 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--text-muted)' }}>
          <Link href="/products" className="link-btn">Catalog</Link>
          <ChevronRight size={14} />
          <Link href={`/products?category=${encodeURIComponent(product.category)}`} className="link-btn">
            {product.category}
          </Link>
          <ChevronRight size={14} />
          <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{product.name}</span>
        </div>
      </div>

      {/* Product Main Section */}
      <section className="shell" style={{ padding: '36px 0 80px' }}>
        <div className="checkout-grid" style={{ display: 'grid', gridTemplateColumns: '1.1fr 0.9fr', gap: 60 }}>
          {/* Image Container */}
          <div
            style={{
              background: '#FFFFFF',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-xl)',
              overflow: 'hidden',
              boxShadow: 'var(--shadow-md)',
              height: 480,
              display: 'grid',
              placeItems: 'center',
              position: 'relative',
            }}
          >
            <img
              src={productImageUrl(product.id)}
              alt={product.name}
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              onError={e => {
                e.currentTarget.onerror = null
                e.currentTarget.src = placeholderImage(product.name, product.id)
              }}
            />
            {outOfStock && (
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  background: 'rgba(15, 17, 23, 0.65)',
                  backdropFilter: 'blur(6px)',
                  display: 'grid',
                  placeItems: 'center',
                  color: '#FFFFFF',
                  fontFamily: 'var(--font-mono)',
                  fontSize: 16,
                  fontWeight: 700,
                  textTransform: 'uppercase',
                  letterSpacing: '0.1em',
                }}
              >
                Out of Stock
              </div>
            )}
          </div>

          {/* Details Column */}
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <div className="eyebrow">{product.category} · In-House Design</div>
            <h1 style={{ fontSize: 'clamp(28px, 3.5vw, 42px)', margin: '8px 0 14px' }}>{product.name}</h1>
            
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 20 }}>
              <span className="price" style={{ fontSize: 26, color: 'var(--text-primary)' }}>
                {money(product.price)}
              </span>
              {outOfStock ? (
                <span className="badge badge-red">
                  <span className="badge-dot" /> Out of stock
                </span>
              ) : isLowStock ? (
                <span className="badge badge-orange">
                  <span className="badge-dot" /> Only {product.stockQuantity} remaining
                </span>
              ) : (
                <span className="badge badge-green">
                  <span className="badge-dot" /> In stock & ready
                </span>
              )}
            </div>

            <p style={{ fontSize: 16, lineHeight: 1.65, color: 'var(--text-secondary)', marginBottom: 28 }}>
              {product.description}
            </p>

            {/* Technical Specifications Grid */}
            <div className="detail-grid" style={{ margin: '0 0 28px' }}>
              <div className="detail-item">
                <span>FILAMENT MATERIAL</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Layers size={14} color="var(--accent-primary)" /> {product.material}
                </div>
              </div>
              <div className="detail-item">
                <span>COLOR FINISH</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span
                    style={{
                      width: 12,
                      height: 12,
                      borderRadius: '50%',
                      background: 'var(--accent-primary)',
                      display: 'inline-block',
                    }}
                  />
                  {product.color}
                </div>
              </div>
              <div className="detail-item">
                <span>LAYER RESOLUTION</span>
                <div style={{ fontFamily: 'var(--font-mono)' }}>0.12mm (Ultra-Fine)</div>
              </div>
              <div className="detail-item">
                <span>DISPATCH SPEED</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Truck size={14} color="var(--accent-emerald)" /> 24 - 48 Hours
                </div>
              </div>
            </div>

            {/* Quantity Stepper & Add to Cart */}
            {!outOfStock && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
                <div className="quantity">
                  <button
                    onClick={() => setQuantity(q => Math.max(1, q - 1))}
                    aria-label="Decrease quantity"
                  >
                    −
                  </button>
                  <span style={{ minWidth: 28, textAlign: 'center' }}>{quantity}</span>
                  <button
                    onClick={() => setQuantity(q => Math.min(product.stockQuantity, q + 1))}
                    aria-label="Increase quantity"
                  >
                    +
                  </button>
                </div>
                <button
                  className="btn btn-primary"
                  style={{ flex: 1, padding: '14px 28px', fontSize: 16 }}
                  onClick={addToCart}
                >
                  <ShoppingBag size={18} /> Add to Cart · {money(product.price * quantity)}
                </button>
              </div>
            )}

            {outOfStock && (
              <button className="btn secondary" disabled style={{ width: '100%', padding: '14px 28px' }}>
                Currently Out of Stock
              </button>
            )}

            {/* Studio Guarantee */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--text-muted)', fontSize: 13, marginTop: 12 }}>
              <ShieldCheck size={16} color="var(--accent-emerald)" />
              <span>Tested for layer adhesion, structural rigidity & surface finish.</span>
            </div>
          </div>
        </div>
      </section>

      {/* Related Products Carousel */}
      {related.length > 0 && (
        <section className="shell section">
          <div className="section-head">
            <div>
              <div className="eyebrow">Complementary Objects</div>
              <h2>You might also like.</h2>
            </div>
          </div>
          <div className="product-grid">
            {related.map(item => (
              <ProductCard
                key={item.id}
                product={item}
                onAdd={p => {
                  addItem(p)
                  setNotice(`${p.name} added to cart`)
                  setTimeout(() => setNotice(''), 2400)
                }}
              />
            ))}
          </div>
        </section>
      )}

      {notice && (
        <div className="toast">
          <Sparkles size={18} color="var(--accent-primary)" />
          <span>{notice}</span>
        </div>
      )}
    </main>
  )
}
