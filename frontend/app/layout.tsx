import type { Metadata } from 'next'
import './globals.css'
import { Providers } from '../lib/providers'

export const metadata: Metadata = {
  title: 'PrintForge | Custom ideas. Physical creations.',
  description: 'A considered collection of ready-to-print objects and custom 3D printing.',
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><Providers>{children}</Providers></body></html>
}
