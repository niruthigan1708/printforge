'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { ArrowUpRight, Sparkles, Layers, Cpu, ShieldCheck, Box, UploadCloud, ChevronRight } from 'lucide-react'
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
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    Promise.all([api.get<Product[]>('/products'), api.get<Category[]>('/categories')])
      .then(([productList, categoryList]) => {
        setProducts(productList.slice(0, 4))
        setCategories(categoryList)
      })
      .catch(err => setError(err instanceof ApiError ? err.message : 'Could not load PrintForge right now.'))
      .finally(() => setLoading(false))
  }, [])

  function addToCart(product: Product) {
    addItem(product)
    setNotice(`${product.name} added to cart`)
    setTimeout(() => setNotice(''), 2400)
  }

  return (
    <main>
      <Header />

      {/* Hero Section */}
      <section className="shell hero" id="top">
        <div>
          <div className="eyebrow">Precision 3D Printing Studio · Colombo</div>
          <h1>
            Custom ideas.<br />
            <span style={{ color: 'var(--accent-primary)' }}>Physical</span> creations.
          </h1>
          <p className="hero-copy">
            High-precision 3D printed objects for desks, devices, and creators. Choose from our curated catalog or upload your custom 3D model for instant on-demand fabrication.
          </p>
          <div className="actions">
            <Link className="btn btn-primary" href="/products">
              Explore Collection <ArrowUpRight size={16} />
            </Link>
            <Link className="btn secondary" href="/custom-print">
              <UploadCloud size={16} /> Upload 3D File
            </Link>
          </div>
        </div>

        <div className="hero-art-container">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: '#A0AAB5', letterSpacing: '0.08em' }}>
              STUDIO FORGE · RIG-01
            </span>
            <span className="badge badge-green" style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#34D399', border: '1px solid rgba(16, 185, 129, 0.3)' }}>
              <span className="badge-dot" style={{ background: '#10B981' }} /> ONLINE & READY
            </span>
          </div>

          <div className="hero-3d-visual">
            <div className="floating-model">
              <Box size={52} strokeWidth={1.5} />
            </div>
            <div style={{ marginTop: 20, textAlign: 'center' }}>
              <div style={{ fontFamily: 'var(--font-heading)', color: '#FFFFFF', fontWeight: 700, fontSize: 16 }}>
                Industrial Grade Additive Manufacturing
              </div>
              <div style={{ fontFamily: 'var(--font-mono)', color: '#8E99A8', fontSize: 12, marginTop: 4 }}>
                PLA+ · PETG · ABS · Carbon Fiber
              </div>
            </div>
          </div>

          <div className="hero-specs-pills">
            <div className="spec-pill">
              <span className="spec-pill-dot" /> 0.12mm Layer Height
            </div>
            <div className="spec-pill">
              <span className="spec-pill-dot" /> ±0.1mm Tolerance
            </div>
            <div className="spec-pill">
              <span className="spec-pill-dot" /> 48h Fast Dispatch
            </div>
          </div>
        </div>
      </section>

      {/* Infinite Marquee Ticker */}
      <div className="ticker-wrapper">
        <div className="ticker-track">
          <div className="ticker-item">
            <span className="ticker-bullet">●</span> Made to order in Sri Lanka
            <span className="ticker-bullet">●</span> 0.12mm Ultra Precision
            <span className="ticker-bullet">●</span> High-Strength PLA+ & PETG
            <span className="ticker-bullet">●</span> Custom STL / 3MF Slicing
            <span className="ticker-bullet">●</span> 48h Dispatch Guaranteed
            <span className="ticker-bullet">●</span> Zero Minimum Order Quantity
          </div>
          <div className="ticker-item">
            <span className="ticker-bullet">●</span> Made to order in Sri Lanka
            <span className="ticker-bullet">●</span> 0.12mm Ultra Precision
            <span className="ticker-bullet">●</span> High-Strength PLA+ & PETG
            <span className="ticker-bullet">●</span> Custom STL / 3MF Slicing
            <span className="ticker-bullet">●</span> 48h Dispatch Guaranteed
            <span className="ticker-bullet">●</span> Zero Minimum Order Quantity
          </div>
        </div>
      </div>

      {/* Featured Products */}
      <section className="shell section" id="shop">
        <div className="section-head">
          <div>
            <div className="eyebrow">Featured Objects</div>
            <h2>Useful things, made better.</h2>
          </div>
          <Link className="section-note" href="/products">
            View full catalog <ChevronRight size={16} />
          </Link>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {loading && (
          <div className="loading-state">
            <div className="spinner" />
            <p>Loading curated objects…</p>
          </div>
        )}

        {!loading && products.length > 0 && (
          <div className="product-grid">
            {products.map(product => (
              <ProductCard key={product.id} product={product} onAdd={addToCart} />
            ))}
          </div>
        )}
      </section>

      {/* Categories Grid */}
      <section className="shell section">
        <div className="section-head">
          <div>
            <div className="eyebrow">Browse Categories</div>
            <h2>Engineered for every space.</h2>
          </div>
        </div>

        <div className="categories">
          {categories.map((category, idx) => (
            <Link
              key={category.id}
              href={`/products?category=${encodeURIComponent(category.name)}`}
              className="category-tile"
            >
              <div>
                <div style={{ color: 'var(--accent-primary)', marginBottom: 14 }}>
                  {idx % 4 === 0 && <Box size={28} />}
                  {idx % 4 === 1 && <Layers size={28} />}
                  {idx % 4 === 2 && <Cpu size={28} />}
                  {idx % 4 === 3 && <Sparkles size={28} />}
                </div>
                <h3>{category.name}</h3>
                {category.description && (
                  <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 6 }}>
                    {category.description}
                  </p>
                )}
              </div>
              <div className="category-tile-footer">
                <span>Explore</span>
                <ArrowUpRight size={16} />
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* How It Works - 3 Step Blueprint */}
      <section className="shell section" id="how">
        <div className="section-head">
          <div>
            <div className="eyebrow">Studio Process</div>
            <h2>From digital design to physical reality.</h2>
          </div>
        </div>

        <div className="steps">
          <div className="step-card">
            <div className="step-header">
              <span className="step-badge">STEP 01</span>
              <div className="step-icon-wrap">
                <Box size={22} />
              </div>
            </div>
            <h3>Choose or Upload</h3>
            <p>Browse our in-house catalog or upload your custom 3D model (STL, 3MF, OBJ) up to 20MB.</p>
          </div>

          <div className="step-card">
            <div className="step-header">
              <span className="step-badge">STEP 02</span>
              <div className="step-icon-wrap">
                <Layers size={22} />
              </div>
            </div>
            <h3>Configure & Review</h3>
            <p>Select your preferred filament material (PLA, PETG, ABS), color finish, and print density.</p>
          </div>

          <div className="step-card">
            <div className="step-header">
              <span className="step-badge">STEP 03</span>
              <div className="step-icon-wrap">
                <ShieldCheck size={22} />
              </div>
            </div>
            <h3>Precision Print & Ship</h3>
            <p>Manufactured on calibrated industrial 3D printers, hand-inspected, and delivered safely to your door.</p>
          </div>
        </div>
      </section>

      {/* Custom Print CTA */}
      <section className="shell section" id="custom" style={{ borderBottom: 'none' }}>
        <div className="cta-banner">
          <div>
            <div className="eyebrow" style={{ color: '#FFA066' }}>Custom Manufacturing</div>
            <h2>Have a 3D model?<br />We bring it to life.</h2>
            <p style={{ color: '#A0AAB5', marginTop: 10, maxWidth: 460 }}>
              Upload your 3D CAD design for an instant review. We inspect slicing tolerances, select high-grade filaments, and print with micron-level detail.
            </p>
          </div>
          <Link className="btn btn-primary" href="/custom-print" style={{ padding: '14px 28px', fontSize: 15 }}>
            <UploadCloud size={18} /> Request Custom Print
          </Link>
        </div>
      </section>

      {/* Polished Footer */}
      <footer className="shell footer">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className="logo-mark" style={{ width: 24, height: 24 }}>
            <Box size={13} />
          </span>
          <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>PrintForge Studio</span>
          <span>· Colombo, Sri Lanka</span>
        </div>
        <div>
          <span>© {new Date().getFullYear()} PrintForge. Precision 3D Manufacturing.</span>
        </div>
      </footer>

      {notice && (
        <div className="toast">
          <Sparkles size={18} color="var(--accent-primary)" />
          <span>{notice}</span>
        </div>
      )}
    </main>
  )
}
