import { readFileSync } from 'node:fs'
import { expect, type Page } from '@playwright/test'

export const API_URL = process.env.E2E_API_URL || 'http://localhost:8080/api'

export const CUSTOMER = { email: 'customer@printforge.com', password: 'Customer@12345' }
export const ADMIN = { email: 'admin@printforge.com', password: 'Admin@12345' }

/** A value no other test run has used, for emails and names. */
export const unique = (prefix: string) => `${prefix}-${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`

/** Signs in through the login form, as a user would. */
export async function login(page: Page, user: { email: string; password: string }) {
  await page.goto('/login')
  await page.locator('input[type=email]').fill(user.email)
  await page.locator('input[type=password]').fill(user.password)
  await page.locator('button[type=submit]').click()
  await expect(page).not.toHaveURL(/\/login/)
}

/** Fails the test on any page error or console error (which includes CSP violations). */
export function failOnBrowserErrors(page: Page) {
  const problems: string[] = []
  page.on('pageerror', error => problems.push(`pageerror: ${error.message}`))
  page.on('console', message => {
    // Expected 4xx answers (e.g. checking a signed-out session) are logged by the browser too.
    if (message.type() === 'error' && !/status of 40[134]/.test(message.text())) problems.push(`console: ${message.text()}`)
  })
  return () => expect(problems, 'browser errors').toEqual([])
}

/** Accepts the confirm() dialogs the app shows before irreversible actions. */
export function acceptDialogs(page: Page) {
  page.on('dialog', dialog => dialog.accept())
}

/**
 * The password reset link most recently sent to {@code email}. In CI the API sends real email to
 * a Mailpit server (MAILPIT_URL); locally, without SMTP, it writes the link to its log
 * (BACKEND_LOG points at that file).
 */
export async function latestResetLink(email: string): Promise<string> {
  const deadline = Date.now() + 20_000
  while (Date.now() < deadline) {
    const link = process.env.MAILPIT_URL ? await fromMailpit(email) : fromBackendLog(email)
    if (link) return link
    await new Promise(resolve => setTimeout(resolve, 500))
  }
  throw new Error(`No password reset link arrived for ${email} (set MAILPIT_URL or BACKEND_LOG)`)
}

async function fromMailpit(email: string): Promise<string | null> {
  const base = process.env.MAILPIT_URL!.replace(/\/$/, '')
  const search = await (await fetch(`${base}/api/v1/search?query=${encodeURIComponent(`to:"${email}"`)}`)).json()
  const newest = search.messages?.[0]
  if (!newest) return null
  const message = await (await fetch(`${base}/api/v1/message/${newest.ID}`)).json()
  return (message.Text as string).match(/https?:\/\/\S+\/reset-password\?token=\S+/)?.[0] ?? null
}

function fromBackendLog(email: string): string | null {
  if (!process.env.BACKEND_LOG) throw new Error('Set MAILPIT_URL or BACKEND_LOG to read password reset links')
  const log = readFileSync(process.env.BACKEND_LOG, 'utf8')
  const escaped = email.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const matches = Array.from(log.matchAll(new RegExp(`reset link for ${escaped} is logged instead of emailed: (\\S+)`, 'g')))
  return matches.length ? matches[matches.length - 1][1] : null
}
