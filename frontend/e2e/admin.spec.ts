import { expect, test } from '@playwright/test'
import { acceptDialogs, ADMIN, failOnBrowserErrors, login, unique } from './support'

test('an admin can create, rename and delete a category', async ({ page }) => {
  const assertNoErrors = failOnBrowserErrors(page)
  acceptDialogs(page)
  await login(page, ADMIN)
  const name = unique('E2E Category')

  await page.goto('/admin/categories')
  await page.getByPlaceholder('e.g. Cosplay Props').fill(name)
  await page.getByPlaceholder('Optional short description').fill('Made by an end-to-end test')
  await page.getByRole('button', { name: /Add Category/ }).click()
  await expect(page.getByText(`Category "${name}" created.`)).toBeVisible()

  const row = page.getByRole('row', { name: new RegExp(name) })
  await row.getByRole('button', { name: /Edit/ }).click()
  await page.getByLabel('Name', { exact: true }).last().fill(`${name} Renamed`)
  await page.getByRole('button', { name: /Save/ }).click()
  await expect(page.getByRole('cell', { name: `${name} Renamed` })).toBeVisible()

  await page.getByRole('button', { name: `Delete ${name} Renamed` }).click()
  await expect(page.getByText(`Category "${name} Renamed" deleted.`)).toBeVisible()
  await expect(page.getByRole('cell', { name: `${name} Renamed` })).toHaveCount(0)

  assertNoErrors()
})

test('a category that still has products cannot be deleted', async ({ page }) => {
  acceptDialogs(page)
  await login(page, ADMIN)
  await page.goto('/admin/categories')
  await page.getByRole('button', { name: 'Delete Gaming' }).click()
  await expect(page.getByText(/still has products/)).toBeVisible()
})
