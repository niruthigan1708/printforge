'use client'
import { FormEvent, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api, ApiError, productImageUrl } from '../../lib/api'
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
  const [photoFile, setPhotoFile] = useState<File | null>(null)
  const [photoVersion, setPhotoVersion] = useState(0)
  const [photoError, setPhotoError] = useState('')
  const [photoNotice, setPhotoNotice] = useState('')
  const [uploadingPhoto, setUploadingPhoto] = useState(false)

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

  async function uploadPhoto() {
    if (!product || !photoFile) return
    setUploadingPhoto(true)
    setPhotoError('')
    setPhotoNotice('')
    try {
      const form = new FormData()
      form.set('file', photoFile)
      await api.postForm(`/products/${product.id}/image`, form)
      setPhotoVersion(v => v + 1)
      setPhotoFile(null)
      setPhotoNotice('Photo updated')
      setTimeout(() => setPhotoNotice(''), 2500)
    } catch (err) {
      setPhotoError(err instanceof ApiError ? err.message : 'Could not upload this photo.')
    } finally {
      setUploadingPhoto(false)
    }
  }

  return (
    <>
      {product && (
        <div className="form-card" style={{ marginBottom: 20 }}>
          <label>Product photo</label>
          <div style={{ display: 'flex', gap: 16, alignItems: 'center', marginTop: 8 }}>
            <img
              src={`${productImageUrl(product.id)}${photoVersion ? `?v=${photoVersion}` : ''}`}
              alt={product.name}
              style={{ width: 90, height: 90, objectFit: 'cover', border: '1px solid var(--line)', flex: 'none' }}
            />
            <div style={{ flex: 1 }}>
              <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={e => setPhotoFile(e.target.files?.[0] ?? null)} />
              <button type="button" className="btn btn-sm" style={{ marginTop: 10 }} disabled={!photoFile || uploadingPhoto} onClick={uploadPhoto}>
                {uploadingPhoto ? 'Uploading…' : 'Upload photo'}
              </button>
            </div>
          </div>
          {photoError && <div className="alert alert-error" style={{ marginTop: 12 }}>{photoError}</div>}
          {photoNotice && <div className="alert alert-success" style={{ marginTop: 12 }}>{photoNotice}</div>}
        </div>
      )}
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
      <label>Fallback image URL<input value={values.imageUrl} onChange={e => setValues({ ...values, imageUrl: e.target.value })} placeholder="https://… (used only if no photo is uploaded)" /></label>
      <label className="checkbox-row"><input type="checkbox" checked={values.active} onChange={e => setValues({ ...values, active: e.target.checked })} /> Active (visible to customers)</label>
      <button className="btn orange" type="submit" disabled={submitting}>{submitting ? 'Saving…' : product ? 'Save changes' : 'Create product'}</button>
      </form>
    </>
  )
}
