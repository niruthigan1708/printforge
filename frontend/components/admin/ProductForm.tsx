'use client'

import { FormEvent, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Upload, CheckCircle2, AlertCircle, ArrowLeft, Image as ImageIcon } from 'lucide-react'
import Link from 'next/link'
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

const EMPTY: ProductFormValues = {
  name: '',
  description: '',
  price: '',
  stockQuantity: '',
  material: 'PLA',
  color: '',
  imageUrl: '',
  categoryId: '',
  active: true,
}

export function ProductForm({ product }: { product?: Product }) {
  const router = useRouter()
  const [categories, setCategories] = useState<Category[]>([])
  const [values, setValues] = useState<ProductFormValues>(EMPTY)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [photoFile, setPhotoFile] = useState<File | null>(null)
  // The server bumps imageVersion on every upload, so this always names the current photo.
  const [imageVersion, setImageVersion] = useState<string | null>(product ? product.imageVersion : null)
  const [photoError, setPhotoError] = useState('')
  const [photoNotice, setPhotoNotice] = useState('')
  const [uploadingPhoto, setUploadingPhoto] = useState(false)
  // The stock value this form was loaded with. Sent with updates so the server can tell an
  // untouched stock field (keep live stock) from an edit made against a stale number.
  const [loadedStock, setLoadedStock] = useState<number | null>(product ? product.stockQuantity : null)
  const [stockConflict, setStockConflict] = useState(false)

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
    if (!values.categoryId) {
      setError('Please select a valid category.')
      return
    }
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
      setStockConflict(false)
      if (product) await api.put(`/products/${product.id}`, { ...body, expectedStockQuantity: loadedStock })
      else await api.post('/products', body)
      router.push('/admin/products')
    } catch (err) {
      if (product && err instanceof ApiError && err.status === 409) setStockConflict(true)
      setError(err instanceof ApiError ? err.message : 'Could not save this product.')
    } finally {
      setSubmitting(false)
    }
  }

  /** After a stock conflict: take the live stock as the new baseline and show it in the field. */
  async function reloadStock() {
    if (!product) return
    try {
      const latest = await api.get<Product>(`/admin/products/${product.id}`)
      setLoadedStock(latest.stockQuantity)
      setValues(current => ({ ...current, stockQuantity: String(latest.stockQuantity) }))
      setStockConflict(false)
      setError('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load the current stock.')
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
      const updated = await api.postForm<Product>(`/products/${product.id}/image`, form)
      setImageVersion(updated.imageVersion)
      setPhotoFile(null)
      setPhotoNotice('Product photo updated successfully!')
      setTimeout(() => setPhotoNotice(''), 3000)
    } catch (err) {
      setPhotoError(err instanceof ApiError ? err.message : 'Could not upload this photo.')
    } finally {
      setUploadingPhoto(false)
    }
  }

  return (
    <div style={{ maxWidth: 760 }}>
      <div style={{ marginBottom: 20 }}>
        <Link href="/admin/products" className="link-btn">
          <ArrowLeft size={14} /> Back to Products
        </Link>
      </div>

      {product && (
        <div className="form-card" style={{ marginBottom: 28 }}>
          <h3 style={{ fontSize: 18, marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
            <ImageIcon size={18} color="var(--accent-primary)" />
            Product Photo Upload
          </h3>

          <div style={{ display: 'flex', gap: 24, alignItems: 'center', flexWrap: 'wrap' }}>
            <div
              style={{
                width: 100,
                height: 100,
                borderRadius: 'var(--radius-md)',
                overflow: 'hidden',
                border: '1px solid var(--border-medium)',
                background: '#F3F4F6',
                flex: 'none',
              }}
            >
              <img
                src={productImageUrl(product.id, imageVersion)}
                alt={product.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            </div>

            <div style={{ flex: 1, minWidth: 240 }}>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                onChange={e => setPhotoFile(e.target.files?.[0] ?? null)}
              />
              <button
                type="button"
                className="btn btn-sm btn-primary"
                style={{ marginTop: 12 }}
                disabled={!photoFile || uploadingPhoto}
                onClick={uploadPhoto}
              >
                <Upload size={14} /> {uploadingPhoto ? 'Uploading Photo…' : 'Upload New Photo'}
              </button>
            </div>
          </div>

          {photoError && <div className="alert alert-error" style={{ marginTop: 14 }}>{photoError}</div>}
          {photoNotice && <div className="alert alert-success" style={{ marginTop: 14 }}>{photoNotice}</div>}
        </div>
      )}

      <form className="form-card" onSubmit={submit}>
        <h3 style={{ fontSize: 18, marginBottom: 20 }}>
          {product ? 'Edit Product Details' : 'Create New 3D Printed Object'}
        </h3>

        {error && (
          <div className="alert alert-error">
            {error}
            {stockConflict && (
              <button type="button" className="link-btn" onClick={reloadStock} style={{ marginLeft: 8 }}>
                Load current stock
              </button>
            )}
          </div>
        )}

        <label>
          Object Name
          <input
            required
            placeholder="e.g. Hexagonal Cable Organizer"
            maxLength={150}
            value={values.name}
            onChange={e => setValues({ ...values, name: e.target.value })}
          />
        </label>

        <label>
          Description & Usage
          <textarea
            required
            rows={4}
            placeholder="Explain functional dimensions, intended purpose, and design details."
            maxLength={2000}
            value={values.description}
            onChange={e => setValues({ ...values, description: e.target.value })}
          />
        </label>

        <div className="form-grid-2">
          <label>
            Price (Rs.)
            <input
              required
              type="number"
              min="0.01"
              step="0.01"
              placeholder="1500.00"
              value={values.price}
              onChange={e => setValues({ ...values, price: e.target.value })}
            />
          </label>
          <label>
            Available Stock Quantity
            <input
              required
              type="number"
              min="0"
              step="1"
              placeholder="10"
              value={values.stockQuantity}
              onChange={e => setValues({ ...values, stockQuantity: e.target.value })}
            />
          </label>
        </div>

        <div className="form-grid-2">
          <label>
            Filament Material
            <select
              value={values.material}
              onChange={e => setValues({ ...values, material: e.target.value })}
            >
              <option value="PLA">PLA (Eco Plastic)</option>
              <option value="PETG">PETG (Tough & Heat Resistant)</option>
              <option value="ABS">ABS (Engineering Grade)</option>
            </select>
          </label>
          <label>
            Color Name
            <input
              required
              placeholder="e.g. Matte Black, Silk Gold"
              maxLength={50}
              value={values.color}
              onChange={e => setValues({ ...values, color: e.target.value })}
            />
          </label>
        </div>

        <label>
          Category
          <select
            required
            value={values.categoryId}
            onChange={e => setValues({ ...values, categoryId: e.target.value })}
          >
            <option value="">Select a category</option>
            {categories.map(category => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Fallback Image URL (Optional)
          <input
            maxLength={2048}
            value={values.imageUrl}
            onChange={e => setValues({ ...values, imageUrl: e.target.value })}
            placeholder="https://… (used if no local photo is uploaded)"
          />
        </label>

        <label style={{ flexDirection: 'row', alignItems: 'center', gap: 10, cursor: 'pointer', marginTop: 6 }}>
          <input
            type="checkbox"
            checked={values.active}
            onChange={e => setValues({ ...values, active: e.target.checked })}
            style={{ width: 'auto' }}
          />
          <span>Active (Make visible to customers in public catalog)</span>
        </label>

        <button
          className="btn btn-primary"
          type="submit"
          disabled={submitting}
          style={{ marginTop: 12, padding: '14px 24px' }}
        >
          {submitting ? 'Saving Changes…' : product ? 'Save Product Changes' : 'Create & Publish Object'}
        </button>
      </form>
    </div>
  )
}
