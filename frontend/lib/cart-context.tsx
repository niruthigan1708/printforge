'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { Product } from './types'

const CART_KEY = 'printforge-cart'
export const DELIVERY_FEE = 350

export type CartItem = {
  productId: number
  name: string
  price: number
  imageUrl: string | null
  material: string
  color: string
  category: string
  stockQuantity: number
  quantity: number
}

type CartContextValue = {
  items: CartItem[]
  addItem: (product: Product, quantity?: number) => void
  updateQuantity: (productId: number, quantity: number) => void
  removeItem: (productId: number) => void
  clear: () => void
  itemCount: number
  subtotal: number
  deliveryFee: number
  total: number
}

const CartContext = createContext<CartContextValue>({
  items: [],
  addItem: () => {},
  updateQuantity: () => {},
  removeItem: () => {},
  clear: () => {},
  itemCount: 0,
  subtotal: 0,
  deliveryFee: 0,
  total: 0,
})

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([])
  const [hydrated, setHydrated] = useState(false)

  useEffect(() => {
    try {
      const saved = localStorage.getItem(CART_KEY)
      if (saved) setItems(JSON.parse(saved))
    } catch {
      // ignore corrupted local storage
    }
    setHydrated(true)
  }, [])

  useEffect(() => {
    if (hydrated) localStorage.setItem(CART_KEY, JSON.stringify(items))
  }, [items, hydrated])

  const addItem = (product: Product, quantity = 1) => {
    setItems(current => {
      const existing = current.find(item => item.productId === product.id)
      if (existing) {
        const nextQuantity = Math.min(existing.quantity + quantity, product.stockQuantity)
        return current.map(item => (item.productId === product.id ? { ...item, quantity: nextQuantity } : item))
      }
      const cappedQuantity = Math.max(1, Math.min(quantity, product.stockQuantity))
      return [...current, {
        productId: product.id,
        name: product.name,
        price: product.price,
        imageUrl: product.imageUrl,
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

  const { itemCount, subtotal } = useMemo(() => ({
    itemCount: items.reduce((sum, item) => sum + item.quantity, 0),
    subtotal: items.reduce((sum, item) => sum + item.price * item.quantity, 0),
  }), [items])

  const deliveryFee = items.length > 0 ? DELIVERY_FEE : 0
  const total = subtotal + deliveryFee

  return (
    <CartContext.Provider value={{ items, addItem, updateQuantity, removeItem, clear, itemCount, subtotal, deliveryFee, total }}>
      {children}
    </CartContext.Provider>
  )
}

export const useCart = () => useContext(CartContext)
