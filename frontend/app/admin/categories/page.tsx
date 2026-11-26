'use client'

import { FormEvent, useEffect, useState } from 'react'
import { Check, Edit, Plus, Trash2, X } from 'lucide-react'
import { api, ApiError } from '../../../lib/api'
import type { Category } from '../../../lib/types'

type Draft = { name: string; description: string }
const EMPTY: Draft = { name: '', description: '' }

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[] | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [draft, setDraft] = useState<Draft>(EMPTY)
  const [creating, setCreating] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editDraft, setEditDraft] = useState<Draft>(EMPTY)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let stale = false
    api.get<Category[]>('/categories')
      .then(list => { if (!stale) setCategories([...list].sort((a, b) => a.name.localeCompare(b.name))) })
      .catch(err => { if (!stale) setError(err instanceof ApiError ? err.message : 'Could not load categories.') })
    return () => { stale = true }
  }, [reloadToken])

  function flash(message: string) {
    setNotice(message)
    setTimeout(() => setNotice(''), 3000)
  }

  async function create(event: FormEvent) {
    event.preventDefault()
    setError('')
    setCreating(true)
    try {
      const created = await api.post<Category>('/categories', { name: draft.name.trim(), description: draft.description.trim() || null })
      setDraft(EMPTY)
      setReloadToken(t => t + 1)
      flash(`Category "${created.name}" created.`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create the category.')
    } finally {
      setCreating(false)
    }
  }

  async function save(category: Category) {
    setError('')
    setBusyId(category.id)
    try {
      await api.put<Category>(`/categories/${category.id}`, { name: editDraft.name.trim(), description: editDraft.description.trim() || null })
      setEditingId(null)
      setReloadToken(t => t + 1)
      flash('Category updated.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update the category.')
    } finally {
      setBusyId(null)
    }
  }

  async function remove(category: Category) {
    if (!confirm(`Delete the category "${category.name}"? This only works while no products use it.`)) return
    setError('')
    setBusyId(category.id)
    try {
      await api.del(`/categories/${category.id}`)
      setReloadToken(t => t + 1)
      flash(`Category "${category.name}" deleted.`)
    } catch (err) {
      // e.g. 409 "Cannot delete a category that still has products"
      setError(err instanceof ApiError ? err.message : 'Could not delete the category.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <div style={{ marginBottom: 28 }}>
        <div className="eyebrow">Studio Inventory</div>
        <h1 className="admin-title" style={{ margin: 0 }}>Categories</h1>
      </div>

      {notice && <div className="alert alert-success">{notice}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <form className="form-card category-form" onSubmit={create} style={{ maxWidth: 'none', marginBottom: 28 }}>
        <label>
          Name
          <input required maxLength={100} placeholder="e.g. Cosplay Props" value={draft.name} onChange={e => setDraft({ ...draft, name: e.target.value })} />
        </label>
        <label>
          Description
          <input maxLength={255} placeholder="Optional short description" value={draft.description} onChange={e => setDraft({ ...draft, description: e.target.value })} />
        </label>
        <button className="btn btn-primary" type="submit" disabled={creating}>
          <Plus size={16} /> {creating ? 'Adding…' : 'Add Category'}
        </button>
      </form>

      {categories === null && !error && <div className="loading-state"><div className="spinner" /><p>Loading categories…</p></div>}

      {categories && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {categories.map(category => editingId === category.id ? (
                <tr key={category.id}>
                  <td><input aria-label="Name" required maxLength={100} value={editDraft.name} onChange={e => setEditDraft({ ...editDraft, name: e.target.value })} /></td>
                  <td><input aria-label="Description" maxLength={255} value={editDraft.description} onChange={e => setEditDraft({ ...editDraft, description: e.target.value })} /></td>
                  <td>
                    <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', alignItems: 'center' }}>
                    <button className="btn btn-sm btn-primary" disabled={busyId === category.id || !editDraft.name.trim()} onClick={() => save(category)}>
                      <Check size={14} /> Save
                    </button>
                    <button className="btn btn-sm secondary" onClick={() => setEditingId(null)}>
                      <X size={14} /> Cancel
                    </button>
                    </div>
                  </td>
                </tr>
              ) : (
                <tr key={category.id}>
                  <td style={{ fontWeight: 700 }}>{category.name}</td>
                  <td style={{ color: 'var(--text-secondary)' }}>{category.description || '—'}</td>
                  <td>
                    {/* A flex row: the icon button is block-level, so nowrap alone let it wrap below Edit. */}
                    <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', alignItems: 'center' }}>
                    <button className="btn btn-sm secondary" onClick={() => { setEditingId(category.id); setEditDraft({ name: category.name, description: category.description || '' }) }}>
                      <Edit size={14} /> Edit
                    </button>
                    <button className="icon-btn" aria-label={`Delete ${category.name}`} title="Delete" disabled={busyId === category.id} onClick={() => remove(category)}>
                      <Trash2 size={16} />
                    </button>
                    </div>
                  </td>
                </tr>
              ))}
              {categories.length === 0 && (
                <tr><td colSpan={3} style={{ textAlign: 'center', padding: 36 }}>No categories yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
