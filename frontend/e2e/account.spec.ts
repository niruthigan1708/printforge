import { expect, test, type Page } from '@playwright/test'
import { failOnBrowserErrors, latestResetLink, login, unique } from './support'

async function register(page: Page, name: string, email: string, password: string) {
  await page.goto('/register')
  await page.getByPlaceholder('e.g. Maya Fernando').fill(name)
  await page.getByPlaceholder('maya@example.com').fill(email)
  await page.getByPlaceholder('At least 8 characters').fill(password)
  await page.locator('button[type=submit]').click()
  await expect(page).not.toHaveURL(/\/register/)
}

test('a new customer can rename themselves and change their password', async ({ page }) => {
  const assertNoErrors = failOnBrowserErrors(page)
  const email = `${unique('e2e-account')}@example.com`
  await register(page, 'Maya Fernando', email, 'First-Pass-123')

  await page.getByTitle('Account settings').click()
  await expect(page.locator('input[disabled]').first()).toHaveValue(email)
  await page.getByLabel('Name').fill('Maya Perera')
  await page.getByRole('button', { name: 'Save changes' }).click()
  await expect(page.getByText('Your name has been updated.')).toBeVisible()
  await expect(page.getByTitle('Account settings')).toContainText('Maya')

  await page.getByLabel('Current password').fill('First-Pass-123')
  await page.getByLabel('New password', { exact: true }).fill('Second-Pass-456')
  await page.getByLabel('Confirm new password').fill('Second-Pass-456')
  await page.getByRole('button', { name: 'Change password' }).click()
  await expect(page.getByText(/Password changed/)).toBeVisible()

  await page.getByTitle('Log out').click()
  await login(page, { email, password: 'Second-Pass-456' })
  assertNoErrors()
})

test('a forgotten password can be reset from the emailed link', async ({ page }) => {
  const email = `${unique('e2e-reset')}@example.com`
  await register(page, 'Reset Tester', email, 'Original-Pass-1')
  await page.getByTitle('Log out').click()

  await page.goto('/login')
  await page.getByRole('link', { name: 'Forgot password?' }).click()
  await page.getByPlaceholder('you@example.com').fill(email)
  await page.getByRole('button', { name: /Email me a reset link/ }).click()
  await expect(page.getByText(/we've sent a link/)).toBeVisible()

  const link = new URL(await latestResetLink(email))
  await page.goto(link.pathname + link.search)
  await page.getByLabel('New Password', { exact: true }).fill('Recovered-Pass-2')
  await page.getByLabel('Confirm New Password').fill('Recovered-Pass-2')
  await page.getByRole('button', { name: /Set new password/ }).click()
  await expect(page.getByText(/password has been changed/)).toBeVisible()

  // The link works once.
  await page.goto(link.pathname + link.search)
  await page.getByLabel('New Password', { exact: true }).fill('Another-Pass-3')
  await page.getByLabel('Confirm New Password').fill('Another-Pass-3')
  await page.getByRole('button', { name: /Set new password/ }).click()
  await expect(page.getByText(/invalid or has expired/)).toBeVisible()

  await login(page, { email, password: 'Recovered-Pass-2' })
})
