import { ORDER_STEPS, type OrderStatus } from '../lib/types'
import { Check, XCircle, Clock, CheckCircle2, Cog, Package, Truck, Home } from 'lucide-react'

const STEP_ICONS: Record<OrderStatus, typeof Clock> = {
  PENDING: Clock,
  CONFIRMED: CheckCircle2,
  PRINTING: Cog,
  READY: Package,
  SHIPPED: Truck,
  DELIVERED: Home,
  CANCELLED: XCircle,
}

const LABELS: Record<OrderStatus, string> = {
  PENDING: 'Placed',
  CONFIRMED: 'Confirmed',
  PRINTING: 'Printing',
  READY: 'Quality QC',
  SHIPPED: 'Dispatched',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
}

export function OrderTracker({ status }: { status: OrderStatus }) {
  if (status === 'CANCELLED') {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '16px 20px',
          background: 'var(--accent-red-subtle)',
          border: '1px solid rgba(239, 68, 68, 0.2)',
          borderRadius: 'var(--radius-md)',
          color: 'var(--accent-red-text)',
          fontWeight: 600,
          margin: '24px 0',
        }}
      >
        <XCircle size={20} color="var(--accent-red)" />
        <span>This order has been cancelled and is no longer active.</span>
      </div>
    )
  }

  const currentIndex = ORDER_STEPS.indexOf(status)

  return (
    <div style={{ overflowX: 'auto', padding: '10px 0' }}>
      <ol className="tracker">
        {ORDER_STEPS.map((step, index) => {
          const isDone = index < currentIndex
          const isCurrent = index === currentIndex
          const Icon = STEP_ICONS[step] || Clock

          return (
            <li
              key={step}
              className={`tracker-step ${isDone ? 'done' : ''} ${isCurrent ? 'active done' : ''}`}
            >
              <span className="tracker-dot">
                {isDone ? (
                  <Check size={14} />
                ) : (
                  <Icon size={14} />
                )}
              </span>
              <span className="tracker-label">{LABELS[step]}</span>
            </li>
          )
        })}
      </ol>
    </div>
  )
}
