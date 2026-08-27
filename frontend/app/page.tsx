'use client'

import { useEffect, useState } from 'react'
import { ArrowUpRight } from 'lucide-react'
import { Header } from '../components/Header'
import { ProductCard } from '../components/ProductCard'
import { useCart } from '../lib/cart-context'
import { api, ApiError } from '../lib/api'
import type { Category, Product } from '../lib/types'

export default function Home() {
  const { addItem } = useCart()
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([api.get<Product[]>('/products'), api.get<Category[]>('/categories')])
      .then(([productList, categoryList]) => { setProducts(productList.slice(0, 4)); setCategories(categoryList) })
      .catch(err => setError(err instanceof ApiError ? err.message : 'Could not load PrintForge right now.'))
  }, [])

  function addToCart(product: Product) {
    addItem(product)
    setNotice(`${product.name} added to your cart`)
    setTimeout(() => setNotice(''), 2400)
  }

  return (
    <main>
      <Header />
      <section className="shell hero" id="top">
        <div>
          <div className="eyebrow">3D printed objects / Colombo</div>
          <h1>Custom ideas.<br /><span style={{ color: 'var(--orange)' }}>Physical</span> creations.</h1>
          <p className="hero-copy">Shop ready-to-print creations or turn your own 3D designs into something you can hold, use, and keep.</p>
          <div className="actions">
            <a className="btn" href="/products">Shop products <ArrowUpRight size={15} style={{ verticalAlign: 'middle' }} /></a>
            <a className="btn secondary" href="/custom-print">Start a custom print</a>
          </div>
        </div>
        <div className="hero-art"><div className="sculpture"><div className="sculpture-tip" /></div><span className="stamp">EST. 2024 / PF-001</span></div>
      </section>
      <div className="ticker"><span>●</span> Made to order in Sri Lanka <span>●</span> Small batch objects <span>●</span> Your idea, made physical <span>●</span> Made to order in Sri Lanka</div>

      <section className="shell section" id="shop">
        <div className="section-head">
          <div><div className="eyebrow">Featured / the collection</div><h2>Useful things, made better.</h2></div>
          <a className="section-note" href="/products">View all products <ArrowUpRight size={14} style={{ verticalAlign: 'middle' }} /></a>
        </div>
        {error && <div className="alert alert-error">{error}</div>}
        {!error && products.length === 0 && <div className="loading-state"><div className="spinner" />Loading products…</div>}
        {products.length > 0 && <div className="product-grid">{products.map(product => <ProductCard key={product.id} product={product} onAdd={addToCart} />)}</div>}
      </section>

      <section className="shell section">
        <div className="section-head"><div><div className="eyebrow">Browse by intention</div><h2>Find your next object.</h2></div></div>
        <div className="categories">
          {categories.slice(0, 4).map(category => (
            <a className="category" key={category.id} href={`/products?category=${encodeURIComponent(category.name)}`}>
              {category.name} <small>SHOP <ArrowUpRight size={16} /></small>
            </a>
          ))}
        </div>
      </section>

      <section className="shell section" id="how">
        <div className="section-head"><div><div className="eyebrow">From pixels to presence</div><h2>Simple by design.</h2></div></div>
        <div className="steps">
          <div className="step"><div className="step-number">01 / DISCOVER</div><h3>Choose your object</h3><p>Thoughtful, everyday forms designed in our small studio and printed in considered batches.</p></div>
          <div className="step"><div className="step-number">02 / SPECIFY</div><h3>Make it yours</h3><p>Pick a material, select a finish, or send us your own 3D model to bring to life.</p></div>
          <div className="step"><div className="step-number">03 / RECEIVE</div><h3>Hold the result</h3><p>We print, check, and dispatch your piece from Colombo. No mystery, just good making.</p></div>
        </div>
      </section>

      <section className="shell section" id="custom">
        <div className="cta">
          <div><div className="eyebrow" style={{ color: 'var(--ink)' }}>Custom printing / STL + 3MF</div><h2>Have the file?<br />We have the material.</h2></div>
          <a className="btn" href="/custom-print">Start your request <ArrowUpRight size={15} style={{ verticalAlign: 'middle' }} /></a>
        </div>
      </section>

      <footer className="shell footer"><span>© 2026 PrintForge</span><span>Custom ideas. Physical creations.</span></footer>
      {notice && <div className="toast"><span>{notice}</span></div>}
    </main>
  )
}
