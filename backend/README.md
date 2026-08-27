# PrintForge API

Spring Boot 3 / Java 21 REST API for PrintForge — authentication, product/category catalogue, orders, and the custom 3D-print workflow.

See the [root README](../README.md) for the full API reference, database design, and setup instructions. Quick start:

```bash
cp ../.env.example .env   # fill in your local DB password and a JWT secret
mvn spring-boot:run       # http://localhost:8080
mvn test                  # runs against an in-memory H2 database
```

Layered architecture: `controller` → `service` → `repository` → `entity`, with `dto` records at every HTTP boundary, `security` for JWT auth, `exception` for centralized error handling, and `config` for security rules, CORS, and demo data seeding.
