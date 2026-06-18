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
- ✅ **Product variants + SKU-level inventory** — `ProductVariant` entity (own SKU, optional color/size,
  price override, per-variant stock, side `product_variant` table — never ALTERs `product`; FK + LAZY
  `@ManyToOne`). `V3__add_product_variants.sql` creates it + adds nullable `variant_sku`/`variant_label`
  to `order_item` (safe ALTER); **validated on real MySQL by the Testcontainers IT** (Flyway V1→V2→V3,
  `ddl-auto=validate` green). Repo is `@RepositoryRestResource(exported=false)` (not an SDR surface).
  `ProductVariantService` owns reads (public `GET /api/catalog/products/{id}/variants` — price/image
  resolved), admin upsert (`GET`/`PUT /api/admin/products/{id}/variants`, replace-by-list) and the
  **checkout stock decrement** (`CheckoutServiceImpl` draws down variant stock per order line; clamped;
  legacy product-level stock stays a display figure). Frontend: variant chips on product-details (price/
  stock/image reflect selection; add-to-cart requires a choice), cart lines keyed by `id+variantSku`
  (`cartItemKey`), `OrderItem` carries the variant, admin product form has a variant editor (FormArray).
  `DataLoader.seedVariants()` seeds mugs/pads (sizes) + luggage (colours); Books stay single-SKU.
  Tests: `ProductVariantServiceTest` (price resolution + decrement clamp). 48 backend + 17 frontend green.
- ✅ **Tax + shipping at checkout** — `TaxRate` (region: country + optional state, %; most-specific match
  wins) + `ShippingMethod` (code/name/baseRate/freeOverThreshold/estimatedDays) entities; `V4` migration
  creates both tables + adds nullable `shipping_amount`/`tax_amount`/`shipping_method` to `orders` (MySQL
  IT-validated). `TaxShippingService.quote()` is the **single source of truth** — it backs both the
  storefront's live totals (`POST /api/checkout/quote`) and the **server-side recompute at order time**
  (`CheckoutServiceImpl` now always recomputes total = subtotal − coupon + shipping + tax when a subtotal
  is sent; legacy/demo callers without a subtotal keep their posted total). Public `GET
  /api/checkout/shipping-methods`; admin CRUD via `AdminTaxShippingController` (`/api/admin/tax-rates`,
  `/api/admin/shipping-methods`). Repos hidden from SDR. Frontend: shipping-method card + live
  subtotal/discount/shipping/tax/total breakdown on checkout (re-quotes on region/method/coupon change),
  `Order`/`Purchase` carry the amounts + method code, new **admin "Tax & Shipping"** page (`/admin/tax-shipping`).
  `DataLoader.seedTaxAndShipping()` seeds 2 methods (Standard free > $50, Express) + 8 US state rates.
  Tests: `TaxShippingServiceTest` (state/country-wide/no-match tax, free-shipping threshold, coupon-before-tax).
- ✅ **Returns / RMA + refunds** — `ReturnRequest` entity (lifecycle REQUESTED→APPROVED/DENIED→REFUNDED);
  `V5` migration creates `return_request` + adds nullable `payment_intent_id` to `orders` (MySQL
  IT-validated). `ReturnService`: customer opens a return (`POST /api/returns`, email must match the
  order — looked up via `OrderRepository.findByOrderTrackingNumber`, `@RestResource(exported=false)`;
  blocks duplicate open returns); admin approves/denies (`AdminReturnController`
  `/api/admin/returns` + `PUT /{id}/decision`). **Approval issues a real Stripe refund** against the
  order's PaymentIntent when Stripe is configured (→ REFUNDED + `stripeRefundId`), else stays APPROVED
  for a manual refund — graceful degradation, non-fatal on Stripe error. Checkout now captures the
  Stripe **PaymentIntent id** onto the order (`Purchase.paymentIntentId` → `Order`). Frontend:
  order-history gains a "Request a return" inline form + status badge (`return.service.ts`); new admin
  **Returns** queue (`/admin/returns`) with approve/deny + refund amount/note. Tests: `ReturnServiceTest`
  (email-match, duplicate guard, approve-without-Stripe, deny). 57 backend + 17 frontend tests green.
- ✅ **Gift cards / store credit** — `GiftCard` entity (unique code, initial + remaining balance,
  recipient, active); `V6` migration creates `gift_card` + adds nullable `gift_card_code`/`gift_card_amount`
  to `orders` (MySQL IT-validated). `GiftCardService`: `check()` previews balance (non-mutating);
  `redeem()` draws down store credit atomically inside the order tx, **clamped to the balance** (never
  negative/over-applied); admin issue (auto-generates `GIFT-XXXX-XXXX` when code blank)/list/deactivate.
  Public `GET /api/checkout/gift-card?code=`; admin `AdminGiftCardController` `/api/admin/gift-cards`.
  Checkout applies a card like a second discount: shows **Gift card** + **Amount due** lines; Stripe is
  charged the `amountDue` and the card step is **skipped when store credit covers the order** (avoids a
  $0 PaymentIntent). `CheckoutServiceImpl` redeems server-side after the quote and records it on the
  order. `DataLoader` seeds GIFT25/GIFT50. Tests: `GiftCardServiceTest` (check active/inactive/empty,
  redeem cap + partial + unknown). New admin **Gift Cards** page (`/admin/gift-cards`).
- ✅ **Loyalty & rewards points** — earn 1 pt/$1, redeem as store credit at 1¢/pt; lifetime points drive
  a Bronze/Silver/Gold tier. `Customer` gains nullable `loyalty_points`/`lifetime_points`; new
  `loyalty_transaction` ledger; `orders` gets nullable `loyalty_points_redeemed`/`loyalty_discount`
  (`V7`, MySQL IT-validated). `LoyaltyService.award()` (floor of order total) + `redeem()` (capped by
  balance **and** order total, logged to ledger) run in the checkout tx after the order saves (so the
  ledger has an order id); `summary()` powers the account view. Public `GET /api/loyalty?email=`.
  `CheckoutServiceImpl` redeems `purchase.pointsToRedeem` then awards. Checkout: "Use my rewards points"
  applies points as store credit (Rewards + Amount due lines, stacks with gift card; card step skipped
  when credit covers the order). Account page gains a **Rewards** card (balance/tier/progress/history).
  Tests: `LoyaltyServiceTest` (earn floor, redeem cap-by-balance + cap-by-total, tier/progress).
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
- ✅ **Real-MySQL integration test + prod config** — `MySqlIntegrationTest` (`@SpringBootTest` +
  Testcontainers `mysql:8.4`, `@ServiceConnection`, `disabledWithoutDocker`) boots the app the way prod
  does (**Flyway enabled + `ddl-auto=validate`**), so it actually exercises the V1/V2 migrations +
  entity↔schema match on real MySQL (the H2 slice tests can't). Production config in
  `application.properties` (graceful shutdown, gzip, HikariCP pool, swallow-size cap) + an
  `application-prod.properties` profile (`SPRING_PROFILES_ACTIVE=prod`: SQL logging off, Swagger off,
  ECS JSON logs, health details hidden). Testcontainers BOM 2.0.5 imported (Boot 4.1's BOM doesn't pin it).
- ✅ **Cloud deploy hardened (GCP Cloud Run)** — made the GCP path genuinely deployable. **CORS is now
  centralized + config-driven**: a single `CorsConfigurationSource` bean in `SecurityConfig` (applied by
  the `.cors()` servlet filter on both chains, so it governs SDR *and* every custom controller) reads
  `app.cors.allowed-origins` (`APP_CORS_ALLOWED_ORIGINS`, default localhost). This **replaced the 12
  hardcoded `@CrossOrigin` annotations + the SDR CORS mapping**, which would have rejected (403) the
  deployed frontend's origin at the MVC layer. Added the **Cloud SQL JDBC Socket Factory**
  (`mysql-socket-factory-connector-j-8`, runtime, inert unless the JDBC URL names `socketFactory=…`) for
  IAM-auth'd/mTLS DB access without a public password port, and `server.forward-headers-strategy=framework`
  (https links behind Cloud Run's TLS termination). `deploy-gcp.yml` is now **single-pass** (deploy backend →
  read live URL → build/deploy frontend against it → auto-update backend `APP_CORS_ALLOWED_ORIGINS`/links —
  no chicken-and-egg) with the `prod` profile, memory/scaling flags, and `--add-cloudsql-instances`.
  `deploy/gcp-setup.sh` is an idempotent one-time provisioner (registry, Cloud SQL, deployer SA + roles,
  runtime SA `cloudsql.client`). 41 backend tests green incl. the real-MySQL IT. See `docs/DEPLOYMENT.md`.
- ✅ **GCP deploy is plug & play (config-file driven)** — all GCP settings live in one committed file,
  `deploy/gcp.env` (`GCP_PROJECT`/region/repo/instance/tier/DB/service-names/sizing), read by **both**
  `gcp-setup.sh` (sources it; inline env vars still override) **and** `deploy-gcp.yml` (a "Load deploy
  config" step greps the `KEY=value` lines into `$GITHUB_ENV`, so the YAML has **no `<-- EDIT`
  placeholders** — every step uses `$GCP_PROJECT` etc.). Set `GCP_PROJECT` once = done. Secrets shrank
  to **two** (`GCP_SA_KEY`, `DB_PASS`; `DB_USER` moved into `gcp.env` as non-secret), and the setup
  script **auto-pushes them via the `gh` CLI** (`gh secret set`, then deletes `key.json`) when `gh` is
  authenticated, else prints them to paste (honors `GH_REPO=owner/repo`). `gcp.env` is committed by
  design (project id isn't secret); gitignore it + use repo Variables if you'd rather not. AWS/Azure
  still use their workflow `env:` block. Verified: both config-load paths parse `gcp.env` correctly and
  inline overrides win.
- ✅ **AWS + Azure brought to the same standard** — `deploy-aws.yml` (App Runner + RDS + ECR) and
  `deploy-azure.yml` (Container Apps + Azure DB for MySQL + ACR) are now also **single-pass** (deploy
  backend → read live URL → build/deploy frontend → re-point backend `APP_CORS_ALLOWED_ORIGINS`/links),
  with `prod` profile + plain JDBC URLs (no socket factory needed off-GCP) + idempotent
  `deploy/aws-setup.sh` / `deploy/azure-setup.sh`. **No backend changes** — the centralized CORS +
  forwarded-headers work cloud-agnostically. Caveats: AWS uses a hyphen-free RDS DB name
  (`fullstackecommerce` — RDS rejects hyphens in the initial DB), polls App Runner status (no `wait`
  verb), and its setup opens RDS 3306 (VPC connector recommended for prod); Azure uses `sslMode=REQUIRED`.
  These cloud workflows/scripts are **not runtime-verified** (need the respective cloud accounts).
- ✅ **AWS + Azure are plug & play too (config-file driven)** — same pattern as GCP: all settings live
  in one committed file (`deploy/aws.env` / `deploy/azure.env`), read by **both** the setup script
  (sources it) **and** the workflow (a "Load deploy config" step greps `KEY=value` → `$GITHUB_ENV`), so
  **no `<-- EDIT` `env:` blocks** remain in any workflow. Setup scripts **auto-push the secrets via the
  `gh` CLI** (fallback: print; honors `GH_REPO=owner/repo`); `DB_USER` moved into the config as
  non-secret. **AWS is now repo-variable-free**: the workflow discovers the **account id** (`aws sts
  get-caller-identity`) and **RDS endpoint** (`describe-db-instances` on `DB_INSTANCE`) at runtime, so
  `AWS_ACCOUNT_ID` + the `RDS_ENDPOINT` var are gone (secrets: just the 2 access keys + `DB_PASS`).
  Azure: set `ACR_NAME` + `MYSQL_SERVER` (globally-unique) in `azure.env` — the old `$RANDOM` name
  generation is gone, and both the setup script and the workflow fast-fail on the unedited
  `your-unique-*` placeholders. Still **not runtime-verified** (need the cloud accounts). All three
  config files are committed by design (no secrets in them); gitignore + repo Variables if preferred.
- ✅ **Security hardening pass + sessions/tokens model** — the app is **stateless** server-side:
  `SessionCreationPolicy.STATELESS` on both chains (no `HttpSession`/`JSESSIONID`/app cookie); the
  "session" is the Okta token set in the browser (okta-auth-js, Authorization Code + PKCE). Secured
  chain now also gates `/api/account/**` + `POST /api/newsletter/send-now` and scopes actuator
  (health public, metrics/info/prometheus authenticated); a startup `ApplicationRunner` logs a loud
  warning if the `prod` profile runs without an Okta issuer. The **SPA is hardened at the nginx edge**:
  CSP (own bundles + Stripe + Google Fonts; `connect-src` for the API/Okta/Stripe), HSTS, X-Frame-Options,
  nosniff, Referrer/Permissions-Policy. Build disables Angular `inlineCritical` (its inline `onload`
  handler would be CSP-blocked, unstyling the page). `docs/SECURITY.md` gained a "Sessions, tokens &
  cookies" section (incl. the BFF upgrade path + why CSRF is N/A). **Runtime-verified** via
  `docker compose up --build`: all security headers present, **0 CSP violations**, 12 products render,
  Flyway migrates + seeds the empty DB, prod warning fires. 45 backend tests + 17 frontend tests green.

Okta (M3), Stripe (M5) and Email (M6) require external accounts/credentials to run; the app still
boots and the catalog/cart/checkout flow works with placeholder config, so they don't block local dev.

## Layout
- `backend/` — Spring Boot (Maven). Package root `com.bob.ecommerceangularapp`.
- `frontend/angular-ecommerce/` — Angular 21 standalone app.

## Commands
- Backend build + tests: `cd backend && ./mvnw clean package` (unit/slice tests run on in-memory H2 — no Docker needed; the **Testcontainers MySQL integration test** runs when Docker is available and auto-skips otherwise)
- Backend run (needs Docker for MySQL on :3307): `cd backend && ./mvnw spring-boot:run` (→ http://localhost:8585)
- Frontend build: `cd frontend/angular-ecommerce && npm install && npx ng build`
- Frontend tests: `cd frontend/angular-ecommerce && CI=true npx ng test --watch=false`
- Frontend E2E (Playwright, hermetic — stubs the API, starts `ng serve` itself): `cd frontend/angular-ecommerce && npx playwright install chromium` (one-time) then `npm run e2e`
- Frontend dev server: `cd frontend/angular-ecommerce && npm start` (→ http://localhost:4250)
- One-shot build + launch + open browser (Git Bash): `./run.sh` — Ctrl+C stops both servers
- Full-stack local deploy (all 3 tiers in containers, prod-shaped — mirrors cloud): `docker compose up --build` (repo-root `compose.yaml`) → http://localhost:4250. See `docs/DEPLOYMENT.md`.
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
