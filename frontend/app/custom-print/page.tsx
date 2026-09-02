'use client'

import { FormEvent, useState } from 'react'
import Link from 'next/link'
import {
  UploadCloud,
  CheckCircle2,
  FileCode,
  Layers,
  ArrowRight,
  ShieldCheck,
  X,
} from 'lucide-react'
import { Header } from '../../components/Header'
import { RequireAuth } from '../../lib/guards'
import { api, ApiError } from '../../lib/api'
import type { CustomPrintRequest } from '../../lib/types'

const ALLOWED_EXTENSIONS = ['stl', '3mf', 'obj']
const MAX_SIZE_BYTES = 20 * 1024 * 1024

const MATERIALS = [
  {
    id: 'PLA',
    name: 'PLA+ Eco',
    desc: 'Crisp detail, smooth finish. Best for organizers, figurines & indoor models.',
    tag: 'Popular',
  },
  {
    id: 'PETG',
    name: 'PETG Tough',
    desc: 'Durable, impact & heat resistant up to 75°C. Best for functional parts & brackets.',
    tag: 'Durable',
  },
  {
    id: 'ABS',
    name: 'ABS Industrial',
    desc: 'High mechanical strength, temperature resistant up to 100°C. Best for tooling & enclosures.',
    tag: 'High Temp',
  },
]

const COLORS = [
  { name: 'Matte Black', hex: '#1C1C1E' },
  { name: 'Alpine White', hex: '#F2F2F7' },
  { name: 'Signal Orange', hex: '#FF5C00' },
  { name: 'Silk Silver', hex: '#8E8E93' },
  { name: 'Royal Blue', hex: '#007AFF' },
  { name: 'Cyber Green', hex: '#34C759' },
]

function CustomPrintForm() {
  const [file, setFile] = useState<File | null>(null)
  const [material, setMaterial] = useState('PLA')
  const [color, setColor] = useState('Matte Black')
  const [quantity, setQuantity] = useState(1)
  const [notes, setNotes] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [created, setCreated] = useState<CustomPrintRequest | null>(null)
  const [isDragging, setIsDragging] = useState(false)

  function handleFile(selected: File | null) {
    setError('')
    if (!selected) {
      setFile(null)
      return
    }
    const extension = selected.name.split('.').pop()?.toLowerCase() || ''
    if (!ALLOWED_EXTENSIONS.includes(extension)) {
      setError('Please choose an STL, 3MF, or OBJ file.')
      setFile(null)
      return
    }
    if (selected.size > MAX_SIZE_BYTES) {
      setError('File exceeds the maximum allowed size of 20MB.')
      setFile(null)
      return
    }
    setFile(selected)
  }

  const selectedMaterialObj = MATERIALS.find(m => m.id === material) || MATERIALS[0]

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!file) {
      setError('Please upload a 3D model file (.stl, .3mf, or .obj).')
      return
    }
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

      <section className="shell page-intro" style={{ padding: '60px 0 40px' }}>
        <div className="eyebrow">Custom 3D Printing Studio</div>
        <h1 className="page-title">
          Bring your 3D design<br />
          <span style={{ color: 'var(--accent-primary)' }}>into physical reality.</span>
        </h1>
        <p className="hero-copy">
          Upload your STL, 3MF, or OBJ file. Our engineering team reviews print orientation, slices tolerances, and provides a formal quote before precision fabrication.
        </p>
      </section>

      <section className="shell" style={{ paddingBottom: 100 }}>
        {created ? (
          <div
            className="form-card"
            style={{
              maxWidth: 720,
              margin: '0 auto',
              padding: '48px 40px',
              textAlign: 'center',
            }}
          >
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: '50%',
                background: 'var(--accent-emerald-subtle)',
                color: 'var(--accent-emerald)',
                display: 'grid',
                placeItems: 'center',
                margin: '0 auto 20px',
              }}
            >
              <CheckCircle2 size={36} />
            </div>
            <div className="eyebrow" style={{ justifyContent: 'center' }}>
              Request Received · {created.requestNumber}
            </div>
            <h2>We have received your 3D file!</h2>
            <p style={{ marginTop: 12, color: 'var(--text-secondary)' }}>
              Our lab engineers are inspecting <b>{created.fileName}</b> for slicing compatibility and printability. You will receive a quote notification in your dashboard shortly.
            </p>

            <div
              style={{
                background: 'var(--bg-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '20px',
                margin: '28px 0',
                display: 'grid',
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: 12,
                textAlign: 'left',
              }}
            >
              <div>
                <span style={{ fontSize: 11, fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>MATERIAL</span>
                <p style={{ fontWeight: 700, margin: 0 }}>{created.material}</p>
              </div>
              <div>
                <span style={{ fontSize: 11, fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>COLOR</span>
                <p style={{ fontWeight: 700, margin: 0 }}>{created.color}</p>
              </div>
              <div>
                <span style={{ fontSize: 11, fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>QUANTITY</span>
                <p style={{ fontWeight: 700, margin: 0 }}>{created.quantity} units</p>
              </div>
            </div>

            <div style={{ display: 'flex', gap: 14, justifyContent: 'center' }}>
              <Link className="btn btn-primary" href={`/custom-print/${created.id}`}>
                View Request Status <ArrowRight size={16} />
              </Link>
              <button className="btn secondary" onClick={() => { setCreated(null); setFile(null); }}>
                Submit Another Model
              </button>
            </div>
          </div>
        ) : (
          <div className="form-layout" style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: 48 }}>
            <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
              {error && <div className="alert alert-error">{error}</div>}

              {/* 1. File Upload Dropzone */}
              <div>
                <label style={{ marginBottom: 8 }}>
                  1. Upload 3D CAD File
                </label>
                {!file ? (
                  <div
                    className={`dropzone ${isDragging ? 'dragging' : ''}`}
                    onDragOver={e => { e.preventDefault(); setIsDragging(true); }}
                    onDragLeave={() => setIsDragging(false)}
                    onDrop={e => {
                      e.preventDefault()
                      setIsDragging(false)
                      handleFile(e.dataTransfer.files?.[0] ?? null)
                    }}
                  >
                    <div className="dropzone-icon">
                      <UploadCloud size={28} />
                    </div>
                    <div>
                      <strong style={{ fontSize: 16, display: 'block', color: 'var(--text-primary)' }}>
                        Drag & drop your 3D model here
                      </strong>
                      <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>
                        or click to browse from device · STL, 3MF, OBJ (max 20MB)
                      </span>
                    </div>
                    <input
                      type="file"
                      accept=".stl,.3mf,.obj"
                      onChange={event => handleFile(event.target.files?.[0] ?? null)}
                    />
                  </div>
                ) : (
                  <div
                    style={{
                      background: 'var(--bg-surface)',
                      border: '1px solid var(--accent-primary)',
                      borderRadius: 'var(--radius-lg)',
                      padding: '20px 24px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      boxShadow: 'var(--shadow-sm)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                      <div
                        style={{
                          width: 44,
                          height: 44,
                          borderRadius: 'var(--radius-md)',
                          background: 'var(--accent-primary-subtle)',
                          color: 'var(--accent-primary)',
                          display: 'grid',
                          placeItems: 'center',
                        }}
                      >
                        <FileCode size={24} />
                      </div>
                      <div>
                        <div style={{ fontWeight: 700, fontSize: 15, color: 'var(--text-primary)' }}>
                          {file.name}
                        </div>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                          {(file.size / 1024 / 1024).toFixed(2)} MB · {file.name.split('.').pop()?.toUpperCase()}
                        </div>
                      </div>
                    </div>
                    <button
                      type="button"
                      className="icon-btn"
                      onClick={() => setFile(null)}
                      title="Remove file"
                    >
                      <X size={16} />
                    </button>
                  </div>
                )}
              </div>

              {/* 2. Material Selector Cards */}
              <div>
                <label style={{ marginBottom: 10 }}>2. Select Filament Material</label>
                <div className="material-grid">
                  {MATERIALS.map(m => (
                    <div
                      key={m.id}
                      className={`material-card ${material === m.id ? 'active' : ''}`}
                      onClick={() => setMaterial(m.id)}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span className="material-name">{m.name}</span>
                        <span className="badge badge-neutral" style={{ fontSize: 9 }}>{m.tag}</span>
                      </div>
                      <p className="material-desc">{m.desc}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* 3. Color Swatches */}
              <div>
                <label style={{ marginBottom: 10 }}>3. Finish Color</label>
                <div className="swatch-group">
                  {COLORS.map(c => (
                    <button
                      key={c.name}
                      type="button"
                      className={`swatch-btn ${color === c.name ? 'active' : ''}`}
                      onClick={() => setColor(c.name)}
                    >
                      <span className="swatch-circle" style={{ background: c.hex }} />
                      <span>{c.name}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* 4. Quantity & Instructions */}
              <div className="form-two">
                <label>
                  Quantity (Pieces)
                  <input
                    type="number"
                    min={1}
                    value={quantity}
                    onChange={e => setQuantity(Math.max(1, Number(e.target.value)))}
                    required
                  />
                </label>
              </div>

              <label>
                Additional Specifications & Tolerances
                <textarea
                  rows={4}
                  placeholder="e.g. Infill density requirements, threaded inserts, critical dimensions, or specific surface orientation."
                  value={notes}
                  onChange={e => setNotes(e.target.value)}
                />
              </label>

              <button
                className="btn btn-primary"
                type="submit"
                disabled={submitting || !file}
                style={{ padding: '15px 24px', fontSize: 16 }}
              >
                {submitting ? 'Inspecting & Uploading…' : 'Submit 3D Model for Quote'} <ArrowRight size={18} />
              </button>
            </form>

            {/* Sidebar: What happens next & Guarantee */}
            <aside style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
              <div className="estimator-box">
                <div className="eyebrow" style={{ color: 'var(--accent-primary)' }}>What happens next</div>
                <h3 style={{ marginBottom: 16 }}>A real engineer reviews your model</h3>

                <div className="estimator-row">
                  <span style={{ color: 'var(--text-secondary)' }}>Selected Material</span>
                  <strong>{selectedMaterialObj.name}</strong>
                </div>
                <div className="estimator-row">
                  <span style={{ color: 'var(--text-secondary)' }}>Quantity</span>
                  <strong>{quantity} unit{quantity > 1 ? 's' : ''}</strong>
                </div>

                <div style={{ marginTop: 14, fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                  There's no automated pricing here — we don't guess at a number before actually looking at your file. Our team inspects the geometry, slices it, and sends you a firm quote you can accept or decline.
                </div>
              </div>

              <div className="estimator-box" style={{ background: 'var(--bg-subtle)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 700, marginBottom: 12 }}>
                  <ShieldCheck size={20} color="var(--accent-primary)" />
                  <span>The PrintForge Guarantee</span>
                </div>
                <ul style={{ paddingLeft: 18, color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.8 }}>
                  <li>Calibrated ±0.1mm dimensional tolerance</li>
                  <li>100% inspected before dispatch</li>
                  <li>Free reprint if any layer delamination occurs</li>
                  <li>Files are stored securely and never shared</li>
                </ul>
              </div>
            </aside>
          </div>
        )}
      </section>
    </main>
  )
}

export default function CustomPrintPage() {
  return (
    <RequireAuth>
      <CustomPrintForm />
    </RequireAuth>
  )
}
