'use client'

import { useEffect } from 'react'

/** Blocks that fade up as they scroll into view. Add `data-reveal` to opt any other element in. */
const SELECTOR = [
  '.section-head',
  '.product-card',
  '.category-tile',
  '.step-card',
  '.cta-banner',
  '.dashboard-cards > *',
  '[data-reveal]',
].join(', ')

/** Delay between items that enter the viewport together (e.g. one row of product cards). */
const STAGGER_MS = 70
const MAX_STAGGER_STEPS = 6

/**
 * Scroll-triggered entrance animations, mounted once for the whole app.
 *
 * Progressive enhancement: content is only hidden once this has run (the `reveal-ready` class on
 * <html>), so without JavaScript, or with reduced motion requested, everything simply shows.
 * Elements already on screen when the page first loads are left alone, so they never flash out
 * and back in; content that arrives later (fetched product grids, client-side navigation) is
 * picked up by a MutationObserver before it is painted.
 */
export function ScrollReveal() {
  useEffect(() => {
    if (typeof IntersectionObserver === 'undefined') return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    const root = document.documentElement
    root.classList.add('reveal-ready')

    const observer = new IntersectionObserver(entries => {
      let step = 0
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        const el = entry.target as HTMLElement
        el.style.setProperty('--reveal-delay', `${Math.min(step++, MAX_STAGGER_STEPS) * STAGGER_MS}ms`)
        el.classList.add('is-revealed')
        observer.unobserve(el)
      }
    }, { rootMargin: '0px 0px -6% 0px', threshold: 0.08 })

    const seen = new WeakSet<Element>()
    const viewportHeight = () => window.innerHeight || root.clientHeight

    function scan(initial: boolean) {
      document.querySelectorAll(SELECTOR).forEach(el => {
        if (seen.has(el)) return
        seen.add(el)
        // Already painted on screen at first load: leave it visible.
        if (initial && el.getBoundingClientRect().top < viewportHeight()) return
        el.classList.add('reveal')
        observer.observe(el)
      })
    }

    scan(true)

    let queued = false
    const mutations = new MutationObserver(() => {
      if (queued) return
      queued = true
      // A microtask still runs before the browser paints the new nodes.
      queueMicrotask(() => { queued = false; scan(false) })
    })
    mutations.observe(document.body, { childList: true, subtree: true })

    return () => {
      observer.disconnect()
      mutations.disconnect()
      root.classList.remove('reveal-ready')
    }
  }, [])

  return null
}
