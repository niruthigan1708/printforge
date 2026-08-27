'use client'
import Link from 'next/link'
import { ArrowLeft, Check } from 'lucide-react'
import { FormEvent, useState } from 'react'
import { Header } from '../../components/Header'
import { money } from '../../lib/types'
import { useCart } from '../../lib/store'

export default function CheckoutPage() {
  const { cart, clear } = useCart(); const [placed, setPlaced] = useState(false)
  const subtotal = cart.reduce((sum, item) => sum + item.price, 0); const total = subtotal + (cart.length ? 350 : 0)
  function submit(event:FormEvent) { event.preventDefault(); clear(); setPlaced(true) }
  if (placed) return <main><Header/><section className="shell success-page"><Check size={32}/><div className="eyebrow">Order confirmed / PF-1001</div><h1 className="page-title">It is on<br/><span style={{color:'var(--orange)'}}>its way.</span></h1><p className="hero-copy">Order placed successfully. We will send a confirmation when your pieces move into production.</p><Link className="btn" href="/orders">View your order</Link></section></main>
  return <main><Header/><section className="shell page-intro compact"><div className="eyebrow">Checkout / cash on delivery</div><h1 className="page-title">Nearly<br/><span style={{color:'var(--orange)'}}>there.</span></h1></section><section className="shell checkout-grid"><form onSubmit={submit}><div className="form-two"><label>Full name<input required placeholder="Your name"/></label><label>Email<input required type="email" placeholder="you@example.com"/></label></div><label>Phone<input required placeholder="07X XXX XXXX"/></label><label>Address<input required placeholder="Street address"/></label><div className="form-two"><label>City<input required placeholder="Colombo"/></label><label>Postal code<input required placeholder="00100"/></label></div><button className="btn orange" type="submit">Place order</button></form><aside className="summary"><div className="eyebrow">Order summary</div><h2>{cart.length} pieces</h2><div className="summary-line"><span>Subtotal</span><b>{money(subtotal)}</b></div><div className="summary-line"><span>Delivery</span><b>{money(cart.length ? 350 : 0)}</b></div><div className="summary-total"><span>Total</span><b>{money(total)}</b></div><Link href="/cart" className="section-note"><ArrowLeft size={14}/> Edit cart</Link></aside></section></main>
}
