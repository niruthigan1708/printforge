/**
 * Returns `target` only if it is a same-site path (e.g. `/checkout`), otherwise `fallback`.
 * Rejects absolute URLs (`https://evil.example`), protocol-relative URLs (`//evil.example`),
 * backslash tricks (`/\evil.example`, which browsers treat like `//`), and other schemes.
 */
export function safeRedirect(target: string | null | undefined, fallback = '/'): string {
  if (!target || !target.startsWith('/')) return fallback
  if (target.startsWith('//') || target.startsWith('/\\')) return fallback
  // Control characters and whitespace can be stripped by browsers, turning `/\t/evil` into `//evil`.
  if (/[\u0000-\u001F\u007F\s]/.test(target)) return fallback
  return target
}
