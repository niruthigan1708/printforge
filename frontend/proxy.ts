import { NextRequest, NextResponse } from 'next/server'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
const API_ORIGIN = new URL(API_URL).origin

/**
 * Security headers for every page, including a strict Content Security Policy.
 *
 * Scripts only run if they carry this request's random nonce (Next.js adds it to its own scripts
 * when it sees the policy), so injected markup cannot execute JavaScript. 'strict-dynamic' lets
 * those trusted scripts load the app's chunks. Styles allow 'unsafe-inline' because components use
 * inline style attributes; that does not permit script execution. Images may come from the API
 * and, via admin-set product image URLs, from any HTTPS host.
 */
export function proxy(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString('base64')
  const isDev = process.env.NODE_ENV === 'development'

  const csp = [
    "default-src 'self'",
    // Dev mode's React Refresh needs eval; production builds do not.
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDev ? " 'unsafe-eval'" : ''}`,
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com",
    `img-src 'self' data: blob: ${API_ORIGIN} https:`,
    `connect-src 'self' ${API_ORIGIN}${isDev ? ' ws:' : ''}`,
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
  ].join('; ')

  // Next.js reads the policy (and nonce) from the request headers to tag its own scripts.
  const requestHeaders = new Headers(request.headers)
  requestHeaders.set('x-nonce', nonce)
  requestHeaders.set('Content-Security-Policy', csp)

  const response = NextResponse.next({ request: { headers: requestHeaders } })
  response.headers.set('Content-Security-Policy', csp)
  response.headers.set('X-Content-Type-Options', 'nosniff')
  response.headers.set('X-Frame-Options', 'DENY')
  response.headers.set('Referrer-Policy', 'strict-origin-when-cross-origin')
  response.headers.set('Permissions-Policy', 'camera=(), microphone=(), geolocation=(), payment=()')
  return response
}

export const config = {
  matcher: [
    {
      // Pages only: static assets need no per-request nonce, and prefetches reuse the page's.
      source: '/((?!_next/static|_next/image|icon.svg).*)',
      missing: [
        { type: 'header', key: 'next-router-prefetch' },
        { type: 'header', key: 'purpose', value: 'prefetch' },
      ],
    },
  ],
}
