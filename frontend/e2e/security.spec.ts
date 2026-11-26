import { expect, test } from '@playwright/test'
import { ADMIN, CUSTOMER, failOnBrowserErrors, login } from './support'

test('the session cookie is httpOnly and no token is ever stored in the page', async ({ page, context }) => {
  await login(page, CUSTOMER)

  const session = (await context.cookies()).find(cookie => cookie.name === 'pf_session')
  expect(session?.httpOnly).toBe(true)
  expect(session?.sameSite).toBe('Lax')
  expect(await page.evaluate(() => document.cookie)).not.toContain('pf_session')
  expect(await page.evaluate(() => JSON.stringify(localStorage))).not.toMatch(/eyJ[\w-]+\.[\w-]+\./) // no JWT
})

test('every page is served with a nonce-based CSP and renders without errors', async ({ page }) => {
  const assertNoErrors = failOnBrowserErrors(page)
  await login(page, ADMIN)
  for (const path of ['/', '/products', '/products/1', '/cart', '/orders', '/account', '/admin', '/admin/products', '/admin/categories', '/admin/orders', '/admin/custom-prints']) {
    const response = await page.goto(path)
    expect(response?.headers()['content-security-policy'], path).toMatch(/script-src 'self' 'nonce-[^']+' 'strict-dynamic'/)
    await expect(page.locator('main')).toBeVisible()
  }
  assertNoErrors()
})

test('customers are kept out of the admin studio', async ({ page }) => {
  await login(page, CUSTOMER)
  await page.goto('/admin')
  await expect(page.getByText("You don't have access to the admin studio")).toBeVisible()
})

test('signing out revokes the session', async ({ page }) => {
  await login(page, CUSTOMER)
  await page.getByTitle('Log out').click()
  await expect(page.locator('header').getByRole('link', { name: 'Log In' }).first()).toBeVisible()
  await page.goto('/orders')
  await expect(page).toHaveURL(/\/login/)
})
