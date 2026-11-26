'use client'

import { FormEvent, useState } from 'react'
import { AlertTriangle, PackageCheck, Truck } from 'lucide-react'
import { api, ApiError } from '../lib/api'
import type { Order } from '../lib/types'

const dateTime = (value: string) => new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
const date = (value: string) => new Date(value).toLocaleDateString(undefined, { dateStyle: 'medium' })

/**
 * The customer's side of delivery: the tracking number, confirming the order arrived, and
 * reporting that it did not. What is offered comes from the server's canConfirmReceipt /
 * canReportIssue flags, so the page never shows an action the API would refuse.
 */
export function DeliveryPanel({ order, onChange }: { order: Order; onChange: (order: Order) => void }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [reporting, setReporting] = useState(false)
  const [note, setNote] = useState('')

  async function run(action: () => Promise<Order>) {
    setBusy(true)
    setError('')
    try {
      onChange(await action())
      setReporting(false)
      setNote('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  function confirmReceipt() {
    if (!confirm('Confirm that you received this order? You will no longer be able to report a delivery problem online.')) return
    run(() => api.put<Order>(`/orders/${order.id}/received`))
  }

  function report(event: FormEvent) {
    event.preventDefault()
    run(() => api.put<Order>(`/orders/${order.id}/not-received`, { note }))
  }

  if (!order.trackingNumber && order.status !== 'DELIVERED') return null

  return (
    <div className="delivery-panel">
      {order.trackingNumber && (
        <div className="delivery-row">
          <Truck size={16} color="var(--accent-primary)" />
          <span>Tracking number</span>
          <b className="tracking-number">{order.trackingNumber}</b>
        </div>
      )}

      {order.issue && (
        <div className={`alert alert-stack ${order.issue.open ? 'alert-warning' : 'alert-success'}`} style={{ margin: '14px 0 0' }}>
          <div style={{ fontWeight: 600 }}>
            {order.issue.open
              ? `You reported this order as not received on ${dateTime(order.issue.reportedAt)}. The studio is looking into it.`
              : `Your delivery problem was resolved on ${dateTime(order.issue.resolvedAt!)}.`}
          </div>
          {order.issue.note && <div style={{ marginTop: 6 }}>Your note: {order.issue.note}</div>}
          {!order.issue.open && order.issue.resolution && <div style={{ marginTop: 6 }}>Studio: {order.issue.resolution}</div>}
        </div>
      )}

      {order.receipt === 'CONFIRMED' && order.receivedAt && (
        <p className="delivery-note"><PackageCheck size={15} /> You confirmed this order arrived on {dateTime(order.receivedAt)}.</p>
      )}
      {order.receipt === 'AUTO' && (
        <p className="delivery-note"><PackageCheck size={15} /> Counted as received: no problem was reported within 14 days of delivery.</p>
      )}

      {(order.canConfirmReceipt || order.canReportIssue) && (
        <div style={{ marginTop: 16 }}>
          <p className="delivery-note" style={{ marginTop: 0 }}>
            Marked as delivered on {dateTime(order.deliveredAt!)}. Did it arrive?
            {order.canReportIssue && order.receiptDeadline && <> You can report a problem until {date(order.receiptDeadline)}.</>}
          </p>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            {order.canConfirmReceipt && (
              <button className="btn btn-primary" disabled={busy} onClick={confirmReceipt}>
                <PackageCheck size={16} /> Yes, I received it
              </button>
            )}
            {order.canReportIssue && !reporting && (
              <button className="btn secondary" disabled={busy} onClick={() => setReporting(true)}>
                <AlertTriangle size={16} /> I haven&apos;t received it
              </button>
            )}
          </div>
          {reporting && (
            <form onSubmit={report} className="report-form">
              <label>
                What happened? (optional)
                <textarea rows={3} maxLength={2000} value={note} onChange={e => setNote(e.target.value)}
                  placeholder="e.g. Nothing was delivered and the courier did not call." />
              </label>
              <div style={{ display: 'flex', gap: 12 }}>
                <button className="btn btn-primary" type="submit" disabled={busy}>{busy ? 'Sending…' : 'Report problem'}</button>
                <button className="btn secondary" type="button" disabled={busy} onClick={() => setReporting(false)}>Back</button>
              </div>
            </form>
          )}
        </div>
      )}
      {error && <div className="alert alert-error" style={{ marginTop: 14 }}>{error}</div>}
    </div>
  )
}
