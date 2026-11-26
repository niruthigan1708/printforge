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

const KEYFRAMES: Keyframe[] = [
  { opacity: 0, transform: 'translateY(22px)' },
  { opacity: 1, transform: 'none' },
]
const DURATION_MS = 600
const EASING = 'cubic-bezier(0.16, 1, 0.3, 1)'

/** Delay between items that enter the viewport together (e.g. one row of product cards). */
const STAGGER_MS = 70
const MAX_STAGGER_STEPS = 6

/**
 * Scroll-triggered entrance animations, mounted once for the whole app.
 *
 * Uses the Web Animations API rather than classes or inline styles: React-rendered elements may
 * not have hydrated yet when they are found, and changing their attributes would make hydration
 * see different HTML from the server's. An animation changes no attribute. Each element is held
 * at the first frame (hidden) until it scrolls into view, then plays; once finished the animation
 * is removed, so the element's own CSS (hover lifts and so on) applies again.
 *
 * Progressive enhancement: nothing is hidden without JavaScript, or with reduced motion
 * requested. Elements already on screen at first load are left alone so they never flash out and
 * back in; content that arrives later (fetched product grids, client-side navigation) is picked
 * up by a MutationObserver before it is painted.
 */
export function ScrollReveal() {
  useEffect(() => {
    if (typeof IntersectionObserver === 'undefined' || typeof Element.prototype.animate !== 'function') return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    const pending = new Map<Element, Animation>()

    const observer = new IntersectionObserver(entries => {
      let step = 0
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        const animation = pending.get(entry.target)
        observer.unobserve(entry.target)
        pending.delete(entry.target)
        if (!animation) continue
        animation.effect?.updateTiming({ delay: Math.min(step++, MAX_STAGGER_STEPS) * STAGGER_MS })
        animation.play()
        animation.finished.then(() => animation.cancel(), () => {})
      }
    }, { rootMargin: '0px 0px -6% 0px', threshold: 0.08 })

    const seen = new WeakSet<Element>()
    const viewportHeight = () => window.innerHeight || document.documentElement.clientHeight

    function scan(initial: boolean) {
      document.querySelectorAll(SELECTOR).forEach(el => {
        if (seen.has(el)) return
        seen.add(el)
        // Already painted on screen at first load: leave it visible.
        if (initial && el.getBoundingClientRect().top < viewportHeight()) return
        const animation = el.animate(KEYFRAMES, { duration: DURATION_MS, easing: EASING, fill: 'backwards' })
        animation.pause() // held at the first (hidden) frame until it scrolls into view
        pending.set(el, animation)
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
      pending.forEach(animation => animation.cancel())
      pending.clear()
    }
  }, [])

  return null
}
