export type Product = { id:number; name:string; category:string; price:number; material:string; image:string; tone:string; description?:string }

export const products: Product[] = [
  { id:1, name:'Arc Phone Stand', category:'Phone Accessories', price:1850, material:'PLA', tone:'#cfe9ff', image:'https://images.unsplash.com/photo-1601972602237-8c79241e468b?auto=format&fit=crop&w=700&q=80', description:'A calm, considered stand for your daily screen time.' },
  { id:2, name:'Grid Desk Organizer', category:'Organization', price:2650, material:'PETG', tone:'#d7f36b', image:'https://images.unsplash.com/photo-1593642532400-2682810df593?auto=format&fit=crop&w=700&q=80', description:'A modular home for the small things that make work work.' },
  { id:3, name:'Orbit Headphone Hook', category:'Desk Accessories', price:2200, material:'PLA', tone:'#e9e0d5', image:'https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&w=700&q=80', description:'A clean hook that keeps your headphones close and your desk clear.' },
  { id:4, name:'Terrain Tile Set', category:'Miniatures', price:4200, material:'PLA', tone:'#ffb29f', image:'https://images.unsplash.com/photo-1577083552431-6e5fd01988b5?auto=format&fit=crop&w=700&q=80', description:'Four textured tiles for tabletop worlds with somewhere to go.' },
]
export const money = (value:number) => `Rs. ${value.toLocaleString('en-LK')}`
