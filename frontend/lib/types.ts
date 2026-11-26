export type Role = 'CUSTOMER' | 'ADMIN'

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PRINTING' | 'READY' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export type RequestStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'QUOTED' | 'ACCEPTED' | 'REJECTED' | 'IN_PRODUCTION' | 'COMPLETED' | 'CANCELLED'

export type Category = { id: number; name: string; description: string | null }

export type Product = {
  id: number
  name: string
  description: string
  price: number
  stockQuantity: number
  material: string
  color: string
  imageUrl: string | null
  category: string
  active: boolean
  /** Changes whenever the photo changes; pass to productImageUrl for cache-safe URLs. */
  imageVersion: string
  /** Bumped by every admin save or archive; sent back on update to detect concurrent edits. */
  editVersion: number
}

/** The signed-in user. No token: the session is an httpOnly cookie page scripts cannot read. */
export type AuthResponse = { id: number; name: string; email: string; role: Role }

export type OrderItem = { name: string; quantity: number; unitPrice: number; subtotal: number }

export type OrderIssue = { reportedAt: string; note: string | null; resolvedAt: string | null; resolution: string | null; open: boolean }

export type OrderEventType = 'PLACED' | 'STATUS_CHANGED' | 'RECEIVED' | 'ISSUE_REPORTED' | 'ISSUE_RESOLVED'

/** One history entry. For customers, staff appear as "PrintForge studio" with no email. */
export type OrderEvent = {
  type: OrderEventType
  status: OrderStatus | null
  actorName: string | null
  actorEmail: string | null
  actorRole: Role | null
  note: string | null
  createdAt: string
}

export type Order = {
  id: number
  orderNumber: string
  subtotal: number
  deliveryFee: number
  totalAmount: number
  status: OrderStatus
  shippingName: string
  shippingPhone: string
  shippingAddress: string
  shippingCity: string
  shippingPostalCode: string
  createdAt: string
  updatedAt: string
  customerName: string
  customerEmail: string
  items: OrderItem[]
  /** Statuses the server will accept as the next move for this order. */
  nextStatuses: OrderStatus[]
  /** True while the customer may still cancel this order themselves. */
  canCancel: boolean
  /** Required before an order can be delivered; shown to the customer. */
  trackingNumber: string | null
  deliveredAt: string | null
  receivedAt: string | null
  /** For DELIVERED orders: confirmed by the customer, counted as received after 14 days, or still awaiting. */
  receipt: 'CONFIRMED' | 'AUTO' | 'AWAITING' | null
  /** Last moment the customer can report that the order never arrived. */
  receiptDeadline: string | null
  canConfirmReceipt: boolean
  canReportIssue: boolean
  issue: OrderIssue | null
  /** Every recorded change, oldest first. */
  history: OrderEvent[]
}

export type CustomPrintRequest = {
  id: number
  requestNumber: string
  fileName: string
  fileType: string
  material: string
  color: string
  quantity: number
  notes: string | null
  status: RequestStatus
  adminQuote: number | null
  adminNotes: string | null
  createdAt: string
  updatedAt: string
  customerName: string
  customerEmail: string
  /** Statuses an admin may set directly via the status endpoint (never QUOTED/ACCEPTED/REJECTED). */
  nextStatuses: RequestStatus[]
  /** Whether a quote can still be sent or revised (only until the customer answers). */
  canQuote: boolean
  /** True while the customer may still withdraw this request themselves. */
  canCancel: boolean
  /** False once the model file of this closed request has been deleted by the retention job. */
  fileAvailable: boolean
}

/** One page of a server-paged list (admin orders, products, custom prints). `page` is zero-based. */
export type Page<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number }

export type DashboardSummary = {
  totalProducts: number
  totalOrders: number
  pendingOrders: number
  /** Delivered orders the customer reported as not received, not yet resolved. */
  deliveryIssues: number
  customRequests: number
  revenue: number
  recentOrders: Order[]
  recentCustomRequests: CustomPrintRequest[]
}

export const ORDER_STEPS: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PRINTING', 'READY', 'SHIPPED', 'DELIVERED']

export const money = (value: number) => `Rs. ${Number(value ?? 0).toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
