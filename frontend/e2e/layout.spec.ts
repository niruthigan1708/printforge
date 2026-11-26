import { expect, test } from '@playwright/test'
import { ADMIN, CUSTOMER, login } from './support'

const PUBLIC = ['/', '/products', '/products/1', '/cart', '/login', '/register']
const CUSTOMER_PAGES = ['/checkout', '/orders', '/account', '/custom-print']
const ADMIN_PAGES = ['/admin', '/admin/products', '/admin/products/new', '/admin/categories', '/admin/orders', '/admin/custom-prints']

test.use({ viewport: { width: 360, height: 740 } })

/** Wide content (tables, button rows) must scroll inside its own box, never sideways across the page. */
async function expectNoSidewaysScroll(page: import('@playwright/test').Page, paths: string[]) {
  for (const path of paths) {
    await page.goto(path)
    await page.waitForLoadState('networkidle')
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
    expect(overflow, `${path} is wider than the screen`).toBeLessThanOrEqual(0)
  }
}

test('pages fit a phone screen for customers', async ({ page }) => {
  await login(page, CUSTOMER)
  await expectNoSidewaysScroll(page, [...PUBLIC, ...CUSTOMER_PAGES])
})

test('admin pages fit a phone screen', async ({ page }) => {
  await login(page, ADMIN)
  await expectNoSidewaysScroll(page, ADMIN_PAGES)
})
