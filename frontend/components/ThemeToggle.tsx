'use client'

import { useSyncExternalStore } from 'react'
import { Moon, Sun } from 'lucide-react'
import { currentTheme, setTheme, subscribeToTheme } from '../lib/theme'

/**
 * Switches between light and dark. The server cannot know the visitor's theme, so it renders a
 * neutral placeholder and the icon appears once the browser has hydrated (no hydration mismatch).
 */
export function ThemeToggle({ className = 'icon-btn', withLabel = false }: { className?: string; withLabel?: boolean }) {
  const theme = useSyncExternalStore(subscribeToTheme, currentTheme, () => null)
  const next = theme === 'dark' ? 'light' : 'dark'
  const label = `Switch to ${next} mode`

  return (
    <button
      type="button"
      className={className}
      onClick={() => setTheme(next)}
      aria-label={label}
      title={label}
      disabled={theme === null}
    >
      {theme === 'dark' ? <Sun size={17} /> : theme === 'light' ? <Moon size={17} /> : <span style={{ width: 17, height: 17 }} />}
      {withLabel && <span>{theme === 'dark' ? 'Light mode' : 'Dark mode'}</span>}
    </button>
  )
}
