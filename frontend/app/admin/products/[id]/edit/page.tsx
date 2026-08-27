'use client'
import { useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import { ProductForm } from '../../../../../components/admin/ProductForm'
import { api, ApiError } from '../../../../../lib/api'
import type { Product } from '../../../../../lib/types'

export default function EditProductPage() {
  const params = useParams<{ id: string }>()
  const [product, setProduct] = useState<Product | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<Product>(`/products/${params.id}`).then(setProduct).catch(err => setError(err instanceof ApiError ? err.message : 'Product not found.'))
  }, [params.id])

  return (
    <>
      <h1 className="admin-title">Edit product</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {!error && !product && <div className="loading-state"><div className="spinner" />Loading…</div>}
      {product && <ProductForm product={product} />}
    </>
  )
}
