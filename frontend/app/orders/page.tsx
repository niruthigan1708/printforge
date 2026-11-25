'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { Package, ArrowRight, Layers } from 'lucide-react'
import { Header } from '../../components/Header'
import { StatusBadge } from '../../components/StatusBadge'
import { Pagination } from '../../components/Pagination'
import { RequireAuth } from '../../lib/guards'
import { api, ApiError } from '../../lib/api'
import { money, type CustomPrintRequest, type Order, type Page } from '../../lib/types'

const PAGE_SIZE = 10
const formatDate = (iso: string) => new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })

/** Loads one page of a list; loading is derived from which page last finished (no setState in the effect body). */
function usePagedList<T>(path: string) {
  const [pageNumber, setPageNumber] = useState(0)
  const [page, setPage] = useState<Page<T> | null>(null)
  const [error, setError] = useState('')
  const [finishedPage, setFinishedPage] = useState<number | null>(null)

  useEffect(() => {
    let stale = false
    const requested = pageNumber
    api.get<Page<T>>(`${path}?page=${requested}&size=${PAGE_SIZE}`)
      .then(result => { if (!stale) setPage(result) })
      .catch(err => { if (!stale) setError(err instanceof ApiError ? err.message : 'Could not load this list.') })
      .finally(() => { if (!stale) setFinishedPage(requested) })
    return () => { stale = true }
  }, [path, pageNumber])

  return { page, error, loading: finishedPage !== pageNumber, setPageNumber }
}

function EmptyState({ icon, title, text, href, cta }: { icon: React.ReactNode; title: string; text: string; href: string; cta: string }) {
  return (
    <div className="empty" style={{ padding: '48px 40px' }}>
      <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'var(--bg-subtle)', color: 'var(--text-muted)', display: 'grid', placeItems: 'center', margin: '0 auto 16px' }}>
        {icon}
      </div>
      <h2>{title}</h2>
      <p style={{ marginTop: 8 }}>{text}</p>
      <Link className="btn btn-primary" style={{ marginTop: 24 }} href={href}>{cta}</Link>
    </div>
  )
}

function Loading({ text }: { text: string }) {
  return <div className="loading-state"><div className="spinner" /><p>{text}</p></div>
}

function OrdersList() {
  const orders = usePagedList<Order>('/orders')
  const requests = usePagedList<CustomPrintRequest>('/custom-prints')

  return (
    <main>
      <Header />

      <section className="shell page-intro compact" style={{ padding: '50px 0 30px' }}>
        <div className="eyebrow">Customer Dashboard · Orders</div>
        <h1 className="page-title">
          Track what you&apos;ve<br />
          <span style={{ color: 'var(--accent-primary)' }}>ordered & printed.</span>
        </h1>
      </section>

      <section className="shell" style={{ paddingBottom: 40 }}>
        <h2 style={{ fontSize: 22, marginBottom: 16 }}>Catalogue orders</h2>
        {orders.loading && <Loading text="Loading your orders…" />}
        {!orders.loading && orders.error && <div className="alert alert-error">{orders.error}</div>}
        {!orders.loading && !orders.error && orders.page?.totalElements === 0 && (
          <EmptyState icon={<Package size={26} />} title="No orders placed yet." text="Once you order an object, you can track fabrication here." href="/products" cta="Explore Collection" />
        )}
        {!orders.loading && orders.page && orders.page.content.length > 0 && (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Order Reference</th>
                    <th>Placed Date</th>
                    <th>Items Count</th>
                    <th>Total Amount</th>
                    <th>Status</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.page.content.map(order => (
                    <tr key={order.id}>
                      <td>
                        <Link href={`/orders/${order.id}`} style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--accent-primary)' }}>
                          {order.orderNumber}
                        </Link>
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>{formatDate(order.createdAt)}</td>
                      <td>{order.items.length} item{order.items.length > 1 ? 's' : ''}</td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700 }}>{money(order.totalAmount)}</td>
                      <td><StatusBadge status={order.status} /></td>
                      <td style={{ textAlign: 'right' }}>
                        <Link className="link-btn" href={`/orders/${order.id}`} style={{ color: 'var(--accent-primary)' }}>
                          Track <ArrowRight size={14} />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={orders.page} onChange={orders.setPageNumber} />
          </>
        )}
      </section>

      <section className="shell" style={{ paddingBottom: 100 }}>
        <h2 style={{ fontSize: 22, marginBottom: 16 }}>Custom print requests</h2>
        {requests.loading && <Loading text="Loading your custom print requests…" />}
        {!requests.loading && requests.error && <div className="alert alert-error">{requests.error}</div>}
        {!requests.loading && !requests.error && requests.page?.totalElements === 0 && (
          <EmptyState icon={<Layers size={26} />} title="No custom print requests yet." text="Upload your own STL, 3MF or OBJ model and we'll quote it." href="/custom-print" cta="Request a Custom Print" />
        )}
        {!requests.loading && requests.page && requests.page.content.length > 0 && (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Request</th>
                    <th>Submitted</th>
                    <th>Model File</th>
                    <th>Quote</th>
                    <th>Status</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.page.content.map(request => (
                    <tr key={request.id}>
                      <td>
                        <Link href={`/custom-print/${request.id}`} style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--accent-primary)' }}>
                          {request.requestNumber}
                        </Link>
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>{formatDate(request.createdAt)}</td>
                      <td style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={request.fileName}>{request.fileName}</td>
                      <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700 }}>{request.adminQuote != null ? money(request.adminQuote) : '—'}</td>
                      <td><StatusBadge status={request.status} /></td>
                      <td style={{ textAlign: 'right' }}>
                        <Link className="link-btn" href={`/custom-print/${request.id}`} style={{ color: 'var(--accent-primary)' }}>
                          View <ArrowRight size={14} />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={requests.page} onChange={requests.setPageNumber} />
          </>
        )}
      </section>
    </main>
  )
}

export default function OrdersPage() {
  return (
    <RequireAuth>
      <OrdersList />
    </RequireAuth>
  )
}
