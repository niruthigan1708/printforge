# PrintForge

**Custom ideas. Physical creations.**

PrintForge is a portfolio-style MVP for a Sri Lankan 3D-printing studio: a considered collection of useful objects, plus a clear path for customers to request a custom print.

## Current MVP

- Responsive product-led storefront
- Ready-to-print collection with Sri Lankan Rupee pricing
- Add/remove cart drawer with subtotal calculation
- Product catalogue route with search and category filtering
- Cart, checkout, order confirmation, login, and registration screens
- Custom printing form with STL / 3MF file selection and client validation
- Spring Boot API foundation with PostgreSQL-ready product and category endpoints
- Environment-driven configuration with no committed secrets

## Run

```bash
npm install
npm run dev
```

Open `http://localhost:3000`.

Available customer routes: `/products`, `/cart`, `/checkout`, `/custom-print`, `/login`, and `/register`.

The backend requires Java 21, Maven 3.9+, and PostgreSQL. Copy `.env.example` to `.env` and fill in local values, then run `mvn spring-boot:run` inside `backend/`.

## API

- `GET /api/products`
- `GET /api/products?search=stand`
- `GET /api/products?category=Gaming`
- `GET /api/products/{id}`
- `GET /api/categories`

Product responses are DTOs, keeping JPA entities out of the HTTP contract.

## Architecture

The backend is separated into `controller`, `service`, `repository`, `entity`, `dto`, and `config`. The frontend uses the Next.js App Router and keeps the initial shopping interaction local so the storefront remains demonstrable before authentication and checkout persistence are connected.

## Future slices

JWT/BCrypt authentication, transactional orders with stock checks, local custom-print uploads, admin product/order/request management, and focused service tests. Payment gateways, automated STL analysis, cloud infrastructure, and other non-MVP complexity are deliberately excluded.
