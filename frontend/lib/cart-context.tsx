'use client'

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { api } from './api'
import type { Product } from './types'

const CART_KEY = 'printforge-cart'

export type CartItem = {
  productId: number
  name: string
  price: number
  imageUrl: string | null
  imageVersion?: string
  material: string
  color: string
  category: string
  stockQuantity: number
  quantity: number
  /** Set by refresh() when the product was archived or removed; excluded from totals and checkout. */
  unavailable?: boolean
}

/** What refresh() changed, so pages can tell the customer instead of changing totals silently. */
export type CartRefreshResult = { priceChanges: string[]; unavailable: string[]; quantityReduced: string[] }

type CartContextValue = {
  items: CartItem[]
  addItem: (product: Product, quantity?: number) => void
  updateQuantity: (productId: number, quantity: number) => void
  removeItem: (productId: number) => void
  clear: () => void
  /** Re-reads prices, stock and availability from the server. */
  refresh: () => Promise<CartRefreshResult>
  itemCount: number
  subtotal: number
  /** Null until the server's delivery fee has loaded (it is never hardcoded here). */
  deliveryFee: number | null
  total: number | null
  /** Subtotal for free delivery, or null when unknown or switched off. */
  freeDeliveryThreshold: number | null
  hasUnavailable: boolean
}

type ShopSettings = { deliveryFee: number; freeDeliveryThreshold: number | null }

const EMPTY_REFRESH: CartRefreshResult = { priceChanges: [], unavailable: [], quantityReduced: [] }

const CartContext = createContext<CartContextValue>({
  items: [],
  addItem: () => {},
  updateQuantity: () => {},
  removeItem: () => {},
  clear: () => {},
  refresh: async () => EMPTY_REFRESH,
  itemCount: 0,
  subtotal: 0,
  deliveryFee: null,
  total: null,
  freeDeliveryThreshold: null,
  hasUnavailable: false,
})

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([])
  const [hydrated, setHydrated] = useState(false)
  const [settings, setSettings] = useState<ShopSettings | null>(null)
  const itemsRef = useRef(items)
  itemsRef.current = items

  useEffect(() => {
    try {
      const saved = localStorage.getItem(CART_KEY)
      if (saved) setItems(JSON.parse(saved))
    } catch {
      // ignore corrupted local storage
    }
    setHydrated(true)
    api.get<ShopSettings>('/shop/settings')
      .then(s => setSettings({ deliveryFee: Number(s.deliveryFee), freeDeliveryThreshold: s.freeDeliveryThreshold == null ? null : Number(s.freeDeliveryThreshold) }))
      .catch(() => {})
  }, [])

  useEffect(() => {
    if (hydrated) localStorage.setItem(CART_KEY, JSON.stringify(items))
  }, [items, hydrated])

  const addItem = (product: Product, quantity = 1) => {
    setItems(current => {
      const existing = current.find(item => item.productId === product.id)
      if (existing) {
        const nextQuantity = Math.min(existing.quantity + quantity, product.stockQuantity)
        return current.map(item => (item.productId === product.id
          ? { ...item, quantity: nextQuantity, price: product.price, stockQuantity: product.stockQuantity, imageVersion: product.imageVersion, unavailable: false }
          : item))
      }
      const cappedQuantity = Math.max(1, Math.min(quantity, product.stockQuantity))
      return [...current, {
        productId: product.id,
        name: product.name,
        price: product.price,
        imageUrl: product.imageUrl,
        imageVersion: product.imageVersion,
        material: product.material,
        color: product.color,
        category: product.category,
        stockQuantity: product.stockQuantity,
        quantity: cappedQuantity,
      }]
    })
  }

  const updateQuantity = (productId: number, quantity: number) => {
    setItems(current => current
      .map(item => (item.productId === productId ? { ...item, quantity: Math.min(Math.max(quantity, 1), item.stockQuantity) } : item))
      .filter(item => item.quantity > 0))
  }

  const removeItem = (productId: number) => setItems(current => current.filter(item => item.productId !== productId))
  const clear = () => setItems([])

  const refresh = useCallback(async (): Promise<CartRefreshResult> => {
    // Child effects run before this provider loads the saved cart; until then there is nothing
    // to refresh. Because hydrated is a dependency, callers get a new refresh once it has loaded.
    if (!hydrated) return EMPTY_REFRESH
    const snapshot = itemsRef.current
    if (snapshot.length === 0) return EMPTY_REFRESH
    const ids = snapshot.map(item => item.productId).join(',')
    const latest = await api.get<Product[]>(`/products?ids=${ids}`)
    const byId = new Map(latest.map(product => [product.id, product]))

    const result: CartRefreshResult = { priceChanges: [], unavailable: [], quantityReduced: [] }
    for (const item of snapshot) {
      const product = byId.get(item.productId)
      if (!product || product.stockQuantity === 0) { if (!item.unavailable) result.unavailable.push(item.name); continue }
      if (Number(product.price) !== Number(item.price)) result.priceChanges.push(product.name)
      if (item.quantity > product.stockQuantity) result.quantityReduced.push(product.name)
    }

    // Merge into the current state, so quantity edits made while the request was in flight survive.
    setItems(current => current.map(item => {
      const product = byId.get(item.productId)
      if (!product || product.stockQuantity === 0) return { ...item, unavailable: true }
      return {
        ...item,
        name: product.name,
        price: product.price,
        imageUrl: product.imageUrl,
        imageVersion: product.imageVersion,
        material: product.material,
        color: product.color,
        category: product.category,
        stockQuantity: product.stockQuantity,
        quantity: Math.min(item.quantity, product.stockQuantity),
        unavailable: false,
      }
    }))
    return result
  }, [hydrated])

  const { itemCount, subtotal, hasUnavailable } = useMemo(() => {
    const available = items.filter(item => !item.unavailable)
    return {
      itemCount: available.reduce((sum, item) => sum + item.quantity, 0),
      // Round to cents: float sums (0.1 + 0.2) must not make the expected total differ from the
      // server's exact decimal total.
      subtotal: Math.round(available.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0) * 100) / 100,
      hasUnavailable: available.length !== items.length,
    }
  }, [items])

  // Mirrors ShopSettings.deliveryFeeFor on the server; the server's numbers always win, and an
  // order whose total differs from this one is rejected rather than charged.
  const deliveryFee = itemCount === 0 ? 0
    : settings === null ? null
    : settings.freeDeliveryThreshold !== null && subtotal >= settings.freeDeliveryThreshold ? 0
    : settings.deliveryFee
  const total = deliveryFee === null ? null : subtotal + deliveryFee
  const freeDeliveryThreshold = settings ? settings.freeDeliveryThreshold : null

  return (
    <CartContext.Provider value={{ items, addItem, updateQuantity, removeItem, clear, refresh, itemCount, subtotal, deliveryFee, total, freeDeliveryThreshold, hasUnavailable }}>
      {children}
    </CartContext.Provider>
  )
}

export const useCart = () => useContext(CartContext)

/** Turns a refresh result into one sentence for the customer, or null if nothing changed. */
export function describeCartChanges(result: CartRefreshResult): string | null {
  const parts: string[] = []
  if (result.priceChanges.length) parts.push(`Prices were updated for ${result.priceChanges.join(', ')}.`)
  if (result.quantityReduced.length) parts.push(`Quantities were reduced to the available stock for ${result.quantityReduced.join(', ')}.`)
  if (result.unavailable.length) parts.push(`${result.unavailable.join(', ')} ${result.unavailable.length === 1 ? 'is' : 'are'} no longer available.`)
  return parts.length ? parts.join(' ') : null
}
