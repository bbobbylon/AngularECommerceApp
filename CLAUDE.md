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
- ✅ **Milestone 2** — cart + checkout + save order (CartService w/ sessionStorage, cart status/details,
  reactive checkout, Country/State + Customer/Address/Order/OrderItem, `POST /api/checkout/purchase`).
- ✅ **Milestone 3** — security (Okta OIDC). OAuth2 resource server protects `GET /api/orders/**`
  *only when* an issuer URI is set; Angular login-status + auth interceptor + guarded order history.
- ✅ **Milestone 5** — Stripe payments. `POST /api/checkout/payment-intent` (stripe-java) + Stripe
  Elements card in the checkout. Needs a Stripe test key at runtime.
- 📄 **Milestone 4** — HTTPS is documented as opt-in config (commented block in `application.properties`
  + steps in BUILD_PLAN.md); plain HTTP stays the default so local dev isn't disrupted.
- ✅ **Milestone 6** — Email + account settings + storefront polish. `spring-boot-starter-mail` +
  `@EnableScheduling`; gated `EmailService`/`EmailTemplates` (welcome, order confirmation, settings,
  weekly blast); `NewsletterSubscriber` + `Customer.newsletterSubscribed`/`unsubscribeToken`;
  `Product.originalPrice` (sales); `NewsletterService` + `WeeklyAdScheduler`; `NewsletterController` +
  `AccountController`. Frontend: `/sale`, `/about`, guarded `/account`, newsletter signup, sale pricing,
  promo bar, marketing sections, checkout opt-in. See `docs/EMAIL.md`.

Okta (M3), Stripe (M5) and Email (M6) require external accounts/credentials to run; the app still
boots and the catalog/cart/checkout flow works with placeholder config, so they don't block local dev.

## Layout
- `backend/` — Spring Boot (Maven). Package root `com.bob.ecommerceangularapp`.
- `frontend/angular-ecommerce/` — Angular 21 standalone app.

## Commands
- Backend build + tests: `cd backend && ./mvnw clean package` (tests run against in-memory H2 — no Docker needed)
- Backend run (needs Docker for MySQL on :3307): `cd backend && ./mvnw spring-boot:run` (→ http://localhost:8585)
- Frontend build: `cd frontend/angular-ecommerce && npm install && npx ng build`
- Frontend tests: `cd frontend/angular-ecommerce && CI=true npx ng test --watch=false`
- Frontend dev server: `cd frontend/angular-ecommerce && npm start` (→ http://localhost:4250)
- One-shot build + launch + open browser (Git Bash): `./run.sh` — Ctrl+C stops both servers
- Stripe setup (optional, for real card payments): see `docs/STRIPE.md`. Without it, checkout runs in demo mode.

Ports are non-default on purpose: backend **8585**, frontend **4250**, MySQL **3307** (avoids 8080/4200/3306).

## Conventions
- Java 21 (pom pins `<java.version>21</java.version>`). Don't reintroduce the removed
  starters (ldap/saml2/batch/webflux/postgres) — see BUILD_PLAN.md §0.2. The
  `security-oauth2-resource-server` (M3), `stripe-java` (M5) and `spring-boot-starter-mail` (M6)
  starters are intentional.
- API base path is `/api` (Spring Data REST). Frontend reads `environment.apiUrl`.
- MySQL only, course-faithful; Hibernate `ddl-auto=update`; data seeded via `CommandLineRunner`.
- Security/payments degrade gracefully: keep them gated on config (Okta issuer URI,
  Stripe key) so the app builds and runs without those external accounts.
- Verify both builds after changes: `./mvnw -DskipTests clean package` and `npx ng build`.
