'use client'

import { FormEvent, useEffect, useState } from 'react'
import { useParams } from 'next/navigation'
import Link from 'next/link'
import { AlertTriangle, ArrowLeft, PackageCheck, Send, ShieldCheck, Truck } from 'lucide-react'
import { StatusBadge } from '../../../../components/StatusBadge'
import { OrderHistory } from '../../../../components/OrderHistory'
import { api, ApiError } from '../../../../lib/api'
import { money, type Order, type OrderStatus } from '../../../../lib/types'

const dateTime = (value: string) => new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export default function AdminOrderDetailPage() {
  const params = useParams<{ id: string }>()
  const [order, setOrder] = useState<Order | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [nextStatus, setNextStatus] = useState<OrderStatus | ''>('')
  const [trackingNumber, setTrackingNumber] = useState('')
  const [note, setNote] = useState('')
  const [resolution, setResolution] = useState('')

  useEffect(() => {
    api.get<Order>(`/admin/orders/${params.id}`)
      .then(setOrder)
      .catch(err => setError(err instanceof ApiError ? err.message : 'Order not found.'))
  }, [params.id])

  function done(updated: Order, message: string) {
    setOrder(updated)
    setNotice(message)
    setTimeout(() => setNotice(''), 3000)
  }

  async function updateStatus(event: FormEvent) {
    event.preventDefault()
    if (!order || !nextStatus) return
    const prompt = nextStatus === 'CANCELLED'
      ? `Cancel order ${order.orderNumber}? Its items will be returned to stock. This cannot be undone.`
      : `Move order ${order.orderNumber} to ${nextStatus}? Orders cannot be moved back to an earlier status.`
    if (!confirm(prompt)) return
    setBusy(true)
    setError('')
    try {
      const updated = await api.put<Order>(`/admin/orders/${order.id}/status`, {
        status: nextStatus,
        trackingNumber: nextStatus === 'SHIPPED' ? trackingNumber : undefined,
        note,
      })
      setNextStatus('')
      setTrackingNumber('')
      setNote('')
      done(updated, `Order ${updated.orderNumber} updated to ${updated.status}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update this order status.')
    } finally {
      setBusy(false)
    }
  }

  async function resolveIssue(event: FormEvent) {
    event.preventDefault()
    if (!order) return
    setBusy(true)
    setError('')
    try {
      const updated = await api.put<Order>(`/admin/orders/${order.id}/issue/resolve`, { resolution })
      setResolution('')
      done(updated, 'Delivery problem marked as resolved')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not resolve this problem.')
    } finally {
      setBusy(false)
    }
  }

  if (error && !order) return <div className="alert alert-error">{error}</div>
  if (!order) {
    return (
      <div className="loading-state">
        <div className="spinner" />
        <p>Loading order…</p>
      </div>
    )
  }

  const shipping = nextStatus === 'SHIPPED'

  return (
    <div style={{ maxWidth: 880 }}>
      <div style={{ marginBottom: 20 }}>
        <Link href="/admin/orders" className="link-btn">
          <ArrowLeft size={14} /> Back to Orders
        </Link>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div className="eyebrow">Order · placed {dateTime(order.createdAt)}</div>
          <h1 className="admin-title" style={{ margin: 0 }}>{order.orderNumber}</h1>
        </div>
        <StatusBadge status={order.status} />
      </div>

      {notice && <div className="alert alert-success">{notice}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Open delivery problem: the most urgent thing on this page. */}
      {order.issue?.open && (
        <div className="form-card" style={{ marginBottom: 28, maxWidth: 'none', borderColor: 'var(--accent-amber)' }}>
          <h3 style={{ fontSize: 18, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
            <AlertTriangle size={18} color="var(--accent-amber)" />
            Customer reports this order did not arrive
          </h3>
          <p style={{ fontSize: 14, color: 'var(--text-secondary)', marginBottom: 6 }}>
            Reported {dateTime(order.issue.reportedAt)}. Marked delivered {order.deliveredAt ? dateTime(order.deliveredAt) : '—'}
            {order.trackingNumber && <> with tracking number <b className="tracking-number">{order.trackingNumber}</b></>}.
          </p>
          {order.issue.note && <p style={{ fontSize: 14, marginBottom: 16 }}>&ldquo;{order.issue.note}&rdquo;</p>}
          <form onSubmit={resolveIssue} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <label>
              Resolution (shown to the customer)
              <textarea required rows={3} maxLength={2000} value={resolution} onChange={e => setResolution(e.target.value)}
                placeholder="e.g. The courier located the parcel and re-delivered it on Friday." />
            </label>
            <button className="btn btn-primary" type="submit" disabled={busy || !resolution.trim()} style={{ width: 'max-content' }}>
              <ShieldCheck size={15} /> {busy ? 'Saving…' : 'Mark as resolved'}
            </button>
          </form>
        </div>
      )}

      {/* Status */}
      <div className="form-card" style={{ marginBottom: 28, maxWidth: 'none' }}>
        <h3 style={{ fontSize: 18, marginBottom: 14 }}>Update Status</h3>
        {order.trackingNumber && (
          <div className="delivery-row" style={{ marginBottom: 14 }}>
            <Truck size={16} color="var(--accent-primary)" /> Tracking number <b className="tracking-number">{order.trackingNumber}</b>
          </div>
        )}
        {order.status === 'DELIVERED' && (
          <p className="delivery-note" style={{ marginTop: 0, marginBottom: 14 }}>
            <PackageCheck size={15} />
            {order.receipt === 'CONFIRMED' && order.receivedAt && <>The customer confirmed receipt on {dateTime(order.receivedAt)}.</>}
            {order.receipt === 'AUTO' && <>Counted as received: no problem reported within 14 days of delivery.</>}
            {order.receipt === 'AWAITING' && order.receiptDeadline && <>Waiting for the customer. They can report a problem until {dateTime(order.receiptDeadline)}.</>}
          </p>
        )}
        {order.nextStatuses.length === 0 ? (
          <p style={{ fontSize: 14, color: 'var(--text-secondary)', margin: 0 }}>This order is {order.status} and can no longer change.</p>
        ) : (
          <form onSubmit={updateStatus} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="form-grid-2">
              <label>
                New status
                <select className="status-select" required value={nextStatus} onChange={e => setNextStatus(e.target.value as OrderStatus)}>
                  <option value="" disabled>Choose…</option>
                  {order.nextStatuses.map(status => <option key={status} value={status}>{status}</option>)}
                </select>
              </label>
              {shipping && (
                <label>
                  Tracking number
                  <input required maxLength={100} value={trackingNumber} onChange={e => setTrackingNumber(e.target.value)} placeholder="e.g. LK123456789" />
                </label>
              )}
            </div>
            {order.status !== 'SHIPPED' && (
              <p style={{ fontSize: 13, color: 'var(--text-muted)', margin: 0 }}>
                An order can only be marked delivered after it has been shipped with a tracking number.
              </p>
            )}
            <label>
              Note (optional, shown to the customer in the order history)
              <textarea rows={2} maxLength={2000} value={note} onChange={e => setNote(e.target.value)} />
            </label>
            <button className="btn btn-primary" type="submit" disabled={busy || !nextStatus || (shipping && !trackingNumber.trim())} style={{ width: 'max-content' }}>
              <Send size={15} /> {busy ? 'Saving…' : 'Update status'}
            </button>
          </form>
        )}
      </div>

      {/* Customer and items */}
      <div className="form-card" style={{ marginBottom: 28, maxWidth: 'none' }}>
        <h3 style={{ fontSize: 18, marginBottom: 16 }}>Customer & Delivery</h3>
        <div className="detail-grid" style={{ margin: '0 0 20px' }}>
          <div className="detail-item">
            <span>CUSTOMER</span>
            <b>{order.customerName}</b>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{order.customerEmail}</div>
          </div>
          <div className="detail-item">
            <span>SHIP TO</span>
            <b>{order.shippingName} · {order.shippingPhone}</b>
            <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{order.shippingAddress}, {order.shippingCity} {order.shippingPostalCode}</div>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Item</th><th>Qty</th><th>Unit Price</th><th style={{ textAlign: 'right' }}>Subtotal</th></tr>
            </thead>
            <tbody>
              {order.items.map((item, index) => (
                <tr key={index}>
                  <td><b>{item.name}</b></td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{item.quantity}</td>
                  <td style={{ fontFamily: 'var(--font-mono)' }}>{money(item.unitPrice)}</td>
                  <td style={{ fontFamily: 'var(--font-mono)', textAlign: 'right' }}>{money(item.subtotal)}</td>
                </tr>
              ))}
              <tr>
                <td colSpan={3}>Delivery fee</td>
                <td style={{ fontFamily: 'var(--font-mono)', textAlign: 'right' }}>{money(order.deliveryFee)}</td>
              </tr>
              <tr>
                <td colSpan={3}><b>Total</b></td>
                <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, textAlign: 'right' }}>{money(order.totalAmount)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* History, with the staff member behind each change */}
      <div className="form-card" style={{ maxWidth: 'none' }}>
        <h3 style={{ fontSize: 18, marginBottom: 18 }}>History</h3>
        <OrderHistory events={order.history} showEmails />
      </div>
    </div>
  )
}
