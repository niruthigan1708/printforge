'use client'
import Link from 'next/link'
import { ArrowRight } from 'lucide-react'
import { FormEvent } from 'react'
import { Header } from '../../components/Header'
export default function RegisterPage() { function submit(event:FormEvent) { event.preventDefault() } return <main><Header/><section className="shell auth"><div className="eyebrow">Join the workshop</div><h1 className="page-title">Make room<br/>for <span style={{color:'var(--orange)'}}>better.</span></h1><form onSubmit={submit}><label>Name<input required placeholder="Your name"/></label><label>Email<input required type="email" placeholder="you@example.com"/></label><label>Password<input required minLength={8} type="password" placeholder="At least 8 characters"/></label><button className="btn orange" type="submit">Create account <ArrowRight size={15}/></button></form><p className="fine-print">Already have an account? <Link href="/login">Log in</Link></p></section></main> }
