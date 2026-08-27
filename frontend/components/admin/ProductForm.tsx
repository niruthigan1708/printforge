'use client'
import { FormEvent, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api, ApiError } from '../../lib/api'
import type { Category, Product } from '../../lib/types'

type ProductFormValues = {
  name: string
  description: string
  price: string
  stockQuantity: string
  material: string
  color: string
  imageUrl: string
  categoryId: string
  active: boolean
}

const EMPTY: ProductFormValues = { name: '', description: '', price: '', stockQuantity: '', material: 'PLA', color: '', imageUrl: '', categoryId: '', active: true }

export function ProductForm({ product }: { product?: Product }) {
  const router = useRouter()
  const [categories, setCategories] = useState<Category[]>([])
  const [values, setValues] = useState<ProductFormValues>(EMPTY)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    api.get<Category[]>('/categories').then(setCategories).catch(() => {})
  }, [])

  useEffect(() => {
    if (!product) return
    api.get<Category[]>('/categories').then(list => {
      const match = list.find(c => c.name === product.category)
      setValues({
        name: product.name,
        description: product.description,
        price: String(product.price),
        stockQuantity: String(product.stockQuantity),
        material: product.material,
        color: product.color,
        imageUrl: product.imageUrl || '',
        categoryId: match ? String(match.id) : '',
        active: product.active,
      })
    })
  }, [product])

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    if (!values.categoryId) { setError('Please choose a category.'); return }
    setSubmitting(true)
    try {
      const body = {
        name: values.name,
        description: values.description,
        price: Number(values.price),
        stockQuantity: Number(values.stockQuantity),
        material: values.material,
        color: values.color,
        imageUrl: values.imageUrl,
        categoryId: Number(values.categoryId),
        active: values.active,
      }
      if (product) await api.put(`/products/${product.id}`, body)
      else await api.post('/products', body)
      router.push('/admin/products')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save this product.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="form-card" onSubmit={submit}>
      {error && <div className="alert alert-error">{error}</div>}
      <label>Name<input required value={values.name} onChange={e => setValues({ ...values, name: e.target.value })} /></label>
      <label>Description<textarea required rows={4} value={values.description} onChange={e => setValues({ ...values, description: e.target.value })} /></label>
      <div className="form-grid-2">
        <label>Price (Rs.)<input required type="number" min="0.01" step="0.01" value={values.price} onChange={e => setValues({ ...values, price: e.target.value })} /></label>
        <label>Stock quantity<input required type="number" min="0" step="1" value={values.stockQuantity} onChange={e => setValues({ ...values, stockQuantity: e.target.value })} /></label>
      </div>
      <div className="form-grid-2">
        <label>Material<select value={values.material} onChange={e => setValues({ ...values, material: e.target.value })}><option>PLA</option><option>PETG</option><option>ABS</option></select></label>
        <label>Color<input required value={values.color} onChange={e => setValues({ ...values, color: e.target.value })} /></label>
      </div>
      <label>Category
        <select required value={values.categoryId} onChange={e => setValues({ ...values, categoryId: e.target.value })}>
          <option value="">Choose a category</option>
          {categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
      </label>
      <label>Image URL<input value={values.imageUrl} onChange={e => setValues({ ...values, imageUrl: e.target.value })} placeholder="https://…" /></label>
      <label className="checkbox-row"><input type="checkbox" checked={values.active} onChange={e => setValues({ ...values, active: e.target.checked })} /> Active (visible to customers)</label>
      <button className="btn orange" type="submit" disabled={submitting}>{submitting ? 'Saving…' : product ? 'Save changes' : 'Create product'}</button>
    </form>
  )
}
