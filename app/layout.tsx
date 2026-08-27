import type { Metadata } from 'next'
import './globals.css'
import { StoreProvider } from '../lib/store'

export const metadata: Metadata = {
  title: 'PrintForge | Custom ideas. Physical creations.',
  description: 'A considered collection of ready-to-print objects and custom 3D printing.',
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><StoreProvider>{children}</StoreProvider></body></html>
}
