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
}

export type AuthResponse = { token: string; id: number; name: string; email: string; role: Role }

export type OrderItem = { name: string; quantity: number; unitPrice: number; subtotal: number }

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
}

export type DashboardSummary = {
  totalProducts: number
  totalOrders: number
  pendingOrders: number
  customRequests: number
  revenue: number
  recentOrders: Order[]
  recentCustomRequests: CustomPrintRequest[]
}

export const ORDER_STEPS: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PRINTING', 'READY', 'SHIPPED', 'DELIVERED']

export const money = (value: number) => `Rs. ${Number(value ?? 0).toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
