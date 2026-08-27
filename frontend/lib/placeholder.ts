const TONES: [string, string][] = [
  ['#ff5c35', '#ffffff'],
  ['#d7f36b', '#171717'],
  ['#cfe9ff', '#171717'],
  ['#e9e0d5', '#171717'],
  ['#171717', '#ffffff'],
]

function wrap(text: string, maxCharsPerLine: number) {
  const lines: string[] = []
  let current = ''
  for (const word of text.split(' ')) {
    if (current.length > 0 && current.length + 1 + word.length > maxCharsPerLine) { lines.push(current); current = '' }
    current = current.length > 0 ? `${current} ${word}` : word
  }
  if (current.length > 0) lines.push(current)
  return lines
}

/** Self-contained SVG placeholder as a data URI — never depends on an external image host. */
export function placeholderImage(label: string, seed = 0): string {
  const [bg, fg] = TONES[Math.abs(seed) % TONES.length]
  const escaped = label.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const lines = wrap(escaped, 16)
  const startY = 300 - (lines.length - 1) * 20
  const tspans = lines.map((line, i) => `<tspan x="300" y="${startY + i * 40}">${line}</tspan>`).join('')
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600" viewBox="0 0 600 600"><rect width="600" height="600" fill="${bg}"/><text font-family="Arial, sans-serif" font-size="32" font-weight="700" fill="${fg}" text-anchor="middle" dominant-baseline="middle">${tspans}</text></svg>`
  if (typeof window === 'undefined') return `data:image/svg+xml;base64,${Buffer.from(svg, 'utf-8').toString('base64')}`
  return `data:image/svg+xml;base64,${window.btoa(unescape(encodeURIComponent(svg)))}`
}
