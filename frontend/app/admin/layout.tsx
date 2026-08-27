'use client'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Header } from '../../components/Header'
import { RequireAdmin } from '../../lib/guards'

const LINKS = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/products', label: 'Products' },
  { href: '/admin/orders', label: 'Orders' },
  { href: '/admin/custom-prints', label: 'Custom prints' },
]

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  return (
    <RequireAdmin>
      <Header />
      <div className="admin-shell">
        <nav className="admin-sidebar">
          {LINKS.map(link => (
            <Link key={link.href} href={link.href} className={pathname === link.href || (link.href !== '/admin' && pathname.startsWith(link.href)) ? 'active' : ''}>
              {link.label}
            </Link>
          ))}
        </nav>
        <main className="admin-main">{children}</main>
      </div>
    </RequireAdmin>
  )
}
