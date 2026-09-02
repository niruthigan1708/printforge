'use client'

import Link from 'next/link'
import { Plus, Check, Layers } from 'lucide-react'
import { money, type Product } from '../lib/types'
import { placeholderImage } from '../lib/placeholder'
import { productImageUrl } from '../lib/api'

export function ProductCard({ product, onAdd }: { product: Product; onAdd: (product: Product) => void }) {
  const outOfStock = product.stockQuantity <= 0
  const isLowStock = !outOfStock && product.stockQuantity <= 5

  return (
    <article className="product-card">
      <Link href={`/products/${product.id}`} className="product-image" aria-label={product.name}>
        <img
          src={productImageUrl(product.id)}
          alt={product.name}
          loading="lazy"
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
              backdropFilter: 'blur(4px)',
              display: 'grid',
              placeItems: 'center',
              color: '#FFFFFF',
              fontFamily: 'var(--font-mono)',
              fontSize: 12,
              fontWeight: 700,
              textTransform: 'uppercase',
              letterSpacing: '0.08em',
            }}
          >
            Out of Stock
          </div>
        )}
      </Link>

      <div className="product-meta">
        <div className="product-category-row">
          <span className="product-category">{product.category}</span>
          <span
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              color: 'var(--text-muted)',
              display: 'flex',
              alignItems: 'center',
              gap: 4,
            }}
          >
            <Layers size={11} /> {product.material}
          </span>
        </div>

        <Link href={`/products/${product.id}`} className="product-name" title={product.name}>
          {product.name}
        </Link>

        <div style={{ margin: '6px 0 12px' }}>
          {outOfStock ? (
            <span className="badge badge-red">
              <span className="badge-dot" /> Out of stock
            </span>
          ) : isLowStock ? (
            <span className="badge badge-orange">
              <span className="badge-dot" /> Only {product.stockQuantity} left
            </span>
          ) : (
            <span className="badge badge-green">
              <span className="badge-dot" /> In stock
            </span>
          )}
        </div>

        <div className="product-bottom">
          <span className="price">{money(product.price)}</span>
          <button
            className="add-btn"
            aria-label={`Add ${product.name} to cart`}
            disabled={outOfStock}
            onClick={e => {
              e.preventDefault()
              onAdd(product)
            }}
            title={outOfStock ? 'Out of stock' : 'Add to cart'}
          >
            <Plus size={18} />
          </button>
        </div>
      </div>
    </article>
  )
}
