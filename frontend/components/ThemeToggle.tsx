'use client'

import { useSyncExternalStore, type MouseEvent } from 'react'
import { flushSync } from 'react-dom'
import { Moon, Sun } from 'lucide-react'
import { currentTheme, setTheme, subscribeToTheme, type Theme } from '../lib/theme'

const REVEAL_MS = 1400
const REVEAL_EASING = 'cubic-bezier(0.55, 0, 0.45, 1)' // ease in and out, so the glow spreads evenly instead of rushing out
/** Share of the reveal mask's radius that is fully the new theme (the 30% stop in globals.css). */
const SOLID_SHARE = 0.3

/**
 * Switches the theme with a soft glow that grows out of the button until the new theme covers the
 * screen, like light (or shade) spreading from the sun or moon.
 *
 * Uses the View Transitions API: the browser snapshots the page, the theme changes underneath,
 * and the new theme fades in through an expanding mask (see globals.css). Browsers without the
 * API, and visitors who ask for reduced motion, get an instant switch instead.
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

  // Going dark, the dark glow spreads out of the moon. Going light is the reverse: the dark glow
  // gathers back into the sun, uncovering the light page beneath it.
  const collapse = next === 'light'

  // Colour fades (header, buttons) would play inside the new snapshot and fight the reveal.
  // theme-collapse puts the old (dark) snapshot on top and masks it instead (see globals.css).
  // At full size the mask's solid centre (SOLID_SHARE of its radius) reaches past the farthest
  // corner, so the glow covers the whole screen.
  const size = (2 * radius) / SOLID_SHARE
  const small = { maskSize: '0px 0px', maskPosition: `${x}px ${y}px` }
  const full = { maskSize: `${size}px ${size}px`, maskPosition: `${x - size / 2}px ${y - size / 2}px` }
  root.style.setProperty('--reveal-full-size', full.maskSize)
  root.style.setProperty('--reveal-full-position', full.maskPosition)

  root.classList.add('theme-switching')
  if (collapse) root.classList.add('theme-collapse')
  const transition = document.startViewTransition(() => {
    // Update the icon in the same frame, so the new snapshot already shows it.
    flushSync(() => setTheme(next))
  })
  transition.ready.then(() => {
    root.animate(collapse ? [full, small] : [small, full], {
      duration: REVEAL_MS,
      easing: REVEAL_EASING,
      pseudoElement: collapse ? '::view-transition-old(root)' : '::view-transition-new(root)',
      fill: 'both',
    })
  }).catch(() => { /* skipped (e.g. tab hidden): the theme has still changed */ })
  transition.finished.finally(() => {
    root.classList.remove('theme-switching', 'theme-collapse')
    root.style.removeProperty('--reveal-full-size')
    root.style.removeProperty('--reveal-full-position')
  })
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
