import { Box } from 'lucide-react'
import { Header } from './Header'

/** Centered card used by the sign-in style pages (forgot / reset password). */
export function AuthCard({ eyebrow, title, subtitle, children }: { eyebrow: string; title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <main>
      <Header />
      <section className="shell" style={{ padding: '70px 0 100px', display: 'grid', placeItems: 'center' }}>
        <div className="form-card" style={{ width: '100%', maxWidth: 480, padding: '40px 36px', boxShadow: 'var(--shadow-lg)' }}>
          <div style={{ textAlign: 'center', marginBottom: 28 }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 'var(--radius-md)',
                background: 'linear-gradient(135deg, #FF7733 0%, var(--accent-primary) 100%)',
                color: 'white',
                display: 'grid',
                placeItems: 'center',
                margin: '0 auto 16px',
                boxShadow: 'var(--shadow-orange)',
              }}
            >
              <Box size={26} />
            </div>
            <div className="eyebrow" style={{ justifyContent: 'center' }}>{eyebrow}</div>
            <h1 style={{ fontSize: 28, margin: '4px 0 8px' }}>{title}</h1>
            <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>{subtitle}</p>
          </div>
          {children}
        </div>
      </section>
    </main>
  )
}
