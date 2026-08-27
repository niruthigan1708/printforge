const TONES: Record<string, string> = {
  PENDING: 'badge-neutral',
  SUBMITTED: 'badge-neutral',
  CONFIRMED: 'badge-blue',
  UNDER_REVIEW: 'badge-blue',
  PRINTING: 'badge-orange',
  IN_PRODUCTION: 'badge-orange',
  READY: 'badge-lime',
  QUOTED: 'badge-lime',
  SHIPPED: 'badge-blue',
  ACCEPTED: 'badge-lime',
  DELIVERED: 'badge-green',
  COMPLETED: 'badge-green',
  CANCELLED: 'badge-red',
  REJECTED: 'badge-red',
}

export function StatusBadge({ status }: { status: string }) {
  const tone = TONES[status] || 'badge-neutral'
  return <span className={`badge ${tone}`}>{status.replace('_', ' ')}</span>
}
