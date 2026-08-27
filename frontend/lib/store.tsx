'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { Product } from './types'

const CartContext = createContext<{ cart: Product[]; add: (product: Product) => void; remove: (index: number) => void; clear: () => void }>({ cart: [], add: () => {}, remove: () => {}, clear: () => {} })

export function StoreProvider({ children }: { children: React.ReactNode }) {
  const [cart, setCart] = useState<Product[]>([])
  useEffect(() => { const saved = localStorage.getItem('printforge-cart'); if (saved) setCart(JSON.parse(saved)) }, [])
  useEffect(() => { localStorage.setItem('printforge-cart', JSON.stringify(cart)) }, [cart])
  const value = useMemo(() => ({ cart, add: (product: Product) => setCart(items => [...items, product]), remove: (index: number) => setCart(items => items.filter((_, itemIndex) => itemIndex !== index)), clear: () => setCart([]) }), [cart])
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export const useCart = () => useContext(CartContext)
