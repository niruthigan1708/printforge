'use client'

import { Suspense, useEffect, useMemo, useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { Search, Sparkles, X, SlidersHorizontal } from 'lucide-react'
import { Header } from '../../components/Header'
import { ProductCard } from '../../components/ProductCard'
import { useCart } from '../../lib/cart-context'
import { api, ApiError } from '../../lib/api'
import type { Category, Product } from '../../lib/types'

function ProductsPageInner() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const { addItem } = useCart()
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [query, setQuery] = useState(searchParams.get('search') || '')
  const [category, setCategory] = useState(searchParams.get('category') || 'All')
  const [sort, setSort] = useState('newest')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    api.get<Category[]>('/categories').then(setCategories).catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    setError('')
    const params = new URLSearchParams()
    if (query) params.set('search', query)
    else if (category !== 'All') params.set('category', category)
    
    api.get<Product[]>(`/products${params.toString() ? `?${params}` : ''}`)
      .then(setProducts)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Could not load products right now.'))
      .finally(() => setLoading(false))
  }, [query, category])

  const sorted = useMemo(() => {
    const list = [...products]
    if (sort === 'price-asc') list.sort((a, b) => a.price - b.price)
    if (sort === 'price-desc') list.sort((a, b) => b.price - a.price)
    if (sort === 'name') list.sort((a, b) => a.name.localeCompare(b.name))
    return list
  }, [products, sort])

  function addToCart(product: Product) {
    addItem(product)
    setNotice(`${product.name} added to cart`)
    setTimeout(() => setNotice(''), 2400)
  }

  function resetFilters() {
    setQuery('')
    setCategory('All')
    setSort('newest')
  }

  return (
    <main>
      <Header />

      <section className="shell page-intro" style={{ padding: '60px 0 36px' }}>
        <div className="eyebrow">Studio Catalog · {products.length} Objects</div>
        <h1 className="page-title">
          Functional objects with<br />
          <span style={{ color: 'var(--accent-primary)' }}>purpose & precision.</span>
        </h1>
        <p className="hero-copy">
          Small-batch 3D printed goods engineered for desks, devices, workbenches, and living spaces.
        </p>
      </section>

      <section className="shell" style={{ paddingBottom: 100 }}>
        {/* Filters and Search Bar */}
        <div className="filters-bar">
          <div className="search-input-wrap">
            <Search size={18} />
            <input
              aria-label="Search catalog"
              placeholder="Search by object name, material, or color…"
              value={query}
              onChange={e => setQuery(e.target.value)}
            />
            {query && (
              <button
                style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}
                onClick={() => setQuery('')}
              >
                <X size={16} />
              </button>
            )}
          </div>

          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', flex: 1 }}>
            <button
              className={`swatch-btn ${category === 'All' ? 'active' : ''}`}
              onClick={() => setCategory('All')}
            >
              All
            </button>
            {categories.map(item => (
              <button
                key={item.id}
                className={`swatch-btn ${category === item.name ? 'active' : ''}`}
                onClick={() => setCategory(item.name)}
              >
                {item.name}
              </button>
            ))}
          </div>

          <div style={{ minWidth: 160 }}>
            <select
              aria-label="Sort products"
              value={sort}
              onChange={e => setSort(e.target.value)}
              className="filter-select"
            >
              <option value="newest">Sort: Newest</option>
              <option value="price-asc">Price: Low to High</option>
              <option value="price-desc">Price: High to Low</option>
              <option value="name">Name: A to Z</option>
            </select>
          </div>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="product-grid">
            {[1, 2, 3, 4, 5, 6, 7, 8].map(i => (
              <div key={i} className="product-card" style={{ opacity: 0.6 }}>
                <div className="product-image" style={{ background: 'var(--bg-subtle)' }}>
                  <div className="spinner" />
                </div>
                <div className="product-meta">
                  <div style={{ height: 12, background: 'var(--bg-subtle)', borderRadius: 4, width: '40%', marginBottom: 12 }} />
                  <div style={{ height: 18, background: 'var(--bg-subtle)', borderRadius: 4, width: '80%', marginBottom: 16 }} />
                  <div style={{ height: 16, background: 'var(--bg-subtle)', borderRadius: 4, width: '30%', marginTop: 'auto' }} />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Error State */}
        {!loading && error && <div className="alert alert-error">{error}</div>}

        {/* Empty State */}
        {!loading && !error && sorted.length === 0 && (
          <div className="empty">
            <div className="eyebrow" style={{ justifyContent: 'center' }}>No Matches</div>
            <h2>No objects matched your criteria.</h2>
            <p style={{ marginTop: 8 }}>Try adjusting your search terms or selecting a different category filter.</p>
            <button className="btn secondary" style={{ marginTop: 20 }} onClick={resetFilters}>
              Reset all filters
            </button>
          </div>
        )}

        {/* Product Grid */}
        {!loading && !error && sorted.length > 0 && (
          <div className="product-grid">
            {sorted.map(product => (
              <ProductCard key={product.id} product={product} onAdd={addToCart} />
            ))}
          </div>
        )}
      </section>

      {notice && (
        <div className="toast">
          <Sparkles size={18} color="var(--accent-primary)" />
          <span>{notice}</span>
        </div>
      )}
    </main>
  )
}

export default function ProductsPage() {
  return (
    <Suspense
      fallback={
        <main>
          <Header />
          <div className="shell loading-state" style={{ margin: '60px auto' }}>
            <div className="spinner" />
            <p>Loading catalog…</p>
          </div>
        </main>
      }
    >
      <ProductsPageInner />
    </Suspense>
  )
}
