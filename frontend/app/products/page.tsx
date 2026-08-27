'use client'

import { Suspense, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { Header } from '../../components/Header'
import { ProductCard } from '../../components/ProductCard'
import { useCart } from '../../lib/cart-context'
import { api, ApiError } from '../../lib/api'
import type { Category, Product } from '../../lib/types'

function ProductsPageInner() {
  const searchParams = useSearchParams()
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
    setNotice(`${product.name} added to your cart`)
    setTimeout(() => setNotice(''), 2400)
  }

  return (
    <main>
      <Header />
      <section className="shell page-intro">
        <div className="eyebrow">The collection / {products.length} objects</div>
        <h1 className="page-title">Objects with<br /><span style={{ color: 'var(--orange)' }}>purpose.</span></h1>
        <p className="hero-copy">Small-batch 3D printed goods for desks, devices, and the spaces around them.</p>
      </section>
      <section className="shell section">
        <div className="filters">
          <input aria-label="Search products" placeholder="Search the collection" value={query} onChange={event => setQuery(event.target.value)} />
          <select aria-label="Filter by category" value={category} onChange={event => setCategory(event.target.value)}>
            <option value="All">All categories</option>
            {categories.map(item => <option key={item.id} value={item.name}>{item.name}</option>)}
          </select>
          <select aria-label="Sort products" value={sort} onChange={event => setSort(event.target.value)}>
            <option value="newest">Newest</option>
            <option value="price-asc">Price: low to high</option>
            <option value="price-desc">Price: high to low</option>
            <option value="name">Name: A to Z</option>
          </select>
        </div>

        {loading && <div className="loading-state"><div className="spinner" />Loading products…</div>}
        {!loading && error && <div className="alert alert-error">{error}</div>}
        {!loading && !error && sorted.length === 0 && <div className="empty"><h2>Nothing found.</h2><p>Try a different search or category.</p></div>}
        {!loading && !error && sorted.length > 0 && <div className="product-grid">{sorted.map(product => <ProductCard key={product.id} product={product} onAdd={addToCart} />)}</div>}
      </section>
      {notice && <div className="toast">{notice}</div>}
    </main>
  )
}

export default function ProductsPage() {
  return <Suspense fallback={<main><Header /><div className="shell loading-state"><div className="spinner" />Loading…</div></main>}><ProductsPageInner /></Suspense>
}
