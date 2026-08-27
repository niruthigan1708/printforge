import { ProductForm } from '../../../../components/admin/ProductForm'

export default function NewProductPage() {
  return (
    <>
      <h1 className="admin-title">Add product</h1>
      <p className="hero-copy" style={{ marginTop: -10, marginBottom: 20 }}>Save the product first, then open it again to upload a photo.</p>
      <ProductForm />
    </>
  )
}
