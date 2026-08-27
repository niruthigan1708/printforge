'use client'
import Link from 'next/link'
import { ArrowLeft, Minus, Plus, X } from 'lucide-react'
import { Header } from '../../components/Header'
import { money } from '../../lib/types'
import { useCart } from '../../lib/store'

export default function CartPage() {
  const { cart, remove } = useCart()
  const subtotal = cart.reduce((sum, item) => sum + item.price, 0)
  const delivery = cart.length ? 350 : 0
  return <main><Header/><section className="shell page-intro compact"><div className="eyebrow">Your selection / {cart.length} pieces</div><h1 className="page-title">The good<br/><span style={{color:'var(--orange)'}}>stuff.</span></h1></section><section className="shell checkout-grid"><div>{cart.length === 0 ? <div className="empty"><h2>Your cart is empty.</h2><p>There is always room for one more useful object.</p><Link className="btn" href="/products">Browse products <ArrowLeft size={15}/></Link></div> : cart.map((item, index) => <div className="cart-row" key={`${item.id}-${index}`}><div className="cart-thumb" style={{background:item.tone}}><img src={item.image} alt=""/></div><div className="cart-info"><b>{item.name}</b><span>{item.category} · {item.material}</span><strong>{money(item.price)}</strong></div><div className="quantity"><button aria-label="Decrease quantity"><Minus size={14}/></button><span>1</span><button aria-label="Increase quantity"><Plus size={14}/></button></div><button className="icon-btn" aria-label={`Remove ${item.name}`} onClick={() => remove(index)}><X size={16}/></button></div>)}</div>{cart.length > 0 && <aside className="summary"><div className="eyebrow">Order summary</div><h2>Ready when you are.</h2><div className="summary-line"><span>Subtotal</span><b>{money(subtotal)}</b></div><div className="summary-line"><span>Delivery</span><b>{money(delivery)}</b></div><div className="summary-total"><span>Total</span><b>{money(subtotal + delivery)}</b></div><Link className="btn orange" href="/checkout">Proceed to checkout</Link><p className="fine-print">Demo payment / Cash on delivery</p></aside>}</section></main>
}
