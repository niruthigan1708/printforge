/** Where the visitor's explicit light/dark choice is kept. Absent means "follow the device". */
export const THEME_STORAGE_KEY = 'printforge-theme'

export type Theme = 'light' | 'dark'

const CHANGE_EVENT = 'printforge:theme-change'
const DARK_QUERY = '(prefers-color-scheme: dark)'

/** The theme currently shown: an explicit choice on <html>, else the device setting. */
export function currentTheme(): Theme {
  const chosen = document.documentElement.getAttribute('data-theme')
  if (chosen === 'light' || chosen === 'dark') return chosen
  return window.matchMedia(DARK_QUERY).matches ? 'dark' : 'light'
}

/** Shows `theme` now and remembers it for later visits (when storage is available). */
export function setTheme(theme: Theme) {
  document.documentElement.setAttribute('data-theme', theme)
  try { localStorage.setItem(THEME_STORAGE_KEY, theme) } catch { /* private mode: this visit only */ }
  window.dispatchEvent(new Event(CHANGE_EVENT))
}

/** Calls `onChange` whenever the shown theme may have changed (toggle, other tab, device setting). */
export function subscribeToTheme(onChange: () => void) {
  const media = window.matchMedia(DARK_QUERY)
  const fromOtherTab = (event: StorageEvent) => {
    if (event.key !== THEME_STORAGE_KEY) return
    if (event.newValue === 'light' || event.newValue === 'dark') document.documentElement.setAttribute('data-theme', event.newValue)
    else document.documentElement.removeAttribute('data-theme')
    onChange()
  }
  media.addEventListener('change', onChange)
  window.addEventListener(CHANGE_EVENT, onChange)
  window.addEventListener('storage', fromOtherTab)
  return () => {
    media.removeEventListener('change', onChange)
    window.removeEventListener(CHANGE_EVENT, onChange)
    window.removeEventListener('storage', fromOtherTab)
  }
}
