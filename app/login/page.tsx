'use client'
import Link from 'next/link'
import { ArrowRight } from 'lucide-react'
import { FormEvent } from 'react'
import { Header } from '../../components/Header'
export default function LoginPage() { function submit(event:FormEvent) { event.preventDefault() } return <main><Header/><section className="shell auth"><div className="eyebrow">Welcome back</div><h1 className="page-title">Good to<br/><span style={{color:'var(--orange)'}}>see you.</span></h1><form onSubmit={submit}><label>Email<input required type="email" placeholder="you@example.com"/></label><label>Password<input required type="password" placeholder="Your password"/></label><button className="btn orange" type="submit">Log in <ArrowRight size={15}/></button></form><p className="fine-print">New to PrintForge? <Link href="/register">Create an account</Link></p></section></main> }
