import { AlertTriangle, CheckCircle2, Circle, PackageCheck, ShieldCheck } from 'lucide-react'
import type { OrderEvent } from '../lib/types'
import { ORDER_STATUS_LABELS } from './OrderTracker'

function describe(event: OrderEvent): string {
  switch (event.type) {
    case 'PLACED': return 'Order placed'
    case 'RECEIVED': return 'Customer confirmed the order arrived'
    case 'ISSUE_REPORTED': return 'Customer reported the order did not arrive'
    case 'ISSUE_RESOLVED': return 'Delivery problem resolved'
    default: return event.status ? `Status changed to ${ORDER_STATUS_LABELS[event.status]}` : 'Status changed'
  }
}

const ICONS = {
  PLACED: Circle,
  STATUS_CHANGED: CheckCircle2,
  RECEIVED: PackageCheck,
  ISSUE_REPORTED: AlertTriangle,
  ISSUE_RESOLVED: ShieldCheck,
} as const

/** Every recorded change to an order, oldest first. `showEmails` is for the admin view. */
export function OrderHistory({ events, showEmails = false }: { events: OrderEvent[]; showEmails?: boolean }) {
  if (events.length === 0) return <p className="history-empty">No history recorded for this order.</p>
  return (
    <ol className="order-history">
      {events.map((event, index) => {
        const Icon = ICONS[event.type]
        return (
          <li key={index} className={`history-item history-${event.type.toLowerCase().replace('_', '-')}`}>
            <span className="history-icon"><Icon size={14} /></span>
            <div className="history-body">
              <div className="history-title">{describe(event)}</div>
              <div className="history-meta">
                {new Date(event.createdAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}
                {' · '}
                {event.actorName ?? 'Not recorded'}
                {showEmails && event.actorEmail && event.actorEmail !== event.actorName && <> ({event.actorEmail})</>}
                {showEmails && event.actorRole && <span className="history-role">{event.actorRole === 'ADMIN' ? 'Staff' : 'Customer'}</span>}
              </div>
              {event.note && <p className="history-note">{event.note}</p>}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
