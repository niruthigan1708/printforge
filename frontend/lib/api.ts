const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
const TOKEN_KEY = 'printforge-token'

export class ApiError extends Error {
  status: number
  errors?: Record<string, string>
  constructor(message: string, status: number, errors?: Record<string, string>) {
    super(message)
    this.status = status
    this.errors = errors
  }
}

/** Fired on window when the server rejects our token; the auth context logs the user out. */
export const SESSION_EXPIRED_EVENT = 'printforge:session-expired'

export function getToken(): string | null {
  if (typeof window === 'undefined') return null
  return localStorage.getItem(TOKEN_KEY)
}

/** True if the JWT's `exp` claim is in the past (or the token cannot be read). */
export function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers = new Headers(options.headers)
  const isForm = options.body instanceof FormData
  if (!isForm && options.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  let res: Response
  try {
    res = await fetch(`${API_URL}${path}`, { ...options, headers })
  } catch {
    throw new ApiError('Could not reach the PrintForge server. Please try again.', 0)
  }

  if (res.status === 204) return undefined as T
  const text = await res.text()
  const data = text ? JSON.parse(text) : null

  // A 401 on a request that carried a token means the session is no longer valid (expired,
  // or signed with a rotated secret). Login/register 401s are just wrong credentials.
  if (res.status === 401 && token && !path.startsWith('/auth/')) {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    throw new ApiError('Your session has expired. Please log in again.', 401)
  }

  if (!res.ok) {
    const message = data?.message || 'Something went wrong. Please try again.'
    throw new ApiError(message, res.status, data?.errors)
  }
  return data as T
}

export const api = {
  get: <T,>(path: string) => request<T>(path),
  post: <T,>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T,>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined }),
  del: <T,>(path: string) => request<T>(path, { method: 'DELETE' }),
  postForm: <T,>(path: string, form: FormData) => request<T>(path, { method: 'POST', body: form }),
}

/**
 * Pass the product's imageVersion whenever you have it: a versioned URL is cached long-term and
 * changes when the photo changes. Without it the browser revalidates the photo on every use.
 */
export const productImageUrl = (productId: number, version?: string | null) =>
  `${API_URL}/products/${productId}/image${version ? `?v=${encodeURIComponent(version)}` : ''}`

export { API_URL, TOKEN_KEY }
