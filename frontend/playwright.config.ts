import { defineConfig } from '@playwright/test'

/**
 * End-to-end tests: real user journeys in Chrome against a running frontend + API + database.
 * They create accounts, orders and custom print requests, so they only run against a disposable
 * stack (see e2e/global-setup.ts and the README's "End-to-end tests" section).
 */
export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/global-setup.ts',
  // Journeys share one database and demo accounts; run them one at a time.
  workers: 1,
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:3000',
    // The installed Google Chrome; no browser download needed.
    channel: 'chrome',
    headless: true,
    trace: 'retain-on-failure',
  },
})
