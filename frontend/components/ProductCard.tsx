import Link from 'next/link'
import { Plus } from 'lucide-react'
import { money, type Product } from '../lib/types'

export function ProductCard({ product, onAdd }: { product: Product; onAdd: (product: Product) => void }) {
  const outOfStock = product.stockQuantity <= 0
  return (
    <article className="product-card">
      <Link href={`/products/${product.id}`} className="product-image">
        <img src={product.imageUrl || 'https://placehold.co/600x600/e8e5df/171717?text=PrintForge'} alt={product.name} />
      </Link>
      <div className="product-meta">
        <div className="product-category">{product.category} · {product.material}</div>
        <Link href={`/products/${product.id}`} className="product-name">{product.name}</Link>
        <div className="stock-line">{outOfStock ? <span className="badge badge-red">Out of stock</span> : product.stockQuantity <= 5 ? <span className="badge badge-orange">Only {product.stockQuantity} left</span> : <span className="badge badge-green">In stock</span>}</div>
        <div className="product-bottom">
          <span className="price">{money(product.price)}</span>
          <button className="add" aria-label={`Add ${product.name} to cart`} disabled={outOfStock} onClick={() => onAdd(product)}>
            <Plus size={17} />
          </button>
        </div>
      </div>
    </article>
  )
}
