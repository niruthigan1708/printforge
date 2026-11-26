import { API_URL } from './support'

/**
 * Refuses to run against anything but a disposable stack: these tests create accounts, orders and
 * custom print requests, and change passwords. Also waits until the API is up and seeded.
 */
export default async function globalSetup() {
  if (process.env.E2E_DISPOSABLE !== '1') {
    throw new Error(
      'End-to-end tests write to the database (accounts, orders, password changes). Run them only against a '
      + 'disposable stack started with SEED_DEMO_DATA=true, and set E2E_DISPOSABLE=1 to confirm.',
    )
  }
  const deadline = Date.now() + 120_000
  while (Date.now() < deadline) {
    try {
      const products = await (await fetch(`${API_URL}/products`)).json()
      if (Array.isArray(products) && products.length > 0) return
    } catch { /* not up yet */ }
    await new Promise(resolve => setTimeout(resolve, 2000))
  }
  throw new Error(`The API at ${API_URL} did not come up with demo products within 2 minutes`)
}
