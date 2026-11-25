'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { Page } from '../lib/types'

/** Previous/next controls for a server-paged list; renders nothing when everything fits on one page. */
export function Pagination({ page, onChange }: { page: Page<unknown> | null; onChange: (page: number) => void }) {
  if (!page || page.totalPages <= 1) return null
  const first = page.page * page.size + 1
  const last = Math.min(page.totalElements, first + page.content.length - 1)
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginTop: 16, flexWrap: 'wrap' }}>
      <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>
        Showing {first}–{last} of {page.totalElements}
      </span>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <button className="btn secondary" disabled={page.page === 0} onClick={() => onChange(page.page - 1)} aria-label="Previous page">
          <ChevronLeft size={16} /> Previous
        </button>
        <span style={{ fontSize: 13, fontFamily: 'var(--font-mono)' }}>
          {page.page + 1} / {page.totalPages}
        </span>
        <button className="btn secondary" disabled={page.page + 1 >= page.totalPages} onClick={() => onChange(page.page + 1)} aria-label="Next page">
          Next <ChevronRight size={16} />
        </button>
      </div>
    </div>
  )
}
