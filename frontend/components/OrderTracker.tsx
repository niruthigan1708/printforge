import { ORDER_STEPS, type OrderStatus } from '../lib/types'
import { Check } from 'lucide-react'

const LABELS: Record<OrderStatus, string> = {
  PENDING: 'Order placed',
  CONFIRMED: 'Confirmed',
  PRINTING: 'Printing',
  READY: 'Ready',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
}

export function OrderTracker({ status }: { status: OrderStatus }) {
  if (status === 'CANCELLED') return <div className="tracker tracker-cancelled"><StatusPip active done /> <span>This order has been cancelled.</span></div>

  const currentIndex = ORDER_STEPS.indexOf(status)
  return (
    <ol className="tracker">
      {ORDER_STEPS.map((step, index) => (
        <li key={step} className={index <= currentIndex ? 'tracker-step done' : 'tracker-step'}>
          <span className="tracker-dot">{index <= currentIndex ? <Check size={12} /> : null}</span>
          <span className="tracker-label">{LABELS[step]}</span>
        </li>
      ))}
    </ol>
  )
}

function StatusPip({ active, done }: { active?: boolean; done?: boolean }) {
  return <span className={`tracker-dot ${done ? 'done' : ''} ${active ? 'active' : ''}`} />
}
