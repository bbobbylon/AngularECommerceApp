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
- ✅ **Production-readiness pass** — `GlobalExceptionHandler` (`@RestControllerAdvice`) + Bean Validation
  on public DTOs; info/legal pages (`/faq`, `/contact`, `/shipping-returns`, `/privacy`, `/terms`) +
  footer links; account "Security & sign-in" card. MFA/OTP/passkeys are delegated to Okta (see
  `docs/SECURITY.md`), and ops/cost/upgrade guidance lives in `docs/MAINTENANCE.md`. `DataLoader` now
  self-heals existing DBs (newsletter + sale-price backfills) and never lets an auxiliary step crash
  the catalog.
- ✅ **Admin panel (back-office)** — `/api/admin/**` (gated like `/api/orders`): `AdminController` (stats,
  categories, reviews moderation), `AdminProductController` (product CRUD — custom because SDR writes are
  disabled), `AdminOrderController` (list + status), `AdminCouponController` (coupon CRUD). `AdminService`
  + `PageResponse`/`AdminStats`/`AdminOrderView`/`AdminProductRequest` DTOs. Frontend `/admin` area
  (guarded, full-width — customer sidebar hidden): dashboard, products, orders, reviews, coupons.
  `authInterceptor` also tokens `/api/admin`.
- ✅ **Feature set (reviews, coupons, faceted search, wishlist/tracking)**:
  - **Reviews & ratings** — `Review` entity + `ReviewService`/`ReviewController`; denormalized
    `Product.averageRating`/`reviewCount` (nullable). Stars on cards/details (`StarRating`), reviews list
    + write form, admin moderation. Seeded on ~half the catalog.
  - **Coupons** — `Coupon` entity + `CouponService`/`CouponController` (validate) + admin CRUD; checkout
    promo field; `CheckoutServiceImpl` re-validates server-side and records `Order.couponCode`/`discountAmount`.
    Seeded: WELCOME10, SAVE5, SUMMER20.
  - **Faceted search** — `ProductRepository` is a `JpaSpecificationExecutor`; `ProductQueryService` +
    `/api/catalog/search` (category/keyword/price/in-stock/on-sale/rating/sort). Product list unified to
    this endpoint with a filter panel.
  - **Wishlist + tracking** — `WishlistItem` (email-keyed) + `/api/wishlist` (sync/get/remove); favorites
    page "sync across devices". `OrderTimeline` component on order-confirmation + order-history.
- ✅ **Storefront UX depth (galleries, recently viewed, stock urgency)**:
  - **Multi-image galleries** — `Product.additionalImages` (`@ElementCollection` → side table `product_image`,
    LAZY + `@BatchSize`, never ALTERs the populated `product` table). Serialized via open-in-view. Thumbnail
    picker on product-details; admin product form has a "Gallery images" textarea (one URL/line).
    `DataLoader.galleryFor()` seeds variants + a transactional `backfillGalleryImages()` populates existing DBs.
  - **Recently viewed** — `RecentlyViewedService` (localStorage, signal) + reusable `RecentlyViewed` strip on
    product-details (excludes current) and the home page.
  - **Low-stock urgency** — `isLowStock()`/`LOW_STOCK_THRESHOLD` in `common/product.ts`; "Only N left" / "Out
    of stock" badges on cards + details. `DataLoader.stockFor()` + `backfillStockVariety()` seed a realistic
    spread (most healthy, some 1–4, the odd 0).
  - "You might also like" related products already existed (`ProductService.getRelatedProducts`).
- ✅ **Observability & ops** — `spring-boot-starter-actuator` + `micrometer-registry-prometheus`:
  health (+ liveness/readiness **probes**), `/actuator/info` (build version/time via the `build-info`
  goal), metrics, `/actuator/prometheus`. `RequestIdFilter` adds an `X-Request-Id` correlation id
  (MDC → logs via `logging.pattern.level`, echoed on responses). **`management.health.mail.enabled=false`**
  — the auto-configured mail indicator otherwise forces health DOWN when SMTP is unconfigured (would
  evict healthy pods). Admin-facing `GET /api/admin/system` (`SystemHealthService`/`AdminSystemController`
  + `SystemHealth` DTO) powers the **Admin → Dashboard** "System health" card. Structured JSON logging is
  opt-in (`logging.structured.format.console=ecs`). See `docs/OBSERVABILITY.md`.
- ✅ **Data & reliability** — **Flyway** migrations own the schema (`V1__baseline.sql` generated from
  the entities + `V2__add_search_indexes.sql`), `ddl-auto=validate` (gotcha retired); secondary
  **indexes** on hot columns (`Review.product_id`, `Customer.email`, `Product(active,category_id)`,
  `Order.date_created`, via `@Index` + V2); **Caffeine caching** of the catalog search (cache-safe
  `ProductCardView` projection, evicted on admin product writes). Verified live on both a fresh DB
  (Flyway runs V1+V2) and the existing DB (baselined at V1, V2 applied). See `docs/MAINTENANCE.md`.
- ✅ **Security hardening** — `SecurityConfig` now **role-gates** `/api/admin/**` (`hasAuthority` on a
  configurable JWT groups claim — `app.security.admin-claim`/`admin-role`, defaulting to a `groups`
  claim containing `Admin`) instead of bare `authenticated()`, and applies **response-header hardening**
  (CSP, HSTS, `X-Frame-Options: DENY`, Referrer-Policy, Permissions-Policy) on **both** chains.
  `RateLimitFilter` adds per-IP rate limiting (30/min via Caffeine) + a 64 KB body cap on the public
  write endpoints (`/api/reviews|coupons|newsletter`), returning 429/413. All still gated on config so
  the app runs fully open without Okta (graceful degradation preserved). See `docs/SECURITY.md`.
- ✅ **API docs (OpenAPI/Swagger)** — `springdoc-openapi-starter-webmvc-ui` **3.0.3** (the v3 line targets
  Spring Boot 4 / Spring Framework 7; 2.x is Boot 3 only). Swagger UI at `/swagger-ui.html`, spec at
  `/v3/api-docs`. `OpenApiConfig` supplies the title/description + a Bearer-JWT scheme (Authorize button).
  Because the strict CSP sets `script-src 'none'`, `SecurityConfig` applies a **path-scoped CSP** — the
  relaxed `script-src 'self' 'unsafe-inline'` only on `/swagger-ui`+`/v3/api-docs` (via
  `DelegatingRequestMatcherHeaderWriter`), strict everywhere else. Toggle off in prod with
  `springdoc.api-docs.enabled=false`/`springdoc.swagger-ui.enabled=false`. See `docs/API.md`.
- ✅ **Frontend E2E (Playwright)** — `frontend/angular-ecommerce/e2e/` smoke suite: core storefront flow
  (browse → add to cart → checkout in demo mode → order confirmation) + an app-shell/static-page check.
  **Hermetic** — `e2e/support/mock-backend.ts` stubs the API at the network layer (matching the real
  `/catalog/search`, HAL `_embedded`, and checkout contracts), so no backend/MySQL is needed; the
  Playwright `webServer` starts `ng serve` itself. `npm run e2e` locally; runs in CI as the
  **Frontend (Playwright E2E smoke)** job (`ci.yml`).
- ✅ **Dependency / CVE scanning** — `.github/dependabot.yml` (Maven + npm + actions, weekly + security
  PRs); CI **security** job: `actions/dependency-review-action` (PRs, fail-on high+, both ecosystems) +
  `npm audit --omit=dev --audit-level=high` (shipped deps; green today). Closes the last `SECURITY.md`
  TODO. See `docs/SECURITY.md` → Dependency & CVE scanning.

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
- Frontend E2E (Playwright, hermetic — stubs the API, starts `ng serve` itself): `cd frontend/angular-ecommerce && npx playwright install chromium` (one-time) then `npm run e2e`
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
- MySQL only, course-faithful; data seeded via `CommandLineRunner` (`DataLoader`).
- **Schema is owned by Flyway** (`src/main/resources/db/migration/V{n}__*.sql`); Hibernate runs
  `ddl-auto=validate` — it verifies the schema matches the entities on boot and **fails fast** on a
  mismatch (this retired the old `ddl-auto=update` "silently skips a NOT NULL column on a populated
  table" gotcha). **Any entity change now needs a new `V{n}` migration** — never edit an applied one;
  never reintroduce `ddl-auto=update`. Existing pre-Flyway DBs are baselined at V1; fresh DBs build
  from the migrations. Tests run on H2 with Flyway disabled (`spring.flyway.enabled=false`) +
  `ddl-auto=create-drop`. See `docs/MAINTENANCE.md`.
- Hot catalog reads are cached (Caffeine, `CacheConfig`): `/api/catalog/search` returns the
  `ProductCardView` projection (no lazy gallery → cache-safe, no N+1); admin product writes evict it.
- Security/payments degrade gracefully: keep them gated on config (Okta issuer URI,
  Stripe key) so the app builds and runs without those external accounts.
- Security/payments degrade gracefully: keep them gated on config (Okta issuer URI,
  Stripe key) so the app builds and runs without those external accounts.
- Verify both builds after changes: `./mvnw -DskipTests clean package` and `npx ng build`.
