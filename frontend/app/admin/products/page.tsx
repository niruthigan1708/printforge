'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { Trash2, Edit, Plus, Box } from 'lucide-react'
import { api, ApiError, productImageUrl } from '../../../lib/api'
import { money, type Page, type Product } from '../../../lib/types'
import { Pagination } from '../../../components/Pagination'
import { placeholderImage } from '../../../lib/placeholder'

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const [pageNumber, setPageNumber] = useState(0)
  const [pageInfo, setPageInfo] = useState<Page<Product> | null>(null)

  // Bumped after a delete to refetch the current page.
  const [reloadToken, setReloadToken] = useState(0)
  // Loading is derived from which request last finished, so the effect never sets it synchronously.
  const requestKey = `${pageNumber}:${reloadToken}`
  const [finishedKey, setFinishedKey] = useState<string | null>(null)
  const loading = finishedKey !== requestKey

  useEffect(() => {
    let stale = false
    const key = `${pageNumber}:${reloadToken}`
    api.get<Page<Product>>(`/admin/products?page=${pageNumber}&size=20`)
      .then(result => {
        if (stale) return
        // Deleting the last item on the last page leaves it empty; step back a page.
        if (result.content.length === 0 && result.page > 0) { setPageNumber(result.page - 1); return }
        setPageInfo(result)
        setProducts(result.content)
        setFinishedKey(key)
      })
      .catch(err => {
        if (stale) return
        setError(err instanceof ApiError ? err.message : 'Could not load products.')
        setFinishedKey(key)
      })
    return () => { stale = true }
  }, [pageNumber, reloadToken])

  async function remove(product: Product) {
    if (!confirm(`Delete "${product.name}"? If it appears in past orders it will be archived (hidden from the shop) instead.`)) return
    try {
      const result = await api.del<{ archived: boolean; message: string }>(`/products/${product.id}`)
      setNotice(result?.archived
        ? `"${product.name}" has existing orders, so it was archived and hidden from the shop.`
        : `"${product.name}" was successfully deleted.`)
      setTimeout(() => setNotice(''), 3000)
      setReloadToken(token => token + 1)
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
                        src={productImageUrl(product.id, product.imageVersion)}
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
                    No products added yet. Click &quot;Add New Object&quot; to get started.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
      <Pagination page={pageInfo} onChange={setPageNumber} />
    </>
  )
}
