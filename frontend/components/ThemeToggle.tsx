'use client'

import { useSyncExternalStore, type MouseEvent } from 'react'
import { flushSync } from 'react-dom'
import { Moon, Sun } from 'lucide-react'
import { currentTheme, setTheme, subscribeToTheme, type Theme } from '../lib/theme'

const REVEAL_MS = 550
const REVEAL_EASING = 'cubic-bezier(0.22, 1, 0.36, 1)' // fast start, gentle finish

/**
 * Switches the theme with a circle that grows out of the button until it covers the screen.
 *
 * Uses the View Transitions API: the browser snapshots the page, the theme changes underneath,
 * and the new theme is revealed through an expanding clip-path. Browsers without the API, and
 * visitors who ask for reduced motion, get an instant switch instead.
 */
function switchTheme(next: Theme, event: MouseEvent<HTMLButtonElement>) {
  const root = document.documentElement
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (typeof document.startViewTransition !== 'function' || reduceMotion) {
    setTheme(next)
    return
  }

  // Grow from the centre of the button (keyboard activation has no pointer position).
  const button = event.currentTarget.getBoundingClientRect()
  const x = button.left + button.width / 2
  const y = button.top + button.height / 2
  // Far enough to reach the most distant corner of the viewport.
  const radius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))

  // Colour fades (header, buttons) would play inside the new snapshot and fight the reveal.
  root.classList.add('theme-switching')
  const transition = document.startViewTransition(() => {
    // Update the icon in the same frame, so the new snapshot already shows it.
    flushSync(() => setTheme(next))
  })
  transition.ready.then(() => {
    root.animate(
      { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
      { duration: REVEAL_MS, easing: REVEAL_EASING, pseudoElement: '::view-transition-new(root)' },
    )
  }).catch(() => { /* skipped (e.g. tab hidden): the theme has still changed */ })
  transition.finished.finally(() => root.classList.remove('theme-switching'))
}

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
      className={`${className} theme-toggle`}
      onClick={event => switchTheme(next, event)}
      aria-label={label}
      title={label}
      disabled={theme === null}
    >
      {/* Keyed so the icon remounts and plays its spin-in each time it changes. */}
      {theme === 'dark' ? <Sun key="sun" size={17} className="theme-icon" />
        : theme === 'light' ? <Moon key="moon" size={17} className="theme-icon" />
          : <span style={{ width: 17, height: 17 }} />}
      {withLabel && <span>{theme === 'dark' ? 'Light mode' : 'Dark mode'}</span>}
    </button>
  )
}
