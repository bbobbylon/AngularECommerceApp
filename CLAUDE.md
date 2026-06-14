# AngularECommerceApp — Claude Code guide

Full-stack e-commerce app (Udemy course project) on a modern stack:
**Spring Boot 4.1 + Java 21** backend, **Angular 21 (standalone)** frontend.

## Source of truth for the build
Read **`docs/BUILD_PLAN.md`** before starting work. It has the full milestone
plan, locked decisions (MySQL-only, repo layout), and verification steps.

## Current state
- ✅ **Milestone 0** — project setup/cleanup (backend/ + frontend/ split, clean pom, compose.yaml).
- ✅ **Milestone 1** — product catalog end-to-end (entities, Spring Data REST repos,
  CORS/exposure config, data seeder; Angular product list/details/search/category + pagination).
- ⬜ **Milestone 2 (next)** — cart, checkout, save order. See the Roadmap in BUILD_PLAN.md.

## Layout
- `backend/` — Spring Boot (Maven). Package root `com.bob.ecommerceangularapp`.
- `frontend/angular-ecommerce/` — Angular 21 standalone app.

## Commands
- Backend build: `cd backend && ./mvnw -DskipTests clean package`
- Backend run (needs Docker for MySQL): `cd backend && ./mvnw spring-boot:run`
- Frontend build: `cd frontend/angular-ecommerce && npm install && npx ng build`
- Frontend dev server: `cd frontend/angular-ecommerce && npm start` (→ http://localhost:4200)

## Conventions
- Java 21 (pom pins `<java.version>21</java.version>`); don't reintroduce the removed
  starters (ldap/saml2/batch/mail/webflux/postgres) — see BUILD_PLAN.md §0.2.
- API base path is `/api` (Spring Data REST). Frontend reads `environment.apiUrl`.
- MySQL only, course-faithful; Hibernate `ddl-auto=update`; data seeded via `CommandLineRunner`.
