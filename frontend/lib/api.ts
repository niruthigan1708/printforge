const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'

/**
 * The signed-in user's public details (never the token: the session lives in an httpOnly cookie
 * page scripts cannot read). Its presence means "we believe we are signed in", which is how a
 * 401 is recognised as an expired session rather than a plain wrong password.
 */
export const SESSION_USER_KEY = 'printforge-user'
/** Where older versions kept the token in localStorage; removed on load. */
export const LEGACY_TOKEN_KEY = 'printforge-token'

export class ApiError extends Error {
  status: number
  errors?: Record<string, string>
  code?: string
  constructor(message: string, status: number, errors?: Record<string, string>, code?: string) {
    super(message)
    this.status = status
    this.errors = errors
    this.code = code
  }
}

/** Fired on window when the server rejects our session; the auth context logs the user out. */
export const SESSION_EXPIRED_EVENT = 'printforge:session-expired'

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

function believesSignedIn(): boolean {
  return typeof window !== 'undefined' && localStorage.getItem(SESSION_USER_KEY) !== null
}

/**
 * CSRF token for state-changing requests. The API keeps it in an httpOnly cookie and hands the
 * matching value out from /auth/csrf; requests echo it in a header, which another site cannot do.
 */
let csrf: Promise<{ headerName: string; token: string }> | null = null

function csrfToken(): Promise<{ headerName: string; token: string }> {
  if (!csrf) {
    csrf = fetch(`${API_URL}/auth/csrf`, { credentials: 'include' })
      .then(res => (res.ok ? res.json() : Promise.reject(new Error(`csrf ${res.status}`))))
      .catch(err => { csrf = null; throw err })
  }
  return csrf
}

/** Drops the cached CSRF token (it is tied to the session, so it changes when the session does). */
export function resetCsrfToken() { csrf = null }

async function send(path: string, options: RequestInit): Promise<Response> {
  const method = (options.method || 'GET').toUpperCase()
  const headers = new Headers(options.headers)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (!SAFE_METHODS.has(method)) {
    try {
      const { headerName, token } = await csrfToken()
      headers.set(headerName, token)
    } catch {
      throw new ApiError('Could not reach the PrintForge server. Please try again.', 0)
    }
  }
  try {
    return await fetch(`${API_URL}${path}`, { ...options, headers, credentials: 'include' })
  } catch {
    throw new ApiError('Could not reach the PrintForge server. Please try again.', 0)
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let res = await send(path, options)

  // The CSRF token went stale (e.g. the session changed in another tab): fetch a new one, retry once.
  if (res.status === 403 && !SAFE_METHODS.has((options.method || 'GET').toUpperCase())) {
    const body = parseJson(await res.clone().text())
    if (isRecord(body) && body.code === 'CSRF') {
      resetCsrfToken()
      res = await send(path, options)
    }
  }

  // A 401 while we believe we are signed in means the session is no longer valid (expired,
  // revoked by a logout elsewhere, or signed with a rotated secret). Login/register 401s are just
  // wrong credentials. Checked before reading the body, which may not even be JSON.
  if (res.status === 401 && believesSignedIn() && !path.startsWith('/auth/')) {
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    throw new ApiError('Your session has expired. Please log in again.', 401)
  }

  if (res.status === 204) return undefined as T
  const text = await res.text()
  // Proxies, gateways and timeouts can answer with HTML or plain text instead of our JSON.
  const data = parseJson(text)

  if (!res.ok) {
    const message = (isRecord(data) && typeof data.message === 'string' && data.message)
      || (res.status === 502 || res.status === 503 || res.status === 504
        ? 'The PrintForge server is temporarily unavailable. Please try again shortly.'
        : 'Something went wrong. Please try again.')
    const errors = isRecord(data) && isRecord(data.errors) ? data.errors as Record<string, string> : undefined
    const code = isRecord(data) && typeof data.code === 'string' ? data.code : undefined
    throw new ApiError(message, res.status, errors, code)
  }
  if (text && data === undefined) {
    throw new ApiError('The server sent an unexpected response. Please try again.', res.status)
  }
  return (data ?? null) as T
}

/** Parsed JSON, null for an empty body, or undefined if the body is not JSON. */
function parseJson(text: string): unknown {
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export const api = {
  get: <T,>(path: string) => request<T>(path),
  post: <T,>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T,>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined }),
  del: <T,>(path: string) => request<T>(path, { method: 'DELETE' }),
  postForm: <T,>(path: string, form: FormData) => request<T>(path, { method: 'POST', body: form }),
  /** A raw response (e.g. a file download), sent with the session cookie. Errors as for JSON calls. */
  download: async (path: string): Promise<Blob> => {
    const res = await send(path, { method: 'GET' })
    if (res.status === 401 && believesSignedIn()) {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
      throw new ApiError('Your session has expired. Please log in again.', 401)
    }
    if (!res.ok) {
      const data = parseJson(await res.text())
      throw new ApiError(isRecord(data) && typeof data.message === 'string' ? data.message : 'Could not download the file.', res.status)
    }
    return res.blob()
  },
}

/**
 * Pass the product's imageVersion whenever you have it: a versioned URL is cached long-term and
 * changes when the photo changes. Without it the browser revalidates the photo on every use.
 */
export const productImageUrl = (productId: number, version?: string | null) =>
  `${API_URL}/products/${productId}/image${version ? `?v=${encodeURIComponent(version)}` : ''}`

export { API_URL }
