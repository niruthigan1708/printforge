'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { Trash2, Edit, Plus, Box } from 'lucide-react'
import { api, ApiError, productImageUrl } from '../../../lib/api'
import { money, type Product } from '../../../lib/types'
import { placeholderImage } from '../../../lib/placeholder'

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  function load() {
    setLoading(true)
    api.get<Product[]>('/admin/products')
      .then(setProducts)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Could not load products.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function remove(product: Product) {
    if (!confirm(`Are you sure you want to delete "${product.name}"? This action cannot be undone.`)) return
    try {
      await api.del(`/products/${product.id}`)
      setNotice(`"${product.name}" was successfully deleted.`)
      setTimeout(() => setNotice(''), 3000)
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete this product.')
    }
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 28 }}>
        <div>
          <div className="eyebrow">Studio Inventory</div>
          <h1 className="admin-title" style={{ margin: 0 }}>Product Catalog</h1>
        </div>
        <Link className="btn btn-primary" href="/admin/products/new">
          <Plus size={16} /> Add New Object
        </Link>
      </div>

      {notice && <div className="alert alert-success">{notice}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {loading ? (
        <div className="loading-state">
          <div className="spinner" />
          <p>Loading inventory…</p>
        </div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th style={{ width: 60 }}>Photo</th>
                <th>Product Name</th>
                <th>Category</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Visibility</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {products.map(product => (
                <tr key={product.id}>
                  <td>
                    <div
                      style={{
                        width: 44,
                        height: 44,
                        borderRadius: 'var(--radius-sm)',
                        overflow: 'hidden',
                        background: '#F3F4F6',
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
                    </div>
                  </td>
                  <td>
                    <b>{product.name}</b>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                      {product.material} · {product.color}
                    </div>
                  </td>
                  <td>{product.category}</td>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700 }}>{money(product.price)}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{product.stockQuantity} units</td>
                  <td>
                    <span className={`badge ${product.active ? 'badge-green' : 'badge-neutral'}`}>
                      <span className="badge-dot" />
                      {product.active ? 'Active' : 'Hidden'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                      <Link
                        className="btn btn-sm secondary"
                        href={`/admin/products/${product.id}/edit`}
                        title="Edit product"
                      >
                        <Edit size={14} /> Edit
                      </Link>
                      <button
                        className="icon-btn"
                        aria-label={`Delete ${product.name}`}
                        onClick={() => remove(product)}
                        title="Delete product"
                      >
                        <Trash2 size={16} color="var(--accent-red)" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {products.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '36px' }}>
                    No products added yet. Click "Add New Object" to get started.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
