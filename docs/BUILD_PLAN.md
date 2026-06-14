# Full-Stack E-Commerce (Angular + Spring Boot) — Build Plan

> This is the working plan for the project, committed so it travels with the repo
> (visible in IntelliJ and any other checkout). Milestone 1 is implemented; later
> milestones are an outline.

## Context
Building the "Full Stack: Angular and Java Spring Boot E-Commerce Website" course project
on a modern stack: **Spring Boot 4.1 + Java 21** backend, **Angular 21 (standalone)** frontend.
We implement the course's *concepts* in modern idiom, so code won't match the videos
line-for-line.

## Repo layout
- `backend/` — Spring Boot app (Maven).
- `frontend/angular-ecommerce/` — Angular app.

## Environment notes (remote build sandbox where this was authored)
- JDK 21 (pom pins `<java.version>21</java.version>`); Node 22 / npm 10.
- Docker image pulls are blocked in the sandbox, so MySQL can't run there — the backend
  was verified with `./mvnw -DskipTests clean package`. **Full end-to-end runs locally**
  where Docker is available (see Verification).

## Database — MySQL only (course-faithful)
Docker MySQL via `backend/compose.yaml`. `spring-boot-docker-compose` auto-starts it on
`spring-boot:run`. Hibernate auto-creates tables (`ddl-auto=update`); sample data is seeded
by `DataSeeder` (`CommandLineRunner`) when the product table is empty.

---

## Milestone 1 — Product catalog end-to-end (DONE)

### Backend (`com.bob.ecommerceangularapp`)
- `entity/Product`, `entity/ProductCategory` (JPA, Lombok).
- `dao/ProductRepository` (Spring Data REST) with `findByCategoryId` and `findByNameContaining`
  (paginated); `dao/ProductCategoryRepository`.
- `config/MyDataRestConfig` — read-only exposure (no POST/PUT/DELETE/PATCH), `exposeIdsFor`,
  CORS for `http://localhost:4200`.
- `bootstrap/DataSeeder` — seeds Books / Coffee Mugs / Mouse Pads / Luggage + ~21 products.
- `application.properties` — datasource, `ddl-auto=update`, `spring.data.rest.base-path=/api`.

Endpoints: `/api/products`, `/api/products/{id}`,
`/api/products/search/findByCategoryId?id=&page=&size=`,
`/api/products/search/findByNameContaining?name=&page=&size=`, `/api/product-category`.

### Frontend (`frontend/angular-ecommerce`)
- `common/product.ts`, `common/product-category.ts`.
- `services/product.service.ts` — paginated list/search, categories, single product;
  unwraps `_embedded` + `page`. Base URL from `environments/environment.ts`.
- `components/` — `product-list` (grid + `NgbPagination` + page-size + empty state),
  `product-category-menu`, `search`, `product-details`.
- `app.routes.ts`, `app.config.ts` (`provideHttpClient`, `provideRouter`), root `app` shell
  (header + sidebar + outlet).

---

## Roadmap (later milestones — outline)
- **M2** Cart, checkout, save order (Country/State, Customer/Address/Order/OrderItem, `CheckoutController`).
- **M3** Security — Okta OIDC, `oauth2-resource-server`, protect `/api/orders`, Bearer interceptor.
- **M4** HTTPS — self-signed certs, `ng serve --ssl`, Boot PKCS12 on 8443, env files.
- **M5** Stripe — `stripe-java`, payment-intent endpoint, Stripe Elements.

---

## Verification

### Build (no DB needed)
- Backend: `cd backend && ./mvnw -DskipTests clean package`
- Frontend: `cd frontend/angular-ecommerce && npm install && npx ng build`

### Full end-to-end (local, needs Docker)
1. `cd backend && docker compose up -d` (or let `spring-boot-docker-compose` start it).
2. `cd backend && ./mvnw spring-boot:run` → `http://localhost:8080/api/products` returns
   seeded products with ids + pagination; test both `/search/...` endpoints.
3. `cd frontend/angular-ecommerce && npm start` → `http://localhost:4200`: grid renders,
   category sidebar filters, search works, pagination + page-size work, product detail opens,
   empty search shows "No products found."
