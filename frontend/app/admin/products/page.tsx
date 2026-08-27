'use client'
import Link from 'next/link'
import { useEffect, useState } from 'react'
import { Trash2 } from 'lucide-react'
import { api, ApiError } from '../../../lib/api'
import { money, type Product } from '../../../lib/types'

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  function load() {
    setLoading(true)
    api.get<Product[]>('/products').then(setProducts).catch(err => setError(err instanceof ApiError ? err.message : 'Could not load products.')).finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function remove(product: Product) {
    if (!confirm(`Delete "${product.name}"? This cannot be undone.`)) return
    try {
      await api.del(`/products/${product.id}`)
      setNotice(`${product.name} deleted`)
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete this product.')
    }
  }

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 26 }}>
        <h1 className="admin-title" style={{ margin: 0 }}>Products</h1>
        <Link className="btn" href="/admin/products/new">Add product</Link>
      </div>
      {notice && <div className="alert alert-success">{notice}</div>}
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? <div className="loading-state"><div className="spinner" />Loading products…</div> : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {products.map(product => (
                <tr key={product.id}>
                  <td><b>{product.name}</b></td>
                  <td>{product.category}</td>
                  <td>{money(product.price)}</td>
                  <td>{product.stockQuantity}</td>
                  <td><span className={`badge ${product.active ? 'badge-green' : 'badge-neutral'}`}>{product.active ? 'Active' : 'Inactive'}</span></td>
                  <td style={{ display: 'flex', gap: 10 }}>
                    <Link className="link-btn" href={`/admin/products/${product.id}/edit`}>Edit</Link>
                    <button className="icon-btn" aria-label={`Delete ${product.name}`} onClick={() => remove(product)}><Trash2 size={16} /></button>
                  </td>
                </tr>
              ))}
              {products.length === 0 && <tr><td colSpan={6}>No products yet.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
