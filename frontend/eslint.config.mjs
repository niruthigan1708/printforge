import { defineConfig, globalIgnores } from 'eslint/config'
import nextVitals from 'eslint-config-next/core-web-vitals'

export default defineConfig([
  ...nextVitals,
  globalIgnores(['.next/**', 'out/**', 'build/**', 'next-env.d.ts']),
  {
    rules: {
      // Product photos come from the API on another origin and fall back to an inline SVG
      // placeholder on error, which next/image cannot do.
      '@next/next/no-img-element': 'off',
      // A pages-router rule that misfires on the font <link> in the App Router root layout.
      '@next/next/no-page-custom-font': 'off',
    },
  },
])
