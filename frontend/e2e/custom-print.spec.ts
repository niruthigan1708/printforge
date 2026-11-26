import { expect, test } from '@playwright/test'
import { ADMIN, CUSTOMER, failOnBrowserErrors, login } from './support'

const STL = Buffer.from('solid bracket\n facet normal 0 0 1\n  outer loop\n   vertex 0 0 0\n   vertex 1 0 0\n   vertex 0 1 0\n  endloop\n endfacet\nendsolid bracket\n')

test('a custom print goes from upload to quote to acceptance', async ({ browser }) => {
  const customerContext = await browser.newContext()
  const customer = await customerContext.newPage()
  const assertNoCustomerErrors = failOnBrowserErrors(customer)
  await login(customer, CUSTOMER)

  await customer.goto('/custom-print')
  await customer.locator('input[type=file]').setInputFiles({ name: 'e2e-bracket.stl', mimeType: 'model/stl', buffer: STL })
  await customer.getByRole('button', { name: /Submit 3D Model for Quote/ }).click()
  await customer.getByRole('link', { name: /View Request Status/ }).click()
  await expect(customer).toHaveURL(/\/custom-print\/\d+/)
  const requestPath = new URL(customer.url()).pathname
  const requestId = requestPath.split('/').pop()

  // It is listed on My Orders.
  await customer.goto('/orders')
  await expect(customer.getByText('e2e-bracket.stl').first()).toBeVisible()

  // The admin downloads nothing here, just quotes it.
  const adminContext = await browser.newContext()
  const admin = await adminContext.newPage()
  await login(admin, ADMIN)
  await admin.goto(`/admin/custom-prints/${requestId}`)
  await admin.locator('input[type=number]').fill('2750')
  await admin.getByRole('button', { name: /Send Formal Quote/ }).click()
  await expect(admin.getByText('Formal quote has been sent')).toBeVisible()

  await customer.goto(requestPath)
  await customer.getByRole('button', { name: /Accept Quote/ }).click()
  await expect(customer.getByText(/Quote Accepted/)).toBeVisible()

  assertNoCustomerErrors()
  await customerContext.close()
  await adminContext.close()
})

test('files that are not really models are refused', async ({ page }) => {
  await login(page, CUSTOMER)
  await page.goto('/custom-print')
  await page.locator('input[type=file]').setInputFiles({ name: 'invoice.stl', mimeType: 'model/stl', buffer: Buffer.from('<html>not a model</html>') })
  await page.getByRole('button', { name: /Submit 3D Model for Quote/ }).click()
  await expect(page.getByText(/not a valid STL/)).toBeVisible()
})
