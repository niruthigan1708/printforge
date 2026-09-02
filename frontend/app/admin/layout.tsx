'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { LayoutDashboard, Box, ShoppingBag, Layers, ShieldCheck } from 'lucide-react'
import { Header } from '../../components/Header'
import { RequireAdmin } from '../../lib/guards'

const LINKS = [
  { href: '/admin', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/admin/products', label: 'Products', icon: Box },
  { href: '/admin/orders', label: 'Orders', icon: ShoppingBag },
  { href: '/admin/custom-prints', label: 'Custom Prints', icon: Layers },
]

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()

  return (
    <RequireAdmin>
      <Header />
      <div className="admin-shell">
        <nav className="admin-sidebar">
          <div style={{ padding: '0 12px 12px', fontSize: 11, fontFamily: 'var(--font-mono)', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            STUDIO CONSOLE
          </div>
          {LINKS.map(link => {
            const Icon = link.icon
            const isActive = pathname === link.href || (link.href !== '/admin' && pathname.startsWith(link.href))
            return (
              <Link
                key={link.href}
                href={link.href}
                className={isActive ? 'active' : ''}
              >
                <Icon size={16} />
                <span>{link.label}</span>
              </Link>
            )
          })}
        </nav>
        <main className="admin-main">{children}</main>
      </div>
    </RequireAdmin>
  )
}
