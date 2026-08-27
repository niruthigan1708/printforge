'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { Header } from '../../../components/Header'
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
    setNotice(`${product.name} added to your cart`)
    setTimeout(() => setNotice(''), 2400)
  }

  if (error) return <main><Header /><section className="shell page-intro"><div className="empty"><h2>{error}</h2><button className="btn" style={{ marginTop: 16 }} onClick={() => router.push('/products')}>Back to products</button></div></section></main>
  if (!product) return <main><Header /><div className="shell loading-state"><div className="spinner" />Loading…</div></main>

  const outOfStock = product.stockQuantity <= 0

  return (
    <main>
      <Header />
      <section className="shell section" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 50, paddingTop: 50 }}>
        <div className="product-image" style={{ height: 460 }}>
          <img
            src={productImageUrl(product.id)}
            alt={product.name}
            style={{ width: '100%', height: '100%', objectFit: 'cover', mixBlendMode: 'multiply' }}
            onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(product.name, product.id) }}
          />
        </div>
        <div>
          <div className="eyebrow">{product.category}</div>
          <h1 style={{ fontSize: 40, margin: '14px 0' }}>{product.name}</h1>
          <div className="price" style={{ fontSize: 22, marginBottom: 16 }}>{money(product.price)}</div>
          <p className="hero-copy">{product.description}</p>
          <div className="detail-grid">
            <div className="detail-item"><span>Material</span>{product.material}</div>
            <div className="detail-item"><span>Color</span>{product.color}</div>
            <div className="detail-item"><span>Stock</span>{outOfStock ? 'Out of stock' : `${product.stockQuantity} available`}</div>
          </div>
          {!outOfStock && (
            <div className="quantity" style={{ marginBottom: 20 }}>
              <button onClick={() => setQuantity(q => Math.max(1, q - 1))} aria-label="Decrease quantity">−</button>
              <span>{quantity}</span>
              <button onClick={() => setQuantity(q => Math.min(product.stockQuantity, q + 1))} aria-label="Increase quantity">+</button>
            </div>
          )}
          <button className="btn orange" disabled={outOfStock} onClick={addToCart}>{outOfStock ? 'Out of stock' : 'Add to cart'}</button>
        </div>
      </section>

      {related.length > 0 && (
        <section className="shell section">
          <div className="section-head"><div><h2>You might also like</h2></div></div>
          <div className="product-grid">
            {related.map(item => (
              <a className="product-card" key={item.id} href={`/products/${item.id}`}>
                <div className="product-image">
                  <img
                    src={productImageUrl(item.id)}
                    alt={item.name}
                    onError={e => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(item.name, item.id) }}
                  />
                </div>
                <div className="product-meta"><div className="product-category">{item.category}</div><div className="product-name">{item.name}</div><div className="price">{money(item.price)}</div></div>
              </a>
            ))}
          </div>
        </section>
      )}
      {notice && <div className="toast">{notice}</div>}
    </main>
  )
}
