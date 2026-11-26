'use client'

import { AuthProvider } from './auth-context'
import { CartProvider } from './cart-context'
import { ScrollReveal } from '../components/ScrollReveal'

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <CartProvider>
        {children}
        <ScrollReveal />
      </CartProvider>
    </AuthProvider>
  )
}
