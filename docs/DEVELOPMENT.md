# Luv2Shop — Development Guide

Everything you need to build, run, test, and contribute.

## Prerequisites
- **JDK 21+**, **Node 20+**, **Docker** (for MySQL), Git Bash (Windows).

## Repository layout
```
ecommerceAngularApp/
├─ backend/                  Spring Boot API (Maven)
│  ├─ src/main/java/com/bob/ecommerceangularapp/   entity · dao · dto · service · controller · config · bootstrap
│  ├─ src/test/java/...      JUnit 5 + H2 tests
│  ├─ compose.yaml           MySQL 8 (port 3307)
│  └─ schema.sql             generated DDL (manual DB provisioning)
├─ frontend/angular-ecommerce/   Angular 21 standalone app
│  └─ src/app/               components · services · validators · interceptors · auth · common (models)
├─ docs/                     architecture, API, Stripe, build plan (this folder)
├─ run.sh                    one-command build + run + open
└─ CLAUDE.md / README.md
```

## Quick start
```bash
./run.sh        # builds both, starts them, opens http://localhost:4250
                # Ctrl+C stops everything
```

## Build & test
```bash
# Backend — compiles, runs tests against in-memory H2 (no Docker needed), packages a jar
cd backend && ./mvnw clean package

# Frontend — production build + unit tests (Vitest)
cd frontend/angular-ecommerce && npx ng build
cd frontend/angular-ecommerce && CI=true npx ng test --watch=false
```

## Ports (intentionally non-default)
| Service | Port |
|---|---|
| Frontend (Angular dev server) | **4250** |
| Backend (Spring Boot) | **8585** |
| MySQL (Docker) | **3307** |

## Database
- `spring-boot-docker-compose` auto-starts MySQL on `:3307` when you run the backend.
- Inspect it in MySQL Workbench: host `127.0.0.1`, port **3307**, user `ecommerceapp` / `ecommerceapp`
  (or `root` / `verysecret`), schema `full-stack-ecommerce`.
- To provision a DB by hand, run [`backend/schema.sql`](../backend/schema.sql) (or `seed-database.sql`
  if present) against your MySQL.
- Schema is created automatically via Hibernate `ddl-auto=update`; data via `DataLoader`.

## Enabling the optional integrations
- **Okta (login / order history):** see [BUILD_PLAN.md](BUILD_PLAN.md) §M3 — set the Angular
  `issuer`/`clientId` and the backend `issuer-uri`.
- **Stripe (payments):** see [STRIPE.md](STRIPE.md) — set `STRIPE_SECRET_KEY` + `stripePublishableKey`.
- **HTTPS:** see the commented block in `backend/src/main/resources/application.properties`.

## Conventions
- **Backend:** Java 21; package-by-feature under `com.bob.ecommerceangularapp`; constructor injection;
  Lombok for entities/DTOs; keep auto-exposed repos read-only via `MyDataRestConfig`.
- **Frontend:** Angular standalone components (2025 file-naming: `product-list.ts`, not
  `.component.ts`); services are `providedIn: 'root'`; prefer signals for local state; new control
  flow (`@if`/`@for`).
- **API base path** is `/api`; the frontend reads `environment.apiUrl`.

## Definition of done
A change isn't done until:
1. `./mvnw clean package` is green (backend builds **and** tests pass), and
2. `ng build` is clean **and** `ng test` passes, and
3. the behavior is **verified in a running browser** — not just "it compiles" or "the API responds."
   (Hard lesson: a missing runtime polyfill once broke the product grid while the build and API were
   both perfectly green. Always look at the rendered UI.)

## Troubleshooting
- **Empty page / "no products found" but the API has data** → check the browser console. A runtime
  error (e.g. a missing polyfill) can abort a render branch silently. The `@angular/localize/init`
  polyfill is required because `ng-bootstrap` pagination uses `$localize`.
- **Workbench can't connect as `root`** → you're likely on `:3306` (your local MySQL), not `:3307`
  (the app's Docker MySQL). Use port **3307**.
- **`./run.sh` won't bind a port** → something's already on 8585/4250; stop it first.
