const TONES: Record<string, string> = {
  PENDING: 'badge-neutral',
  SUBMITTED: 'badge-neutral',
  CONFIRMED: 'badge-blue',
  UNDER_REVIEW: 'badge-blue',
  PRINTING: 'badge-orange',
  IN_PRODUCTION: 'badge-orange',
  READY: 'badge-green',
  QUOTED: 'badge-blue',
  SHIPPED: 'badge-blue',
  ACCEPTED: 'badge-green',
  DELIVERED: 'badge-green',
  COMPLETED: 'badge-green',
  CANCELLED: 'badge-red',
  REJECTED: 'badge-red',
}

export function StatusBadge({ status }: { status: string }) {
  const tone = TONES[status] || 'badge-neutral'
  const label = status.replace(/_/g, ' ')
  return (
    <span className={`badge ${tone}`}>
      <span className="badge-dot" />
      {label}
    </span>
  )
}
