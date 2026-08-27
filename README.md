# PrintForge

**Custom ideas. Physical creations.**

PrintForge is a full-stack e-commerce platform for a 3D-printing studio. Customers can shop a ready-made catalogue of printed objects, or upload their own STL/3MF/OBJ file and request a custom print that an admin reviews, quotes, and moves through production.

Built as a realistic, portfolio-quality MVP — clean layered architecture, real authentication and authorization, no unnecessary complexity (no microservices, no payment gateway, no automated STL analysis).

## Features

**Customer**
- Register / log in (JWT, BCrypt-hashed passwords)
- Browse, search, filter, and sort the product catalogue
- Product detail pages with related products
- Cart with quantity controls, persisted locally
- Checkout with cash-on-delivery / demo payment, server-side stock validation
- Order history with a visual status tracker (Placed → Confirmed → Printing → Ready → Shipped → Delivered)
- Upload a custom 3D model (STL / 3MF / OBJ, up to 20MB) with material, color, quantity, and notes
- View custom print request status, accept or reject an admin quote

**Admin**
- Dashboard with key stats (products, orders, pending orders, custom requests, revenue) and recent activity
- Full product CRUD with stock and category management
- Category CRUD (delete blocked while products still reference it)
- Order list and status management
- Custom print request review: download the uploaded file, send a quote, update production status

## Tech Stack

**Backend:** Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security (JWT), Bean Validation, Lombok, Maven
**Database:** PostgreSQL
**Frontend:** Next.js (App Router), React, TypeScript, hand-rolled CSS design system (no UI framework)
**Testing:** JUnit 5, Spring Boot Test, MockMvc, H2 (in-memory, test-only)

## Architecture

```
PrintForge/
├── backend/     Spring Boot API (controller → service → repository → entity, dto, security, exception, config)
├── frontend/    Next.js App Router client (app, components, lib, hooks via React state/context)
└── screenshots/
```

The backend never exposes JPA entities over HTTP — every endpoint returns a DTO. Role-based access is enforced server-side with Spring Security (`@PreAuthorize` + request matchers); the frontend never dictates permissions, it just reflects what the API allows. The frontend keeps cart state in `localStorage` (no persistent cart table) and calls the API directly with a small `fetch` wrapper — no data-fetching library was needed at this scale.

## Database Design

| Table | Notes |
|---|---|
| `users` | `id, name, email (unique), password (BCrypt), role, created_at` |
| `category` | `id, name (unique), description` |
| `product` | `id, name, description, price, stock_quantity, material, color, image_url, category_id, active, created_at, updated_at` |
| `customer_orders` | `id, order_number (PF-1001…), customer_id, subtotal, delivery_fee, total_amount, status, shipping_*, created_at, updated_at` |
| `order_item` | `id, order_id, product_id, product_name, quantity, unit_price, subtotal` — product name/price are snapshotted so past orders stay correct if a product changes later |
| `custom_print_request` | `id, request_number (CR-1001…), customer_id, file_name, file_path, file_type, material, color, quantity, notes, status, admin_quote, admin_notes, created_at, updated_at` |

Relationships: one customer → many orders and many custom print requests; one order → many order items; one category → many products.

**Order status:** `PENDING → CONFIRMED → PRINTING → READY → SHIPPED → DELIVERED`, or `CANCELLED`.
**Custom print status:** `SUBMITTED → UNDER_REVIEW → QUOTED → ACCEPTED → IN_PRODUCTION → COMPLETED`, or `REJECTED` / `CANCELLED`.

## Authentication

JWT bearer tokens, BCrypt password hashing. Public registration always creates a `CUSTOMER` — there is no way to self-register as `ADMIN`. Every protected endpoint checks the role embedded in the verified JWT server-side (Spring Security), never a role sent by the client. Invalid/expired/missing tokens return `401`; valid tokens without the right role return `403`.

## API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register (always `CUSTOMER`) |
| POST | `/api/auth/login` | Public | Log in, returns `{token, id, name, email, role}` |
| GET | `/api/products` | Public | List products (`?search=`, `?category=`) |
| GET | `/api/products/{id}` | Public | Product detail |
| POST/PUT/DELETE | `/api/products[/{id}]` | ADMIN | Manage products |
| GET | `/api/categories` | Public | List categories |
| POST/PUT/DELETE | `/api/categories[/{id}]` | ADMIN | Manage categories (delete blocked if products reference it) |
| POST | `/api/orders` | CUSTOMER | Place an order (validates stock, transactional) |
| GET | `/api/orders` / `/api/orders/{id}` | CUSTOMER | Own orders only |
| GET | `/api/admin/orders` | ADMIN | All orders |
| PUT | `/api/admin/orders/{id}/status` | ADMIN | Update order status |
| POST | `/api/custom-prints` | CUSTOMER | Submit a custom print request (multipart: `file, material, color, quantity, notes`) |
| GET | `/api/custom-prints` / `/{id}` | CUSTOMER | Own requests only |
| PUT | `/api/custom-prints/{id}/accept` \| `/reject` | CUSTOMER | Respond to a quote |
| GET | `/api/admin/custom-prints` / `/{id}` | ADMIN | All requests |
| GET | `/api/admin/custom-prints/{id}/file` | ADMIN | Download the uploaded model |
| PUT | `/api/admin/custom-prints/{id}/quote` | ADMIN | Send a quote (`amount, notes`) |
| PUT | `/api/admin/custom-prints/{id}/status` | ADMIN | Update production status |
| GET | `/api/admin/dashboard` | ADMIN | Summary stats + recent activity |

Errors are returned as consistent JSON: `{ "message", "status", "timestamp" }` (validation errors also include a per-field `errors` map). Stack traces are never exposed to clients.

## How to Run

**Prerequisites:** Java 21, Maven, Node.js, PostgreSQL running locally.

```bash
# 1. Create an empty database (name must match DB_NAME below)
createdb printforge   # or: psql -U postgres -c "CREATE DATABASE printforge"

# 2. Backend
cd backend
cp ../.env.example .env   # then fill in your real DB password and a JWT secret
mvn spring-boot:run       # http://localhost:8080

# 3. Frontend (separate terminal)
cd frontend
npm install
echo "NEXT_PUBLIC_API_URL=http://localhost:8080/api" > .env.local
npm run dev                # http://localhost:3000
```

Demo data (8 categories, 15 products, 1 admin, 1 customer) seeds automatically on first boot against an empty database.

**Run backend tests:** `cd backend && mvn test` (uses an in-memory H2 database, no PostgreSQL required).

> **Why `printforge` and not `campusconnect`?** The environment's `campusconnect` database already holds an unrelated project's tables and a `users.role` check constraint incompatible with PrintForge's `CUSTOMER`/`ADMIN` roles. PrintForge uses its own dedicated database to avoid corrupting that project.

## Environment Variables

See [`.env.example`](.env.example). Never commit a real `.env` — both `backend/.env` and `frontend/.env.local` are gitignored.

| Variable | Used by | Purpose |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | backend | PostgreSQL connection |
| `JWT_SECRET` | backend | HMAC signing key for JWTs — set to a long random value |
| `JWT_EXPIRATION` | backend | Token lifetime in ms (default 24h) |
| `UPLOAD_DIR` | backend | Where custom-print uploads are stored (default `backend/uploads/custom-prints`) |
| `NEXT_PUBLIC_API_URL` | frontend | Base URL the browser calls (default `http://localhost:8080/api`) |

## Demo Accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin@printforge.com` | `Admin@12345` |
| Customer | `customer@printforge.com` | `Customer@12345` |

Seeded by `DataSeeder` on first boot (only when the `users` table is empty); disable with `app.seed.enabled=false`.

## Future Improvements

Deliberately out of scope for this MVP: Stripe/PayPal integration, cloud file storage, in-browser STL/3MF preview, automated model analysis or pricing, product reviews, wishlists, coupons, email notifications, shipping-carrier integration, multiple images per product, a production queue view, customer notifications, and a 3D product configurator.

## Project Structure

```
PrintForge/
├── backend/
│   ├── src/main/java/com/printforge/
│   │   ├── controller/   REST endpoints
│   │   ├── service/      business logic, transactions
│   │   ├── repository/   Spring Data JPA
│   │   ├── entity/       JPA entities
│   │   ├── dto/          request/response records (never expose entities)
│   │   ├── security/     JWT filter + service
│   │   ├── exception/    centralized error handling
│   │   └── config/       security, CORS, seed data
│   └── src/test/java/    JUnit + MockMvc tests
├── frontend/
│   ├── app/               Next.js App Router pages (customer + /admin)
│   ├── components/        Header, ProductCard, StatusBadge, OrderTracker, admin forms
│   └── lib/                api client, auth/cart context, types, route guards
├── screenshots/
└── .env.example
```
