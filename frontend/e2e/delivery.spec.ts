import { expect, test, type Page } from '@playwright/test'
import { acceptDialogs, ADMIN, CUSTOMER, failOnBrowserErrors, login, unique } from './support'

async function placeOrder(page: Page): Promise<string> {
  await page.goto('/products/2')
  await page.getByRole('button', { name: /Add to Cart/ }).click()
  await page.goto('/cart')
  await page.getByRole('link', { name: /Proceed to Checkout/ }).click()
  await page.getByPlaceholder('07X XXX XXXX').fill('0771234567')
  await page.getByPlaceholder('Apartment, suite, unit, building, street').fill('12 Lake Road')
  await page.getByPlaceholder('e.g. Colombo, Kandy, Galle').fill('Colombo')
  await page.getByPlaceholder('e.g. 00100').fill('00300')
  await page.getByRole('button', { name: /Place Order/ }).click()
  const confirmation = page.getByText(/Order Confirmed · PF-\d+/)
  await expect(confirmation).toBeVisible()
  return (await confirmation.textContent())!.match(/PF-\d+/)![0]
}

test('an order is only delivered after shipping, and the customer can report and confirm delivery', async ({ browser }) => {
  const customer = await (await browser.newContext()).newPage()
  const admin = await (await browser.newContext()).newPage()
  const customerErrors = failOnBrowserErrors(customer)
  const adminErrors = failOnBrowserErrors(admin)
  acceptDialogs(customer)
  acceptDialogs(admin)
  await login(customer, CUSTOMER)
  await login(admin, ADMIN)

  const orderNumber = await placeOrder(customer)
  const tracking = unique('LK').toUpperCase()

  // The studio cannot jump straight to DELIVERED, and shipping needs a tracking number.
  await admin.goto('/admin/orders')
  await admin.getByRole('link', { name: orderNumber }).click()
  const status = admin.getByLabel('New status')
  await expect(status.locator('option', { hasText: 'DELIVERED' })).toHaveCount(0)
  await status.selectOption('SHIPPED')
  await expect(admin.getByRole('button', { name: 'Update status' })).toBeDisabled()
  await admin.getByLabel('Tracking number').fill(tracking)
  await admin.getByRole('button', { name: 'Update status' }).click()
  await expect(admin.getByText(`${orderNumber} updated to SHIPPED`)).toBeVisible()
  await status.selectOption('DELIVERED')
  await admin.getByRole('button', { name: 'Update status' }).click()
  await expect(admin.getByText(`${orderNumber} updated to DELIVERED`)).toBeVisible()

  // The customer sees the tracking number and reports that nothing arrived.
  await customer.goto('/orders')
  await customer.getByRole('link', { name: orderNumber }).first().click()
  await expect(customer.locator('.tracking-number', { hasText: tracking })).toBeVisible()
  await customer.getByRole('button', { name: "I haven't received it" }).click()
  await customer.getByLabel(/What happened/).fill('No parcel and no call from the courier.')
  await customer.getByRole('button', { name: 'Report problem' }).click()
  await expect(customer.getByText(/You reported this order as not received/)).toBeVisible()

  // The studio sees the report and resolves it.
  await admin.goto('/admin/orders')
  await expect(admin.getByRole('row', { name: new RegExp(orderNumber) }).getByText('Not received')).toBeVisible()
  await admin.getByRole('link', { name: orderNumber }).click()
  await expect(admin.getByText('No parcel and no call from the courier.').first()).toBeVisible()
  await admin.getByLabel(/Resolution/).fill('The courier re-delivered it this morning.')
  await admin.getByRole('button', { name: 'Mark as resolved' }).click()
  await expect(admin.getByText('Delivery problem marked as resolved')).toBeVisible()

  // The customer sees the answer and confirms the order arrived.
  await customer.reload()
  await expect(customer.getByText(/The courier re-delivered it this morning/).first()).toBeVisible()
  await customer.getByRole('button', { name: 'Yes, I received it' }).click()
  await expect(customer.getByText(/You confirmed this order arrived/)).toBeVisible()
  await expect(customer.getByRole('button', { name: "I haven't received it" })).toHaveCount(0)
  await expect(customer.locator('.history-title')).toHaveText([
    'Order placed',
    'Status changed to Dispatched',
    'Status changed to Delivered',
    'Customer reported the order did not arrive',
    'Delivery problem resolved',
    'Customer confirmed the order arrived',
  ])

  customerErrors()
  adminErrors()
})
