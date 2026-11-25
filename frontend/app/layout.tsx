import type { Metadata, Viewport } from 'next'
import './globals.css'
import { connection } from 'next/server'
import { Providers } from '../lib/providers'

export const metadata: Metadata = {
  title: 'PrintForge — Precision 3D Printing & Custom Studio',
  description: 'A precision 3D printing studio. Browse ready-to-order industrial & designer objects, or upload your own 3D model for custom manufacturing.',
  keywords: ['3D printing', 'custom 3D prints', 'STL printing', 'rapid prototyping', 'PLA', 'PETG', 'ABS', 'Sri Lanka'],
}

export const viewport: Viewport = {
  themeColor: '#0F1115',
  width: 'device-width',
  initialScale: 1,
}

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  // Render every page per request: the CSP nonce set in proxy.ts is new for each request, and a
  // statically prerendered page would carry no nonce, so its scripts would be blocked.
  await connection()
  return (
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600&family=Plus+Jakarta+Sans:wght@500;600;700;800&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}
