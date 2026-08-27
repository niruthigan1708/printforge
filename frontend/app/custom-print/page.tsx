'use client'
import Link from 'next/link'
import { FormEvent, useState } from 'react'
import { ArrowUpRight, Check, Upload } from 'lucide-react'
import { Header } from '../../components/Header'
import { RequireAuth } from '../../lib/guards'
import { api, ApiError } from '../../lib/api'
import type { CustomPrintRequest } from '../../lib/types'

const ALLOWED_EXTENSIONS = ['stl', '3mf', 'obj']
const MAX_SIZE_BYTES = 20 * 1024 * 1024

function CustomPrintForm() {
  const [file, setFile] = useState<File | null>(null)
  const [material, setMaterial] = useState('PLA')
  const [color, setColor] = useState('Black')
  const [quantity, setQuantity] = useState(1)
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [created, setCreated] = useState<CustomPrintRequest | null>(null)

  function handleFile(selected: File | null) {
    setError('')
    if (!selected) { setFile(null); return }
    const extension = selected.name.split('.').pop()?.toLowerCase() || ''
    if (!ALLOWED_EXTENSIONS.includes(extension)) { setError('Please choose an STL, 3MF, or OBJ file.'); setFile(null); return }
    if (selected.size > MAX_SIZE_BYTES) { setError('File exceeds the maximum allowed size of 20MB.'); setFile(null); return }
    setFile(selected)
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!file) { setError('A 3D model file is required.'); return }
    setError('')
    setSubmitting(true)
    try {
      const form = new FormData()
      form.set('file', file)
      form.set('material', material)
      form.set('color', color)
      form.set('quantity', String(quantity))
      form.set('notes', notes)
      const request = await api.postForm<CustomPrintRequest>('/custom-prints', form)
      setCreated(request)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not submit your request. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main>
      <Header />
      <section className="shell page-intro">
        <div className="eyebrow">Custom printing / STL + 3MF</div>
        <h1 className="page-title">Bring your<br /><span style={{ color: 'var(--orange)' }}>design</span> to life.</h1>
        <p className="hero-copy">Send us your model and we will review the details, recommend a material, and come back with a considered quote.</p>
      </section>
      <section className="shell form-layout">
        {created ? (
          <div className="success">
            <Check size={26} />
            <h2>Request received.</h2>
            <p>We have your file. Your request number is <b>{created.requestNumber}</b>. We will review it and be in touch with a quote.</p>
            <Link className="btn" style={{ marginTop: 16 }} href={`/custom-print/${created.id}`}>View request</Link>
          </div>
        ) : (
          <form onSubmit={submit}>
            {error && <div className="alert alert-error">{error}</div>}
            <label className="upload">
              <Upload size={24} />
              <b>{file ? file.name : 'Drop your 3D model here'}</b>
              <span>{file ? `${(file.size / 1024 / 1024).toFixed(2)} MB` : 'or choose a file · STL, 3MF, OBJ · max 20MB'}</span>
              <input type="file" accept=".stl,.3mf,.obj" onChange={event => handleFile(event.target.files?.[0] ?? null)} />
            </label>
            <div className="form-two">
              <label>Material<select value={material} onChange={e => setMaterial(e.target.value)}><option>PLA</option><option>PETG</option><option>ABS</option></select></label>
              <label>Color<select value={color} onChange={e => setColor(e.target.value)}><option>Black</option><option>White</option><option>Red</option><option>Blue</option><option>Custom</option></select></label>
            </div>
            <label>Quantity<input type="number" min={1} value={quantity} onChange={e => setQuantity(Math.max(1, Number(e.target.value)))} required /></label>
            <label>Additional instructions<textarea rows={5} placeholder="Tell us about finish, dimensions, or anything else we should know." value={notes} onChange={e => setNotes(e.target.value)} /></label>
            <button className="btn orange" type="submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit custom request'} <ArrowUpRight size={15} /></button>
          </form>
        )}
        <aside className="form-aside">
          <div className="eyebrow">What happens next</div>
          <h2>A human reviews every request.</h2>
          <ol><li>Upload your model and preferences.</li><li>We review printability and materials.</li><li>You receive a clear quote to accept.</li></ol>
        </aside>
      </section>
    </main>
  )
}

export default function CustomPrintPage() {
  return <RequireAuth><CustomPrintForm /></RequireAuth>
}
