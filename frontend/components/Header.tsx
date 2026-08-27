'use client'
import Link from 'next/link'
import { ShoppingBag } from 'lucide-react'
import { useCart } from '../lib/store'

export function Header() {
  const { cart } = useCart()
  return <header className="shell nav"><Link className="logo" href="/"><span className="logo-mark" />PrintForge</Link><nav className="nav-links"><Link href="/products">Shop</Link><Link href="/#how">How it works</Link><Link href="/custom-print">Custom print</Link></nav><Link className="icon-btn" href="/cart" aria-label="Open cart"><ShoppingBag size={19}/>{cart.length > 0 && <span className="cart-count">{cart.length}</span>}</Link></header>
}
