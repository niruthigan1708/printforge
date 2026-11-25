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
- Full product CRUD with stock and category management, plus a real photo upload per product
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

**Schema migrations:** the schema is owned by [Flyway](https://flywaydb.org/) migrations in `backend/src/main/resources/db/migration` and applied automatically on startup; Hibernate only validates that the entities match (`ddl-auto: validate`). To change the schema, add a new `V<n>__description.sql` file — never edit one that has already run. Databases created before Flyway was adopted are baselined at version 1 on first start and then receive later migrations. The test suite runs the same migrations on H2, so a migration that drifts from the entities fails the build.

**Order status:** `PENDING → CONFIRMED → PRINTING → READY → SHIPPED → DELIVERED`, or `CANCELLED`. Orders only move forward (steps may be skipped) and can be cancelled until they ship; cancelling returns the items to stock. `DELIVERED` and `CANCELLED` are final.

**Custom print status:** `SUBMITTED → UNDER_REVIEW → QUOTED → ACCEPTED → IN_PRODUCTION → COMPLETED`, or `REJECTED` / `CANCELLED`. `QUOTED` is reached only by sending a quote (which can be revised until the customer answers); `ACCEPTED` / `REJECTED` are set only by the customer; admins drive the remaining steps and can cancel until completion. `REJECTED`, `COMPLETED` and `CANCELLED` are final.

Order and request responses include `nextStatuses` — the moves the server will accept — so clients never need to duplicate these rules.

**Order and request numbers** (`PF-…`, `CR-…`) are derived from the database id, so they stay unique when orders are placed concurrently. Stock is decremented under a row lock, so simultaneous orders cannot oversell.

**Editing stock safely:** `PUT /api/products/{id}` accepts an optional `expectedStockQuantity` — the stock value the editor was loaded with. If the submitted `stockQuantity` equals it, the live stock is kept (so sales made while the form was open are not undone); if it differs and the stock has moved in the meantime, the update is rejected with `409` and the current value. The admin form always sends it; without it, `stockQuantity` overwrites the stock.

## Authentication

JWT bearer tokens, BCrypt password hashing. Public registration always creates a `CUSTOMER` — there is no way to self-register as `ADMIN`. Every protected endpoint checks the role embedded in the verified JWT server-side (Spring Security), never a role sent by the client. Invalid/expired/missing tokens return `401`; valid tokens without the right role return `403`.

## API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register (always `CUSTOMER`) |
| POST | `/api/auth/login` | Public | Log in, returns `{token, id, name, email, role}` |
| GET | `/api/products` | Public | List active products (`?search=` and `?category=` combine; `?ids=1,2,3` returns just those, max 100) |
| GET | `/api/products/{id}` | Public | Product detail (404 for inactive/archived products) |
| GET | `/api/admin/products/{id}` | ADMIN | Product detail including inactive products |
| GET | `/api/products/{id}/image` | Public | Product photo — serves the uploaded file, else redirects to the admin-set image URL, else a generated placeholder. Pass `?v=<imageVersion>` for a long-cached URL; unversioned requests are revalidated |
| POST | `/api/products/{id}/image` | ADMIN | Upload/replace a product's photo (multipart: `file`, max 5MB, JPG/PNG/WEBP/GIF) |
| POST/PUT/DELETE | `/api/products[/{id}]` | ADMIN | Manage products |
| GET | `/api/categories` | Public | List categories |
| GET | `/api/shop/settings` | Public | `{ deliveryFee, freeDeliveryThreshold }` used to price orders |
| POST/PUT/DELETE | `/api/categories[/{id}]` | ADMIN | Manage categories (delete blocked if products reference it) |
| POST | `/api/orders` | CUSTOMER | Place an order (validates stock, transactional). Optional `expectedTotal`: rejected with `409` if prices changed since the customer saw it |
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

See [`.env.example`](.env.example). The backend loads `backend/.env` automatically on startup (via `spring.config.import`), so no shell exports are needed; real environment variables take precedence over it. Never commit a real `.env` — both `backend/.env` and `frontend/.env.local` are gitignored.

| Variable | Used by | Purpose |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | backend | PostgreSQL connection |
| `JWT_SECRET` | backend | **Required.** HMAC signing key for JWTs — a long random value (≥32 bytes). The backend refuses to start if it is missing, too short, or left as the `.env.example` placeholder |
| `JWT_EXPIRATION` | backend | Token lifetime in ms (default 24h) |
| `UPLOAD_DIR` | backend | Where custom-print uploads are stored (default `backend/uploads/custom-prints`) |
| `PRODUCT_IMAGE_DIR` | backend | Where uploaded product photos are stored (default `backend/uploads/products`) |
| `DELIVERY_FEE` | backend | Flat delivery fee per order in Rs. (default `350`). The storefront reads it from `GET /api/shop/settings` |
| `FREE_DELIVERY_THRESHOLD` | backend | Orders with a subtotal at or above this ship free (default `5000`); `0` switches free delivery off |
| `MAX_FAILED_LOGINS_PER_ACCOUNT`, `MAX_FAILED_LOGINS_PER_IP`, `LOGIN_LOCKOUT_WINDOW` | backend | Login throttling (defaults `5`, `20`, `15m`): after that many failed logins within the window, further attempts get `429` with `Retry-After` |
| `CORS_ALLOWED_ORIGINS` | backend | Comma-separated origins the browser may call the API from (default `http://localhost:3000`); set to your deployed frontend URL |
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
