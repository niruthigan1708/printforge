'use client'

import { useState } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { ShoppingBag, Menu, X, Box, User as UserIcon, LogOut, ShieldAlert } from 'lucide-react'
import { useCart } from '../lib/cart-context'
import { useAuth } from '../lib/auth-context'

export function Header() {
  const pathname = usePathname()
  const { itemCount } = useCart()
  const { user, logout } = useAuth()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const links = [
    { href: '/products', label: 'Catalog' },
    { href: '/custom-print', label: 'Custom 3D Print' },
    ...(user ? [{ href: '/orders', label: 'My Orders' }] : []),
    ...(user?.role === 'ADMIN' ? [{ href: '/admin', label: 'Admin Studio' }] : []),
  ]

  return (
    <>
      <div className="header-wrapper">
        <header className="shell nav">
          <Link className="logo" href="/">
            <span className="logo-mark">
              <Box size={18} />
            </span>
            <span>Print<span style={{ color: 'var(--accent-primary)' }}>Forge</span></span>
          </Link>

          <nav className="nav-links">
            {links.map(link => {
              const isActive = pathname === link.href || (link.href !== '/' && pathname.startsWith(link.href))
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`nav-link ${isActive ? 'active' : ''}`}
                >
                  {link.label}
                </Link>
              )
            })}
          </nav>

          <div className="nav-actions">
            <Link className="icon-btn" href="/cart" aria-label="Open Cart">
              <ShoppingBag size={18} />
              {itemCount > 0 && <span className="cart-count">{itemCount}</span>}
            </Link>

            {user ? (
              <div className="account-menu">
                <div className="user-pill">
                  <span className="user-avatar">
                    {user.role === 'ADMIN' ? <ShieldAlert size={12} /> : user.name.charAt(0).toUpperCase()}
                  </span>
                  <span>{user.name.split(' ')[0]}</span>
                </div>
                <button className="icon-btn" onClick={logout} title="Log out">
                  <LogOut size={16} />
                </button>
              </div>
            ) : (
              <div className="account-menu">
                <Link className="link-btn" href="/login" style={{ marginRight: 6 }}>
                  Log In
                </Link>
                <Link className="btn btn-sm btn-primary" href="/register">
                  Sign Up
                </Link>
              </div>
            )}

            <button
              className="icon-btn mobile-menu-btn"
              onClick={() => setMobileMenuOpen(true)}
              aria-label="Open navigation menu"
            >
              <Menu size={20} />
            </button>
          </div>
        </header>
      </div>

      {/* Mobile Drawer */}
      <div
        className={`mobile-drawer ${mobileMenuOpen ? 'open' : ''}`}
        onClick={() => setMobileMenuOpen(false)}
      >
        <div className="mobile-drawer-content" onClick={e => e.stopPropagation()}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Link className="logo" href="/" onClick={() => setMobileMenuOpen(false)}>
              <span className="logo-mark">
                <Box size={18} />
              </span>
              <span>PrintForge</span>
            </Link>
            <button className="icon-btn" onClick={() => setMobileMenuOpen(false)}>
              <X size={18} />
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
            {links.map(link => (
              <Link
                key={link.href}
                href={link.href}
                className="nav-link"
                style={{ fontSize: 16, padding: '12px 16px' }}
                onClick={() => setMobileMenuOpen(false)}
              >
                {link.label}
              </Link>
            ))}
          </div>

          <div style={{ marginTop: 'auto', paddingTop: 20, borderTop: '1px solid var(--border-subtle)' }}>
            {user ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div className="user-pill" style={{ justifyContent: 'center' }}>
                  <UserIcon size={14} />
                  <span>{user.name} ({user.role})</span>
                </div>
                <button
                  className="btn secondary"
                  style={{ width: '100%' }}
                  onClick={() => {
                    logout()
                    setMobileMenuOpen(false)
                  }}
                >
                  <LogOut size={16} /> Log Out
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                <Link
                  className="btn secondary"
                  href="/login"
                  style={{ width: '100%' }}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Log In
                </Link>
                <Link
                  className="btn orange"
                  href="/register"
                  style={{ width: '100%' }}
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Create Account
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  )
}
