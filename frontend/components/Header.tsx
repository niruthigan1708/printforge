'use client'
import Link from 'next/link'
import { ShoppingBag } from 'lucide-react'
import { useCart } from '../lib/cart-context'
import { useAuth } from '../lib/auth-context'

export function Header() {
  const { itemCount } = useCart()
  const { user, logout } = useAuth()

  return (
    <header className="shell nav">
      <Link className="logo" href="/"><span className="logo-mark" />PrintForge</Link>
      <nav className="nav-links">
        <Link href="/products">Shop</Link>
        <Link href="/custom-print">Custom print</Link>
        {user && <Link href="/orders">My orders</Link>}
        {user?.role === 'ADMIN' && <Link href="/admin">Admin</Link>}
      </nav>
      <div className="nav-actions">
        <Link className="icon-btn" href="/cart" aria-label="Open cart">
          <ShoppingBag size={19} />
          {itemCount > 0 && <span className="cart-count">{itemCount}</span>}
        </Link>
        {user ? (
          <div className="account-menu">
            <span className="account-name">{user.name.split(' ')[0]}</span>
            <button className="link-btn" onClick={logout}>Log out</button>
          </div>
        ) : (
          <div className="account-menu">
            <Link className="link-btn" href="/login">Log in</Link>
            <Link className="btn" href="/register">Sign up</Link>
          </div>
        )}
      </div>
    </header>
  )
}
