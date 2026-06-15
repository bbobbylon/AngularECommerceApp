# Luv2Shop — Full-Stack E-Commerce (Angular + Spring Boot)

A modern build of the Udemy *"Full Stack: Angular and Spring Boot E-Commerce"* course
project, on a current stack: **Spring Boot 4.1 / Java 21** and **Angular 21 (standalone)**.

## Stack
- **Backend:** Spring Boot 4.1, Spring Data JPA + REST, Spring Security (OAuth2 resource
  server), MySQL, Stripe (`stripe-java`).
- **Frontend:** Angular 21 standalone, Bootstrap 5, ng-bootstrap, Okta (`@okta/okta-angular`),
  Stripe Elements (`@stripe/stripe-js`).

## Layout
- `backend/` — Spring Boot API (package `com.bob.ecommerceangularapp`).
- `frontend/angular-ecommerce/` — Angular app.
- `docs/BUILD_PLAN.md` — milestone plan + how to enable Okta / Stripe / HTTPS.
- `CLAUDE.md` — onboarding guide and conventions.

## Features
- **Catalog** — product grid, keyword search, category filter, server-side pagination,
  product detail, loading + empty states.
- **Cart & checkout** — `sessionStorage` cart with live totals, reactive checkout
  (country/state cascade, copy-shipping-to-billing), order persistence, confirmation page.
- **Security** — Okta OIDC protecting order history. *Gated on config:* the app runs without it.
- **Payments** — Stripe payment intents + card Element. *Gated on config.*
- **HTTPS** — opt-in TLS (documented in `docs/BUILD_PLAN.md`).

Okta and Stripe degrade gracefully — the app builds and runs (catalog → cart → checkout)
with placeholder config, and lights up the moment real keys are supplied.

## Run locally
Prerequisites: **JDK 21+**, **Node 20+**, **Docker** (for MySQL).

```bash
# Backend — spring-boot-docker-compose auto-starts MySQL
cd backend && ./mvnw spring-boot:run            # http://localhost:8080/api/products

# Frontend
cd frontend/angular-ecommerce
npm install && npm start                        # http://localhost:4200
```

## Build & test
```bash
# Backend: compiles, runs tests against in-memory H2 (no Docker needed), packages a jar
cd backend && ./mvnw clean package

# Frontend: production build + unit tests
cd frontend/angular-ecommerce && npx ng build
cd frontend/angular-ecommerce && CI=true npx ng test --watch=false
```

See **`docs/BUILD_PLAN.md`** for the milestone breakdown and the steps to enable Okta,
Stripe, and HTTPS.
