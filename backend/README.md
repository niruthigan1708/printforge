# PrintForge API

Spring Boot 3 / Java 21 API foundation for the PrintForge storefront.

Current slice: PostgreSQL-backed categories and products, DTO responses, simple search/category filtering, CORS, environment-driven configuration, and BCrypt/JWT authentication. Orders, custom-print workflow, admin mutations, and file storage are the next implementation slices.

Run from this directory with `mvn spring-boot:run`. The API listens on `http://localhost:8080`.

Auth endpoints are `POST /api/auth/register` and `POST /api/auth/login`. Public registration always assigns the `CUSTOMER` role.
