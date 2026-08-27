'use client'
import { useState } from 'react'
import { ArrowUpRight, Plus } from 'lucide-react'
import { Header } from '../../components/Header'
import { products, money } from '../../lib/types'
import { useCart } from '../../lib/store'

export default function ProductsPage() {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('All')
  const { add } = useCart()
  const categories = ['All', ...Array.from(new Set(products.map(product => product.category)))]
  const filtered = products.filter(product => product.name.toLowerCase().includes(query.toLowerCase()) && (category === 'All' || product.category === category))
  return <main><Header/><section className="shell page-intro"><div className="eyebrow">The collection / {products.length} objects</div><h1 className="page-title">Objects with<br/><span style={{color:'var(--orange)'}}>purpose.</span></h1><p className="hero-copy">Small-batch 3D printed goods for desks, devices, and the spaces around them.</p></section><section className="shell section"><div className="filters"><input aria-label="Search products" placeholder="Search the collection" value={query} onChange={event => setQuery(event.target.value)}/><select aria-label="Filter by category" value={category} onChange={event => setCategory(event.target.value)}>{categories.map(item => <option key={item}>{item}</option>)}</select></div>{filtered.length ? <div className="product-grid">{filtered.map(product => <article className="product-card" key={product.id}><div className="product-image" style={{background:product.tone}}><img src={product.image} alt={product.name}/></div><div className="product-meta"><div className="product-category">{product.category}</div><div className="product-name">{product.name}</div><div className="product-bottom"><span className="price">{money(product.price)}</span><button className="add" aria-label={`Add ${product.name}`} onClick={() => add(product)}><Plus size={17}/></button></div></div></article>)}</div> : <div className="empty"><h2>Nothing found.</h2><p>Try a different search or category.</p></div>}</section></main>
}
