import { expect, test } from '@playwright/test'
import { acceptDialogs, CUSTOMER, failOnBrowserErrors, login } from './support'

test('a customer can buy a product, track the order and cancel it', async ({ page }) => {
  const assertNoErrors = failOnBrowserErrors(page)
  acceptDialogs(page)
  await login(page, CUSTOMER)

  await page.goto('/products/1')
  await page.getByRole('button', { name: /Add to Cart/ }).click()
  await page.goto('/cart')
  await expect(page.getByText('Adjustable Phone Stand')).toBeVisible()
  await page.getByRole('link', { name: /Proceed to Checkout/ }).click()

  await page.getByPlaceholder('07X XXX XXXX').fill('0771234567')
  await page.getByPlaceholder('Apartment, suite, unit, building, street').fill('12 Lake Road')
  await page.getByPlaceholder('e.g. Colombo, Kandy, Galle').fill('Colombo')
  await page.getByPlaceholder('e.g. 00100').fill('00300')
  await page.getByRole('button', { name: /Place Order/ }).click()

  const confirmation = page.getByText(/Order Confirmed · PF-\d+/)
  await expect(confirmation).toBeVisible()
  const orderNumber = (await confirmation.textContent())!.match(/PF-\d+/)![0]

  await page.goto('/orders')
  await page.getByRole('link', { name: orderNumber }).first().click()
  await expect(page.getByText('PENDING').first()).toBeVisible()
  await page.getByRole('button', { name: 'Cancel Order' }).click()
  await expect(page.getByText('CANCELLED').first()).toBeVisible()
  await expect(page.getByRole('button', { name: 'Cancel Order' })).toHaveCount(0)

  assertNoErrors()
})

test('search and category filters combine', async ({ page }) => {
  await page.goto('/products?search=stand&category=Gaming')
  await expect(page.getByText('Controller Stand').first()).toBeVisible()
  await expect(page.getByText('Headphone Stand')).toHaveCount(0)
})
