import { expect, test, type Page } from '@playwright/test'
import { failOnBrowserErrors } from './support'

const canvas = (page: Page) => page.evaluate(() => getComputedStyle(document.body).backgroundColor)
const LIGHT = 'rgb(248, 249, 250)'
const DARK = 'rgb(14, 16, 22)'

test('the site follows the device theme until the visitor picks one', async ({ browser }) => {
  const context = await browser.newContext({ colorScheme: 'dark' })
  const page = await context.newPage()
  const assertNoErrors = failOnBrowserErrors(page)

  await page.goto('/products')
  expect(await canvas(page)).toBe(DARK)

  await page.getByRole('button', { name: 'Switch to light mode' }).click()
  expect(await canvas(page)).toBe(LIGHT)

  // The choice survives a reload and is applied before the page is painted, so it never flashes.
  await page.reload()
  expect(await page.evaluate(() => document.documentElement.getAttribute('data-theme'))).toBe('light')
  expect(await canvas(page)).toBe(LIGHT)
  await expect(page.getByRole('button', { name: 'Switch to dark mode' })).toBeVisible()

  await page.getByRole('button', { name: 'Switch to dark mode' }).click()
  expect(await canvas(page)).toBe(DARK)

  assertNoErrors()
  await context.close()
})

test('a light device shows the light theme by default', async ({ browser }) => {
  const context = await browser.newContext({ colorScheme: 'light' })
  const page = await context.newPage()
  await page.goto('/')
  expect(await canvas(page)).toBe(LIGHT)
  await expect(page.getByRole('button', { name: 'Switch to dark mode' })).toBeVisible()
  await context.close()
})
