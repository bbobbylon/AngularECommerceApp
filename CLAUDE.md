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
- ✅ **Referral program** — each `Customer` gets a unique `referral_code` (lazily assigned); a
  `referral` ledger tracks who referred whom (`V8`: nullable+unique code on customer — MySQL allows
  multiple NULLs under a unique key — + new table; IT-validated). `ReferralService` reuses
  `LoyaltyService.grantPoints()` to reward **both** parties (referrer 500 / referee 200 pts) when a
  **new** customer places their **first** order carrying a code; guards self-referral + one-per-referee.
  Public `GET /api/referrals?email=` (assigns the code on first view). `CheckoutServiceImpl` calls
  `recordReferral` only when `saved.getOrders().size() == 1`. Frontend: a root-instantiated
  `ReferralService` captures `?ref=CODE` into localStorage on landing and attaches it to the first
  checkout (cleared after); account page gains a **Refer a friend** card (shareable link + copy +
  stats). Tests: `ReferralServiceTest` (reward both, self-referral guard, already-referred guard, code
  assignment).
- ✅ **Back-in-stock notifications** — `StockNotification` entity (email + product, optional variant SKU,
  `notified` flag); `V9` creates the table (IT-validated). `StockNotificationService.subscribe()`
  (deduped while waiting) + `notifyProductRestocked`/`notifyVariantRestocked` email every waiter once
  via the **gated** `EmailService.sendBackInStock` (new `EmailTemplates.backInStock`) and mark them
  notified. Triggers: `AdminService.updateProduct` (when stock > 0) and `ProductVariantService.replaceVariants`
  (per restocked variant). Public `POST /api/stock-notifications`. Frontend: product-details shows an
  "Email me when it's back" form whenever the selected product/variant is out of stock
  (`ProductService.notifyWhenInStock`). Tests: `StockNotificationServiceTest` (subscribe dedup, notify +
  mark, OOS no-op).
- ✅ **Abandoned-cart recovery** — `AbandonedCart` entity (email + item count/total/summary,
  `recovered`/`reminded` flags); `V10` creates the table (IT-validated). Checkout POSTs a cart snapshot
  on email blur (`POST /api/abandoned-cart`, upsert-by-email, resets the reminder on each change);
  `AbandonedCartScheduler` (`@Scheduled`, cron `app.abandoned-cart.cron`, default every 15 min) calls
  `AbandonedCartService.remindStale()` → emails carts idle past `app.abandoned-cart.after-minutes`
  (default 60) via the **gated** `EmailService.sendAbandonedCart` (new `EmailTemplates.abandonedCart`),
  marking them reminded. Placing an order calls `markRecovered(email)` so no reminder fires. Tests:
  `AbandonedCartServiceTest` (capture upsert, skip empty, mark recovered, remind + flag). E2E mock stubs
  the new endpoint.
- ✅ **Address book + saved payment methods** — `SavedAddress` + `SavedPaymentMethod` entities (email-keyed;
  cards store **only** Stripe references + brand/last4/expiry, never raw card data — PCI); `V11` creates
  both tables (IT-validated). `AddressBookService` (CRUD, exactly-one-default, ownership-checked deletes)
  and `PaymentMethodService` (Stripe **SetupIntent** to add a card + record-from-PaymentMethod + list +
  remove-with-detach) — both **gated/graceful** without Stripe. Endpoints under the (Okta-)gated
  `/api/account/addresses` + `/api/account/payment-methods`. Account page gains **Address book** (full
  CRUD) + **Saved cards** (list/remove + Stripe-Elements "Add a card", disabled w/o Stripe) cards;
  checkout shows a **"Use a saved address"** picker (autofills shipping, matching country/state objects)
  loaded on email blur. Tests: `AddressBookServiceTest` (one-default, owner-only delete),
  `PaymentMethodServiceTest` (graceful no-Stripe). E2E mock stubs `/account/addresses`.
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
- ✅ **Runtime i18n + display currency (frontend-only)** — `I18nService` (en/es/fr key→string dicts,
  active language is a persisted signal; `t()` falls back en → key) + impure `t` pipe; `CurrencyService`
  (USD/EUR/GBP/CAD/AUD/JPY, static demo rates, persisted signal) + impure `money` pipe converts
  stored-USD prices for **display only** on browse/cart surfaces (product list/details, cart,
  favorites, recently-viewed). **Checkout/admin stay in the built-in USD pipe** — settlement is USD;
  the order summary shows a note when a non-USD display currency is active. Header gains
  currency + language selectors; nav/footer strings are translated. Extend by adding dict keys +
  `| t` usages (full `@angular/localize` extraction is the ship-every-string path); swap static
  rates for a live FX feed for prod.
- ✅ **SEO (roadmap #11)** — `SeoService` (Angular `Title`/`Meta` + a keyed JSON-LD `<script>`
  injector) sets per-route title/description/canonical/OG/Twitter tags: product-details (Product
  JSON-LD with `offers`/`aggregateRating`), product-list (home/category/search/sale scopes), and
  the static info pages. `App` root injects a one-time Organization/WebSite JSON-LD block. Since
  this is a plain client-side SPA (no SSR/prerendering), tags land after JS runs — fine for
  JS-executing crawlers, not a substitute for prerendering if that's ever needed. `sitemap.xml` is
  generated **by the backend** (`SitemapController`/`SitemapService`, public, outside `/api`) from
  the live catalog (`ProductRepository.findByActiveTrue()` + all categories) — its `<loc>` entries
  point at the frontend (`app.frontend-url`) even though the backend serves it, since the frontend
  has no server to generate it from. `frontend/public/robots.txt` points crawlers at it; the
  Dockerfile bakes in the right backend origin (from the `API_URL` build arg) alongside the
  existing `environment.ts` substitution. See `docs/DEPLOYMENT.md`.
- ✅ **PWA — installable + offline app shell (roadmap #12)** — `@angular/service-worker` registered
  via `provideServiceWorker('ngsw-worker.js', { enabled: !isDevMode(), registrationStrategy:
  'registerWhenStable:30000' })` in `app.config.ts`; gated on `isDevMode()` rather than
  `environment.production` because this project's `environment.ts` has no `fileReplacements` wired
  in `angular.json` (that flag is always `false`) — `isDevMode()` correctly reflects the real build
  mode instead. `ngsw-config.json` prefetches the app shell, lazily caches images/fonts/icons, and
  applies a `freshness` strategy (5s timeout, 1h max age) to `/api/catalog|products|product-category`
  so the storefront still renders from cache on a flaky connection. `public/manifest.webmanifest` +
  `public/icons/*` (brand navy `#1b2133` bag-and-heart glyph, generated once via a throwaway
  puppeteer-core + local Chrome script, sizes 72–512 + a 180 apple-touch-icon) make the app
  installable; `index.html` links the manifest + apple meta tags and its `theme-color` now matches
  the manifest/nav color (was a stray `#241E1A` that matched nothing else in the codebase). New
  `InstallPrompt` component (`components/install-prompt/`) surfaces the native
  `beforeinstallprompt` banner bottom-right, dismissible with a 14-day localStorage cooldown, and
  is **also** gated on `isDevMode()` — Chrome can fire `beforeinstallprompt` from just the
  manifest/icons even with no active service worker, so without this gate the install nag would
  show during plain `ng serve` too. **`serviceWorker: "ngsw-config.json"` lives only on the
  `production` build configuration** in `angular.json`, not the top-level build options — putting it
  there would silently enable the SW during `ng serve` dev sessions. Fixed a real caching bug this
  surfaced: `nginx.conf`'s existing `location ~* \.(?:js|css|...)$` 1-year-immutable rule would have
  also matched `ngsw-worker.js`/`ngsw.json`, permanently freezing the installed app at whatever
  version the browser first cached (Angular's SW update check can never see a new deploy if the
  browser never re-fetches those two files) — added exact-match `location = /ngsw.json` /
  `location = /ngsw-worker.js` blocks (`Cache-Control: no-cache`) ahead of the regex rule (nginx `=`
  locations always win over regex ones). Verified: production build emits
  `ngsw-worker.js`/`ngsw.json`/`manifest.webmanifest`/`icons/`; served statically, the SW registers
  and activates and the install banner renders with real content; `ng serve` shows neither; `nginx -t`
  validates the new config; 17 frontend tests still green.
- ✅ **Accessibility / WCAG 2.1 AA (roadmap #13)** — `e2e/a11y.spec.ts` runs axe-core
  (`@axe-core/playwright`) against all 12 reachable storefront pages **in both the light and dark
  theme** (24 checks), reusing the hermetic `mock-backend.ts`. Went from 24 color-contrast + 2 label
  + 2 select-name violations down to zero by fixing the underlying causes rather than the individual
  flagged nodes: design-system colors (`--teal`, `--accent-600`) that were tuned for icons/buttons
  but reused as text got a theme-aware text-safe variant (`--teal-text`, `--accent-text`, plus a
  `--code-color` for Bootstrap's default code pink) instead of being darkened in place (which would
  have broken their non-text uses); Bootstrap defaults never bridged to the app's own `[data-theme]`
  attribute (`.form-text` — ~1.06:1, functionally invisible checkout helper text —
  `.breadcrumb-item.active`) got explicit themed overrides; `product-category-menu`'s header (locked
  to `bg-white` on purpose, like the footer is locked dark) got a scoped `--muted` override so its
  text stays readable regardless of site theme. Also fixed real bugs: checkout's shipping/billing
  fields had no `for`/`id` association and identical label text in both sections (screen readers
  couldn't tell them apart) — added `customer-*`/`shipping-*`/`billing-*` prefixed ids; the Search
  button was `btn-outline-primary` (transparent) sitting on the dark navbar at ~2.56:1 — switched to
  solid `btn-primary`. See `docs/ACCESSIBILITY.md` for the full writeup + manual (non-automatable)
  checklist. 17 frontend tests + full `npx ng build --configuration production` still green.
- ✅ **Header search typeahead (roadmap #14)** — `components/search/` gives the header search box
  as-you-type suggestions: debounced (250ms) + `distinctUntilChanged` + `switchMap`-cancelled calls
  against the existing `/api/catalog/search?keyword=&size=6` endpoint (no new backend endpoint —
  it's already Caffeine-cached). Built with `toObservable`/`toSignal` interop, no manual
  subscriptions. Implements the ARIA 1.2 combobox pattern: real focus stays on the `<input
  role="searchbox">`, arrow keys move a virtual selection communicated via `aria-activedescendant`
  over `role="option"` rows in a `role="listbox"`; `aria-controls`/`aria-owns` are bound
  conditionally (`null` when the listbox isn't rendered) — a static reference to a
  conditionally-rendered id is an `aria-valid-attr-value` violation on every page, not just when
  the dropdown is open. Enter with a suggestion highlighted navigates straight to that product;
  Enter with none highlighted (or clicking "See all results…") goes to `/search/:keyword`; Escape
  and click-outside close it. Fixed a pre-existing latent dark-mode bug in the same file: the
  search icon had hardcoded `bg-white`/`text-muted` classes that would have failed the 3:1
  non-text-contrast threshold once `--muted` was tuned for the a11y pass (#13) — removed, now
  inherits the already-themed `.input-group-text` color. New `e2e/search-typeahead.spec.ts` (6
  tests: suggestions + click-to-navigate, keyboard nav, Enter-with-no-selection, empty state,
  Escape, click-outside); `mock-backend.ts`'s `/catalog/search` stub is now keyword/size-aware
  instead of always returning both fixtures unfiltered. 32 E2E tests + 17 frontend unit tests +
  production build all green.
- ✅ **Inventory management + CSV (roadmap #15)** — a merged SKU-level stock view across `Product`
  (products with no variants — the product's own SKU is authoritative) and `ProductVariant`
  (products sold per-variant — each variant SKU is authoritative; the base product's row is skipped
  once it has variants, matching the existing checkout-decrement convention). `InventoryAdjustment`
  (`V12` migration, MySQL-IT-validated) logs every stock change — sku, product name snapshot,
  previous/new quantity, delta, source (`MANUAL`/`CSV_IMPORT`), optional note, timestamp — whether
  it came from the admin's inline edit or a bulk CSV import. `InventoryService` owns the merged
  read (`GET /api/admin/inventory`), the audit log (`GET /api/admin/inventory/adjustments`, paged),
  a single edit (`PUT /api/admin/inventory/{sku}` — restocking from 0 re-triggers the existing
  back-in-stock notifications), and CSV export/import (`GET .../export`, `POST .../import` as
  multipart). The CSV parser is hand-rolled (no new dependency) — minimal RFC-4180-ish quoting, a
  header row naming `sku`/`quantity`/optional `note` columns (order-independent), and per-row
  errors (unknown SKU, unparsable quantity) that don't abort the rest of the batch. New admin
  **Inventory** page (`/admin/inventory`): inline stock edits with a save-per-row, a search/filter
  box, CSV export/import buttons with an import-result summary, and a "Recent adjustments" table.
  Browser-verified end to end: inline edit → audit log entry appears; CSV import → partial
  success + per-row error shown; CSV export → `200` on `/api/admin/inventory/export`. 7 new backend
  unit tests (`InventoryServiceTest`) + 32 E2E + 17 frontend unit tests + full `./mvnw clean
  package` (incl. the real-MySQL IT) + production build all green.
- ✅ **Promotions engine (roadmap #16)** — automatic, no-code discounts, distinct from `Coupon` (which
  requires the customer to enter a code). `Promotion` entity (`V13` migration, MySQL-IT-validated):
  name/description, percentOff or amountOff (percent wins if both), optional minSpend, active flag,
  and an optional `startsAt`/`endsAt` date window (either side null = unbounded — this is the piece
  coupons don't have, letting a sale be scheduled in advance, e.g. "Black Friday Nov 24–27").
  `PromotionService.findBest(subtotal)` filters to active + in-window + min-spend-eligible promotions
  and picks whichever is worth the most — consulted by `TaxShippingService.quote()` on *every*
  checkout (no code needed) and **stacked additively on top of any manually-entered coupon**, before
  shipping/tax (mirrors how gift card + rewards later stack on top of the discounted total, just at
  an earlier stage in the pipeline: subtotal→coupon→promotion→shipping→tax→gift card→rewards→amount
  due). Design note: the checkout quote pipeline (`QuoteRequest`/`QuoteResponse`) only ever carried
  an aggregate `subtotal` — no per-line-item data — so this was scoped to **order-level** promotions
  only; product/category-scoped automatic promotions would need to thread cart line items through
  that whole pipeline, a materially bigger change deferred unless a future feature needs it. `Order`
  gains nullable `promotionName`/`promotionDiscount` (set by `CheckoutServiceImpl` alongside the
  existing coupon fields) so an applied promotion is recorded on the order the same way a coupon is.
  Admin CRUD mirrors `AdminCouponController`/coupons page exactly (`/api/admin/promotions`, new
  **Promotions** admin page at `/admin/promotions`); checkout's order summary shows the auto-applied
  promotion as an informational line (no apply/remove UI — it just applies itself). Browser-verified
  end to end: created a 5%-off, always-on promotion in admin → added an item to cart → checkout
  showed "Test Auto Promo: −$X.XX" with no code entered and the total reflected it → deleted the
  test promotion via the API afterward. 8 new backend unit tests (`PromotionServiceTest`) + a new
  `TaxShippingServiceTest` case (coupon + promotion stacking) + 32 E2E + 17 frontend unit tests +
  full `./mvnw clean package` (incl. the real-MySQL IT, which validated `V13`) + production build
  all green.
- ✅ **Simple CMS (roadmap #17)** — deliberately narrow, not a generic page-builder: a single
  site-wide announcement banner (replacing the hardcoded `promo-bar` block in `app.html`) plus an
  editable FAQ list (replacing the hardcoded array in `faq.ts`). `SiteBanner` is a **singleton-row
  entity** — no natural unique key, `ContentService.currentBanner()` finds it via
  `findAll().stream().findFirst()` rather than hardcoding an id, enforced by application logic, not
  a DB constraint. `FaqEntry` (question/answer/sortOrder/active) supports the usual admin
  upsert-by-optional-id pattern. `V14` migration creates both tables (MySQL-IT-validated).
  `ContentService` (backend) exposes public reads (`GET /api/content/banner` — 204 when none is
  configured or it's inactive; `GET /api/content/faq` — active entries ordered by sortOrder) plus
  admin CRUD (`AdminContentController`, `/api/admin/content/banner` GET/PUT,
  `/api/admin/content/faq` GET/POST/DELETE). `DataLoader.seedContent()` seeds the **exact** prior
  hardcoded banner text and all 6 prior hardcoded FAQ entries, so a fresh DB renders identically to
  before this feature — only now an admin owns the content going forward, not a code deploy.
  Frontend `content.service.ts` (public reads, `catchError`-to-`null`/`[]` graceful degradation,
  mirroring `recomputeQuote()`'s transient-failure pattern) backs `app.ts`'s dynamic banner (root
  component fetches once on load) and `faq.ts`'s dynamic FAQ list. New admin **Content** page
  (`/admin/content`, mirrors `admin-tax-shipping`'s two-cards-per-page layout): a banner settings
  form and an edit-in-place FAQ list. Fixed a real bug found during browser verification: the FAQ
  "add entry" form's default `sortOrder` was computed from the signal's value at field-initializer
  time (before `ngOnInit`'s `load()` populated it), so the first entry added on a fresh page load
  collided with an existing entry's sort order — fixed by syncing the default inside the load
  callback instead of a one-shot computed field initializer. Also hardened `search-typeahead.spec.ts`
  and `a11y.spec.ts` with `page.waitForLoadState('networkidle')` after navigation: the new
  CMS-driven banner fetches *after* first paint (unlike the old static markup) and its arrival
  shifts the sticky header down, which was intermittently racing fixed-coordinate clicks/scans in
  those specs. Browser-verified end to end: edited the banner message via `/admin/content` → storefront
  showed the new text on reload; added a test FAQ entry → appeared in the admin list and on `/faq` →
  deleted via the admin API; restored the original banner text via the admin API afterward. 12 new
  backend unit tests (`ContentServiceTest`) + 32 E2E (stable across repeated runs) + 17 frontend
  unit tests + full `./mvnw clean package` (incl. the real-MySQL IT, which validated `V14`) +
  production build all green.
- ✅ **Analytics dashboard (roadmap #18)** — the first feature since #11 with **zero schema change**:
  `AnalyticsService` is a pure read-side aggregation over existing `Order`/`OrderItem`/`Product` data
  (no new entity, no `V{n}` migration). Aggregation is plain Java streams/grouping, not a JPQL
  projection — deliberately matches the codebase's established style (`PromotionService`/
  `TaxShippingService`/`AdminService` all keep aggregation logic in Java, not the DB); confirmed via
  grep there was no existing JPQL-projection precedent to follow instead. `revenueOverTime(days)`
  zero-fills every day in the window (not just days with orders, so the chart doesn't lie about gaps);
  `topProducts(days, limit)` sums units/revenue per product from `OrderItem`s and sorts descending
  (falls back to "Unknown product" if the product was since deleted); `orderStatusBreakdown()` groups
  by status, bucketing null/blank as `"UNKNOWN"`; `summary()` gives avg order value (all-time) plus
  this-month vs. last-month revenue and month-over-month growth — `growthPercent` is a nullable
  `Double`, deliberately `null` (not `0.0`) when last month had no revenue to compare against, so the
  UI can show "no data to compare" instead of a misleading fake percentage. `AdminAnalyticsController`
  (`/api/admin/analytics/{summary,revenue,top-products,order-status}`) needs no new security wiring —
  `/api/admin/**` is already globally role-gated. Frontend `AdminAnalytics` (`/admin/analytics`, new
  "Analytics" nav link right under Dashboard) shows 4 KPI cards, a hand-rolled CSS/flexbox bar chart
  for the revenue trend, and two horizontal-bar lists (top products, order status) reusing the
  existing `.rating-bar` class (originally built for review-star distributions) with a new
  `.bar-fill-accent` modifier — confirmed via `grep -i chart package.json` that no charting dependency
  already existed, so no new one was added (mirrors the hand-rolled-CSV-parser precedent from #15).
  Browser-verified end-to-end with real data: placed a demo-mode order through the storefront, then
  confirmed the dashboard's avg order value, this-month revenue (with "no data last month" since the
  dev DB was otherwise empty), top-products ranking (sorted by revenue), and status breakdown
  (`UNKNOWN` × 1 — correct, since a freshly-placed order has no status until an admin sets one) all
  matched. 8 new backend unit tests (`AnalyticsServiceTest`) + 32 E2E (stable across 2 full runs) + 17
  frontend unit tests + full `./mvnw clean package` + production build all green.
- ✅ **RBAC + global audit log (roadmap #19)** — three back-office roles (`Admin` full access,
  `OrderManager` read-everything + order status/return decisions, `Viewer` read-only) enforced purely
  via **ordered `authorizeHttpRequests` request-matchers** in `SecurityConfig` — deliberately *not*
  `@EnableMethodSecurity`/`@PreAuthorize`, since a grep confirmed this codebase had never used method
  security anywhere; specific matchers (`PUT /api/admin/orders/**`, `PUT /api/admin/returns/**`) are
  inserted ahead of the existing `/api/admin/**` catch-all (Spring matches top-down, already this
  file's convention). No `JwtAuthenticationConverter` change needed — it already passes through
  arbitrary JWT groups-claim values as authorities verbatim, so the new role names just need to appear
  in Okta's groups claim. New `AuditLogEntry`/`audit_log_entry` (`V15`) is a **global, cross-cutting**
  ledger — one row per admin mutation across the *entire* back-office — deliberately kept separate
  from the domain-specific `InventoryAdjustment` ledger (#15, stock-history only); an inventory
  adjustment now writes to both. `AuditLogService.record(...)` is called from **every** admin mutation
  controller (Products+variants, Orders, Returns, Coupons, Promotions, Gift Cards, Tax Rates, Shipping
  Methods, Content/Banner/FAQ, Categories, Reviews, Inventory adjust+CSV import) — not a representative
  sample. Actor resolution uses a plain `Authentication authentication` **method parameter** (Spring
  MVC auto-resolves it, no `@AuthenticationPrincipal` needed) so the same controller code works
  whether the secured JWT chain or the open/no-Okta chain is active; `AuditLogService.resolveActor()`
  returns the principal name when authenticated, else `"anonymous"`. `GET /api/admin/me` reports the
  caller's known roles, defaulting to `["Admin"]` when none are present (i.e. the app is running fully
  open without Okta) — consistent with the app's established graceful-degradation pattern. Frontend
  adds an admin **Audit Log** page (`/admin/audit-log`, paginated + entity-type filter) and a "Signed
  in as: {roles}" badge in the admin sidebar — deliberately just a courtesy label, not a client-side
  permission gate; none of the ~10 existing admin components were changed to hide buttons per-role,
  since the backend's request-matchers are the actual enforcement boundary. Browser-verified live: set
  a real order's status to Processing through the Orders page, then confirmed the resulting
  `ORDER_STATUS_UPDATE` entry (actor `anonymous`, matching the no-Okta dev chain) rendered on the audit
  log page, and that the entity-type filter correctly showed/hid it. 25 new/extended backend tests
  (`AuditLogServiceTest` + `SecurityFilterChainIntegrationTest` RBAC cases) + full `./mvnw clean
  package` (134 tests, incl. the real-MySQL IT validating `V15`) + 32 E2E (stable across 2 full runs) +
  17 frontend unit tests + production build all green.
- ✅ **Fulfillment + multi-warehouse (roadmap #20)** — `Warehouse` (code/name/city/state/country/
  priority/active) + `WarehouseStock` (per-warehouse per-SKU quantity, unique warehouse+sku) +
  `Shipment` (orderId, orderTrackingNumber, warehouse FK, carrier, trackingNumber, status,
  shippedAt/deliveredAt, note) entities; `V16` migration creates all three tables (indexed on
  `shipment.order_id`, MySQL-IT-validated). `FulfillmentService` is the single home for the logic:
  `fulfillmentOptions(orderId)` ranks active warehouses by how many of the order's lines they can
  fully cover (priority as tiebreak — lower ships first); `createShipment()` draws down the chosen
  warehouse's stock (clamped at zero, never negative) and sets the shipment PENDING (picked/packed,
  not yet shipped) or SHIPPED depending on whether carrier/tracking were supplied, advancing the
  order's status forward along `Received→Processing→Shipped→Delivered` — **forward-only**: it never
  downgrades an already-further-along order and never touches a `Cancelled` order; `updateShipmentStatus()`
  advances a shipment PENDING→SHIPPED→DELIVERED the same way; `trackShipments(trackingNumber, email)`
  is the public customer-facing lookup — email must match the order's customer, with a generic
  mismatch error (no user-enumeration, matching the `ReturnService` precedent from #3's returns
  feature). RBAC (#19) extends naturally: `OrderManager` can create/advance shipments (`POST
  .../shipments`, `PUT /api/admin/shipments/**`) alongside its existing order/return authority, but
  warehouse configuration (`/api/admin/warehouses/**`) stays Admin-only — inserted ahead of the
  general `/api/admin/**` catch-all, same ordered-matcher pattern as #19. `DataLoader` seeds two
  warehouses ("East Coast Fulfillment"/"West Coast Fulfillment", priority 0/1) and splits each SKU's
  existing stock 60/40 between them (idempotent — skips if any warehouse exists). Frontend: new admin
  **Warehouses** page (`/admin/warehouses`, mirrors `admin-tax-shipping`'s two-card layout — warehouse
  list+form on the left, per-warehouse stock table with inline edit on the right, reusing
  `admin-inventory`'s save-per-row pattern); the Orders page gains this codebase's **first
  expand-in-place table row** — a "Fulfillment" toggle per order reveals existing shipments (inline
  carrier/tracking + Mark shipped/Mark delivered actions) and a "fulfill from a warehouse" form
  defaulting to the best-coverage option; customer-facing `order-history` gets a read-only
  shipment-tracking block (badge/carrier/tracking#/dates) between the order timeline and the Returns
  section — gated on `email` being set exactly like Returns already was (only populates once Okta is
  configured and the customer is signed in; demo mode shows the timeline without tracking detail, an
  accepted limitation carried over unchanged from #3, not a new gap). Two real bugs caught and fixed
  during browser verification: (1) after creating a shipment, the "fulfill from a warehouse" dropdown
  reset to blank instead of defaulting to the next suggested warehouse, because the post-create
  options-refresh callback updated the options list but never re-synced `shipmentForm.warehouseId`
  from it (only the initial-open code path did) — fixed by applying the same default-to-best-option
  logic in both places; (2) a **pre-existing** bug on the Orders page, not introduced by this feature
  but newly exposed by it: the order-status `<select [value]="o.status">` used a plain property
  binding, which raced against its own `@for`-generated `<option>` children on first paint and
  silently failed to select the right option — confirmed by inspecting the live DOM (`select.value`
  stayed `"Received"`, `selectedIndex 0`, while the badge correctly showed the true status) even on a
  hard reload. It had gone unnoticed because status was previously only ever changed by interacting
  with that same dropdown (so the browser's own last-clicked option coincidentally matched); once
  shipment-driven status changes (this feature) could update status from a different code path, the
  desync became visible. Fixed by switching it to `[(ngModel)]`/`(ngModelChange)`, this codebase's
  established pattern for every other live-bound `<select>` — **lesson for future frontend work
  here: never bind a native `<select>`'s selection with plain `[value]`, always `[(ngModel)]` (or a
  reactive-forms control), even for a "simple" dropdown.** Browser-verified end to end: full shipment
  lifecycle (create → Mark shipped → Mark delivered, order status badge syncing forward-only at each
  step and surviving an unrelated backend restart with zero data loss); warehouse add and the delete
  guard (blocked with a clear error once a warehouse has shipments, succeeds cleanly on one that
  doesn't). 9 new backend unit tests (`FulfillmentServiceTest`) + 3 new
  `SecurityFilterChainIntegrationTest` RBAC cases + full `./mvnw clean package` (146 tests, incl. the
  real-MySQL IT validating `V16`) + 17 frontend unit tests + production build all green. (No new E2E
  spec added this round — verification leaned on the backend suite + live browser testing instead.)
- ✅ **Multi-tenancy foundation, Milestone A (roadmap #21)** — a real, narrow, end-to-end-verified
  tenant boundary around the core catalog/checkout path only: `Product`, `ProductCategory`,
  `ProductVariant`, `Customer`, `Order`, `OrderItem`, `Address`. The remaining ~23 entities, the whole
  frontend, a platform-superadmin tier, and an Okta tenant claim are **explicitly deferred** to future
  Milestones B–D — per the RBAC feature (#19)'s precedent, partial isolation is worse than none because
  it implies a guarantee it doesn't have, so this stayed scoped rather than touching everything shallowly.
  New `Tenant` entity (slug/displayName/contactEmail/active/plan) + `V17` migration: creates `tenant`,
  seeds one `demo` row, and adds a nullable `tenant_id` (real FK + index — a deliberate exception to the
  codebase's mostly-FK-less convention, since this is the single most important isolation boundary in
  the schema) to the 7 scoped tables, backfilling every existing row to the demo tenant (zero regression).
  **Isolation is enforced explicitly, not via Hibernate's `@TenantId`** — that discriminator approach was
  built and then deliberately ripped back out after empirically confirming it breaks Spring Data JPA
  repository bootstrap for *every* repository in the app (not just tenant-scoped ones): Hibernate
  multi-tenancy requires any `EntityManager` — including ones Spring Data opens at startup to check named
  queries — to resolve a tenant id up front, which fails hard on a fresh/empty database before any tenant
  row exists (confirmed via a real stack trace: `HibernateException: SessionFactory configured for
  multi-tenancy, but no tenant identifier specified`, thrown from plain `ProductRepository` bean creation
  in an H2 test). The plan had pre-authorized dropping this exact layer if it proved friction-prone, since
  the explicit mechanism alone is already sufficient — so `@TenantId`, `TenantHibernateConfig` and
  `TenantIdentifierResolver` were removed; `tenant_id` stays a plain mapped column everywhere. **Request-time
  resolution**: `TenantContext` (thread-local, mirrors `RequestIdFilter`'s idiom) + `TenantResolutionFilter`
  (`@Order(HIGHEST_PRECEDENCE+5)`) resolves `X-Tenant-Id` header/`?tenant=` query param → subdomain (against
  `app.tenant.base-domain`) → `app.tenant.default-slug` (default `demo`) fallback, 404ing unknown/inactive
  tenants; `RateLimitFilter`'s key gained a tenant dimension. **`TenantResourceGuardFilter`**
  (`@Order(+6)`) closes the one gap an explicit query-level predicate can't reach: Spring Data REST's
  single-item resources (`GET/PUT/PATCH/DELETE /api/products/{id}`, `/api/product-category/{id}`,
  `/api/orders/{id}`) go straight to `findById` with no query-building step to hook, so this filter does a
  cheap `existsByIdAndTenantId` check first and 404s (never 403) a cross-tenant id. `ProductQueryService`
  adds an explicit tenant predicate to every faceted-search specification (and to its `@Cacheable` key —
  a real cross-tenant cache-leak risk once catalogs diverge); `OrderRepository.sumTotalRevenue` takes an
  explicit `tenantId` param. A real cross-tenant bug was caught and fixed while wiring this up:
  `CustomerRepository.findByEmail` merged same-email customers globally — renamed to
  `findByEmailAndTenantId` (email is deliberately not unique) and fixed everywhere it was called
  (`CheckoutServiceImpl`, `AccountController`, `LoyaltyService`, `NewsletterService`, `ReferralService`).
  `CheckoutServiceImpl.placeOrder` stamps `tenantId` (from `TenantContext`) onto the customer, order, every
  order item, and both addresses before the cascading save. `DataLoader` creates the demo tenant on first
  boot and stamps it onto every seeded category/product/variant; the other ~12 seed methods are untouched
  since their target entities don't carry `tenant_id` in this milestone. Tests: new
  `TenantResolutionFilterTest` (precedence order, unknown-tenant 404, context cleared post-request) +
  `CustomerRepositoryTest` (same email never merges across tenants) + an extended `MySqlIntegrationTest`
  seeding a second tenant via raw JDBC and proving, through the real filter chain, isolation both ways on
  faceted search, a 404 on a cross-tenant item GET, and revenue scoping. **Runtime-verified** via
  `docker compose up --build`: no-header request still renders the demo catalog with zero regression;
  a manually-inserted second tenant's product is invisible to the demo tenant's search and 404s on direct
  item GET, while visible/200 under its own `X-Tenant-Id`. 152 backend tests (H2/unit) + the real-MySQL IT
  (validating `V17`) all green via full `./mvnw package`.
- ✅ **Multi-tenancy, Milestone B (roadmap #21)** — closes the gap Milestone A's own `TenantRepository`
  javadoc had been deferring: there was no way to create/list/edit/deactivate a `Tenant` at all except a
  raw SQL insert or `DataLoader`'s one-time `demo` seed. Adds a **platform-superadmin tier** above the
  tenant-scoped `Admin`/`OrderManager`/`Viewer` roles (#19): a new `SuperAdmin` authority, read through the
  *exact same* JWT groups-claim mechanism (`adminAwareConverter()`) — no new Okta claims config, just a new
  role name — gates a new `/api/platform/tenants` CRUD (`PlatformTenantController`/`PlatformTenantService`,
  no schema change, `Tenant.plan` already existed unused since V17). **Deliberately did not build a literal
  new Okta custom claim** for "tenant identity": research confirmed it would need external per-user Okta
  console config (or the Management API) and collides with a real ordering problem —
  `TenantResolutionFilter` runs at `@Order(HIGHEST_PRECEDENCE+5)`, *before* Spring Security parses the JWT,
  so nothing that early can read a claim. Instead, reused the app's own existing escape hatch: a
  superadmin "views as" a tenant by having the frontend send the same `X-Tenant-Id` header
  `TenantResolutionFilter` already treats as highest-precedence — same substitution-over-new-mechanism
  call as Milestone A dropping Hibernate `@TenantId`. `/api/platform/**` is excluded from tenant
  resolution/guarding entirely by adding it to `TenantResolutionFilter`'s existing `SKIPPED_PREFIXES`
  array (same mechanism already used for `/actuator`/`/swagger-ui` — not a new pattern); this isn't
  cosmetic — without it, a superadmin's browser still carrying a stale `X-Tenant-Id` for a tenant it just
  deactivated would 404 *inside* the filter before ever reaching the `SuperAdmin`-gated route, locking
  them out exactly when cleanup is needed. `AdminController.me()`'s open/no-Okta fallback now includes
  `SuperAdmin` alongside `Admin`, matching the existing graceful-degradation default. Frontend: new
  `/platform` route tier (`PlatformLayout`/`PlatformTenants`, `PlatformService`), a `TenantContextService`
  signal threaded through `authInterceptor` (sets `X-Tenant-Id` on `/admin` calls only — `/platform`
  itself is tenant-agnostic), and an admin-sidebar "Viewing: {slug} (platform) · Reset" banner + a
  `SuperAdmin`-gated "Platform" nav link (courtesy UI, not the enforcement boundary — same principle as
  #19's role badge). **Operational note**: a real superadmin needs *two* Okta group memberships —
  `SuperAdmin` plus an admin-tier role — to both manage tenants and browse a tenant's back office via the
  switcher (documented in `docs/SECURITY.md`). Explicitly out of scope: extending `tenant_id` to the other
  ~22 entities, and roadmap #22 (tenant billing) itself. Tests: extended `TenantResolutionFilterTest`
  (platform routes skip resolution entirely, zero interaction with `TenantResolutionService`), extended
  `SecurityFilterChainIntegrationTest` (anonymous/regular-admin/superadmin cases against
  `/api/platform/tenants`), new `PlatformTenantServiceTest`. 165 backend tests (3 auto-skipped without
  Docker — the real-MySQL IT) + 17 frontend unit tests + full `./mvnw clean package` + production
  `ng build` all green. **Runtime-verified**: created a second tenant ("Verify Co") via `/platform/tenants`;
  deactivating it (soft — `active=false`, never a hard delete) makes its storefront requests 404
  immediately while `demo` (explicit header or no header) keeps working; the audit log records both the
  `PLATFORM_TENANT_CREATE` and `PLATFORM_TENANT_DEACTIVATE` entries.
- ✅ **Multi-tenancy, Milestone B follow-up — admin back office was never actually tenant-scoped
  (roadmap #21)** — live verification of the tenant switcher above surfaced a real **pre-existing gap
  from Milestone A**, not a Milestone B bug: `AdminService`/`AdminController`/`AnalyticsService` never
  consulted `TenantContext` for reads (only `OrderRepository.sumTotalRevenue` did), so switching "viewed
  as" tenant changed the `X-Tenant-Id` header correctly but the back office kept showing `demo`'s data —
  contradicting Milestone A's own stated principle that partial isolation is worse than none. Worse,
  `AdminService.createProduct()`/`createCategory()` never stamped `tenantId` on newly created rows, so
  every admin-created product/category since Milestone A shipped had `tenant_id = null` and was
  **invisible to the tenant-scoped storefront search** (`ProductQueryService`'s `cb.equal(tenantId, …)`
  never matches `NULL`). Fixed by adding tenant-scoped derived-query methods (`findAllByTenantId`,
  `findByIdAndTenantId`, `countByTenantId`, etc., matching the existing `existsByIdAndTenantId`/
  `findByEmailAndTenantId` convention) to `ProductRepository`/`OrderRepository`/`CustomerRepository`/
  `ProductCategoryRepository`, rewiring every read in `AdminService`, `AdminController.categories()`,
  and `AnalyticsService` through `TenantContext.currentTenantId()`, and stamping the tenant onto new
  products/categories at creation (same pattern `CheckoutServiceImpl` already used for customers/orders).
  Removed two `OrderRepository` methods left dead by this change (`findAllByOrderByDateCreatedDesc`,
  `findByDateCreatedGreaterThanEqual`) after confirming no remaining callers. Deliberately bounded to
  the 7 entities Milestone A already scoped — no new migration, no change to non-tenant admin surfaces
  (inventory, reviews, coupons, promotions, gift cards, tax/shipping, content, warehouses, audit log).
  Tests: extended `AdminServiceTest` (incl. new regression tests asserting the exact tenant id threads
  through to the repository call / gets stamped on save) and `AnalyticsServiceTest` for the new method
  signatures. Full `./mvnw clean package` (167 tests incl. the real-MySQL IT) green. **Runtime-verified**
  via the browser: demo's dashboard/products/orders show its real data (100 products, orders, revenue);
  switching "viewed as" to a second tenant zeroes out every admin surface (dashboard stats, products
  list, orders list) instead of mirroring demo's; Reset correctly restores demo's original view.

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
- Second full-stack instance on alt ports (runs alongside the above without clashing): `./deploy.sh` (repo-root `compose.deploy.yaml`) → http://localhost:4251, API 8586, MySQL 3308. `./deploy.sh down` to stop. See `docs/DEPLOYMENT.md`.
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
