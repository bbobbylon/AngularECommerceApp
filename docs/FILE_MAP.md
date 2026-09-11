# File Map

A per-file guide to this repository: what each file does, what it talks to, and which roadmap
feature/milestone it belongs to. Complements the higher-level `ARCHITECTURE.md` (component/pattern
view) and `FLOWS.md` (request/data-flow view) — read those first for the big picture, come here when
you need to know "what is this specific file for, and who else touches it."

This map won't stay perfectly current as the app grows — treat it as a strong starting orientation,
not a guarantee. When in doubt, `git grep` the class/symbol name to see current real usage. Roadmap
feature numbers refer to the enumerated list in `CLAUDE.md`'s "Current state" section, which has the
full narrative (why a feature was built, what bugs it caught, how it was verified) for every entry
named below.

---

## Backend — `backend/src/main/java/com/bob/ecommerceangularapp/`

Package-per-responsibility layout: `entity` (JPA models) → `dao` (Spring Data repositories) →
`service` (business logic) → `controller` (thin REST endpoints), with `dto` as the wire-format layer
between `controller`/`service` and the outside world, and `config`/`bootstrap`/`email` as
infrastructure that doesn't belong to any one feature.

### Root

- **`EcommerceAngularAppApplication.java`** — the `@SpringBootApplication` entry point. Also carries
  `@EnableScheduling` (backs `AbandonedCartScheduler`/`WeeklyAdScheduler` in `service/`) and
  `@EnableCaching` is actually on `config/CacheConfig.java`, not here — this class stays a pure
  bootstrap point, no logic of its own.

### `bootstrap/` — one-time startup data seeding

- **`DataLoader.java`** — the app's single `CommandLineRunner`. Runs on every boot; every seed method
  is idempotent (a `count() > 0` guard) so restarting never duplicates data. Order matters: it creates
  the demo `Tenant` (roadmap #21) first, then categories → products → variants, then the
  independent feature seeds (reviews, coupons, promotions, gift cards, tax/shipping, content,
  warehouses) which all stamp the demo tenant onto their rows. Also self-heals older databases
  (newsletter/sale-price backfills) without letting one failing step crash the whole catalog seed.

### `config/` — cross-cutting infrastructure (not business logic)

- **`SecurityConfig.java`** — the two `SecurityFilterChain` beans (an "open" chain when no Okta issuer
  is configured, a "secured" JWT-validating chain otherwise), the centralized CORS bean (reads
  `app.cors.allowed-origins`), response-header hardening (CSP/HSTS/X-Frame-Options/etc., with a
  path-scoped relaxed CSP for Swagger UI), and the RBAC `authorizeHttpRequests` matchers (roadmap
  #19: `Admin`/`OrderManager`/`Viewer`/`SuperAdmin`, ordered specific-before-general). The single most
  important file for understanding what's public vs. gated in this app.
- **`TenantContext.java`** — a thread-local holding the current request's resolved tenant id (roadmap
  #21). `set()`/`clear()` are public specifically so plain-Mockito service unit tests can bind a
  tenant without running the real filter chain — see any `*ServiceTest` with
  `@BeforeEach setTenantContext()`.
- **`TenantResolutionFilter.java`** — resolves `X-Tenant-Id` header → `?tenant=` query param →
  subdomain → default-slug fallback into `TenantContext`, 404ing unknown/inactive tenants. Runs at
  `@Order(HIGHEST_PRECEDENCE+5)` — **before** Spring Security ever parses a JWT, which is why a real
  Okta tenant claim was ruled out as a mechanism (see `CLAUDE.md` Milestone B). Skips
  `/platform/**`/`/actuator`/`/swagger-ui` via its `SKIPPED_PREFIXES` list.
- **`TenantResourceGuardFilter.java`** — closes the one isolation gap explicit query predicates can't
  reach: Spring Data REST's single-item `GET/PUT/PATCH/DELETE /api/products/{id}` etc. go straight to
  `findById` with no query-building step to intercept, so this filter does a cheap
  `existsByIdAndTenantId` pre-check and 404s a cross-tenant id before the request reaches SDR.
- **`RateLimitFilter.java`** — Caffeine-backed per-IP-and-tenant rate limiting (30/min) + a 64 KB body
  cap on the public write endpoints (`/api/reviews|coupons|newsletter`), returning 429/413.
- **`RequestIdFilter.java`** — stamps an `X-Request-Id` (incoming or generated) into MDC so every log
  line for a request can be correlated; echoes it back on the response.
- **`CacheConfig.java`** — the Caffeine `CacheManager` backing `catalogSearch` (faceted search results,
  evicted on admin product writes) and `tenantLookup` (slug→`Tenant`, resolved on every request).
- **`MyDataRestConfig.java`** — locks Spring Data REST's auto-exposed catalog resources
  (`Product`/`ProductCategory`/`Country`/`State`/`Order`) to read-only and exposes their ids in JSON
  responses (off by default in SDR).
- **`OpenApiConfig.java`** — Swagger/OpenAPI metadata (title/description/contact) + registers the
  Bearer-JWT security scheme so Swagger UI's Authorize button works against the secured endpoints.

### `email/` — gated transactional/marketing email

- **`EmailService.java`** — sends welcome/order-confirmation/settings-changed/back-in-stock/
  abandoned-cart/weekly-ad emails via `spring-boot-starter-mail`. Gated on SMTP config being present —
  every call is a no-op (logged, not thrown) when it isn't, so the app runs fully without email setup.
  Called from `AccountController`, `StockNotificationService`, `AbandonedCartService`,
  `CheckoutServiceImpl` (order confirmation), `WeeklyAdScheduler`.
- **`EmailTemplates.java`** — the actual HTML/text template strings for each email
  `EmailService` sends. Pure string-building, no I/O.

### `entity/` — JPA-mapped domain model

One class per DB table (Flyway-owned schema, `ddl-auto=validate`). Most of the catalog/customer/order
core (`Product`, `ProductCategory`, `ProductVariant`, `Customer`, `Order`, `OrderItem`, `Address`) and
every entity added since roadmap #21 Milestone A carries a nullable `tenant_id` (FK + index) — see the
Multi-tenancy section below rather than repeating that on every line here.

- **`Product.java`** / **`ProductCategory.java`** — the catalog core; `Product` has a `@ManyToOne`
  category, denormalized `averageRating`/`reviewCount` (kept in sync by `ReviewService`), sale
  pricing (`originalPrice`), and a LAZY `additionalImages` side-table collection (galleries feature).
- **`ProductVariant.java`** — optional per-SKU variant of a `Product` (color/size, price override,
  own stock) — roadmap #1. Checkout decrements variant stock when present; base-product stock becomes
  display-only once a product has variants (same convention followed by `InventoryService`, #15).
- **`Customer.java`** — email-keyed (deliberately not unique — see `CustomerRepository`), holds
  loyalty (`loyaltyPoints`/`lifetimePoints`, #5), referral (`referralCode`, #6), and newsletter
  (`newsletterSubscribed`/`unsubscribeToken`, M6) fields directly rather than in side tables.
- **`Order.java`** / **`OrderItem.java`** — the placed-order record. `Order` accumulates a field per
  discount/adjustment mechanism added over time (`couponCode`/`discountAmount`, `promotionName`/
  `promotionDiscount`, `giftCardCode`/`giftCardAmount`, `loyaltyPointsRedeemed`/`loyaltyDiscount`,
  `shippingAmount`/`taxAmount`, `paymentIntentId`) — all nullable, all additive, none required a
  breaking change to existing rows.
- **`Address.java`** / **`Country.java`** / **`State.java`** — shipping/billing address + the
  read-only country/state reference data seeded once and exposed read-only via SDR
  (`CountryRepository`/`StateRepository`).
- **`Coupon.java`** (#0/checkout) · **`Promotion.java`** (#16) — discount mechanisms; `Coupon` needs a
  customer-entered code, `Promotion` applies automatically to eligible orders. Both tenant-scoped
  since Milestone C with a composite `(tenant_id, code)` unique on `Coupon.code`.
- **`GiftCard.java`** (#4) — store-credit code with initial/remaining balance; redemption is clamped
  to balance in `GiftCardService`.
- **`TaxRate.java`** / **`ShippingMethod.java`** (#2) — checkout-quote inputs, consulted by
  `TaxShippingService.quote()`.
- **`ReturnRequest.java`** (#3) — return/refund lifecycle (`REQUESTED→APPROVED/DENIED→REFUNDED`).
- **`LoyaltyTransaction.java`** (#5) / **`Referral.java`** (#6) — append-only ledgers; a `Customer`'s
  point balance is a mutable field on `Customer` itself, these are the audit trail behind it.
- **`StockNotification.java`** (#7) / **`AbandonedCart.java`** (#8) — email-keyed lifecycle rows for
  back-in-stock waitlists and idle-cart recovery, each with a `notified`/`reminded` flag so an email
  fires at most once per state change.
- **`SavedAddress.java`** / **`SavedPaymentMethod.java`** (#9) — account-page saved data;
  `SavedPaymentMethod` stores only Stripe references (brand/last4/expiry/Stripe id) — never raw card
  data, a PCI requirement.
- **`Review.java`** (feature set) / **`WishlistItem.java`** (feature set) — product reviews (drive
  `Product.averageRating`/`reviewCount`) and the email-keyed wishlist/favorites list.
- **`NewsletterSubscriber.java`** (M6) — newsletter opt-in list, unsubscribe token; tenant-scoped with
  a composite `(tenant_id, email)` unique since Milestone D.
- **`InventoryAdjustment.java`** (#15) — audit trail for every stock change (manual edit or CSV
  import) — sku/prev/new/delta/source/note/timestamp.
- **`FaqEntry.java`** / **`SiteBanner.java`** (#17) — the simple-CMS content; `SiteBanner` is a
  singleton-**per-tenant** row (`ContentService.currentBanner()` uses `findFirstByTenantId`, not a
  hardcoded id).
- **`AuditLogEntry.java`** (#19) — the *global* cross-cutting admin-mutation ledger (distinct from the
  domain-specific `InventoryAdjustment` ledger — they answer different questions).
- **`Warehouse.java`** / **`WarehouseStock.java`** / **`Shipment.java`** (#20) — multi-warehouse
  fulfillment: per-warehouse per-SKU stock, and the shipment record
  (`PENDING→SHIPPED→DELIVERED`) tied to an order.
- **`Tenant.java`** (#21 Milestone A) — the tenant row itself (slug/displayName/contactEmail/active/
  plan). Every other tenant-scoped entity's `tenant_id` FKs here. Not itself tenant-scoped (there's
  only ever one row per tenant, by definition).

### `dao/` — Spring Data JPA repositories

Almost entirely derived-query interfaces (no impl classes needed) — one repository per entity, named
`XRepository`. Tenant-scoped entities gained matching `findByXAndTenantId`/`findAllByTenantId`/
`findByIdAndTenantId` methods incrementally across roadmap #21 Milestones A–D (see that section
below) — a repository predating #21 and one written after it look different mainly in whether their
query methods take a `tenantId` parameter. `ProductRepository` is additionally a
`JpaSpecificationExecutor<Product>` (backs `ProductQueryService`'s faceted search spec-building).
`CountryRepository`/`StateRepository` are plain read-only SDR-exposed repos with no custom methods.

### `service/` — business logic (the layer controllers should stay thin around)

- **`CheckoutService.java`** (interface) / **`CheckoutServiceImpl.java`** — the checkout pipeline's
  single home, and the most-extended file in the codebase: every discount/loyalty/referral/
  abandoned-cart feature added since M2 hooks into `placeOrder()` here, in a fixed order (subtotal →
  coupon → promotion → shipping → tax → gift card → rewards → amount due, then Stripe charge → order
  save → loyalty award → referral record → abandoned-cart mark-recovered → confirmation email).
  Stamps `TenantContext`'s tenant id onto the customer/order/order-items/addresses before the
  cascading save (#21 Milestone A). When adding a new checkout-adjacent feature, extend this
  constructor + its dedicated unit test, matching every prior feature's pattern.
- **`ProductQueryService.java`** — faceted search (`JpaSpecificationExecutor` predicates for category/
  keyword/price/in-stock/on-sale/rating/sort), `@Cacheable` keyed to include the tenant id. Backs
  `/api/catalog/search`.
- **`ProductVariantService.java`** — variant reads (price/image resolution), admin
  replace-by-list upsert, and the checkout stock-decrement logic (clamped at zero).
- **`AdminService.java`** — the admin dashboard's aggregate reads/writes (stats, product/category
  CRUD, review moderation) — tenant-scoped since the Milestone B follow-up.
- **`AnalyticsService.java`** (#18) — pure read-side aggregation (Java streams, not JPQL projections,
  matching this codebase's established style) over `Order`/`OrderItem`/`Product`.
- **`AuditLogService.java`** (#19) — `record(...)` is called from every admin mutation controller;
  `resolveActor()` reads the plain `Authentication` method parameter Spring MVC auto-resolves,
  defaulting to `"anonymous"` when unauthenticated.
- **`CouponService.java`** (checkout) / **`PromotionService.java`** (#16) / **`GiftCardService.java`**
  (#4) — the three discount mechanisms; all tenant-scoped since Milestone C
  (`findByIdAndTenantId(...).orElseThrow(...)` on every mutation).
- **`TaxShippingService.java`** (#2) — `quote()` is the single source of truth for tax/shipping math,
  consulted by both the storefront's live quote endpoint and the server-side recompute at order time;
  also consults `PromotionService.findBest()`.
- **`LoyaltyService.java`** (#5) — `award()`/`redeem()` (capped by balance *and* order total),
  `summary()` (tier/progress). Reused by `ReferralService.grantPoints()` for referral rewards.
- **`ReferralService.java`** (#6) — assigns/reads referral codes, `recordReferral()` rewards both
  parties on a new customer's first qualifying order (self-referral + already-referred guards).
- **`StockNotificationService.java`** (#7) — subscribe (deduped)/notify-restocked, triggered from
  `AdminService.updateProduct` and `ProductVariantService.replaceVariants`.
- **`AbandonedCartService.java`** (#8, paired with **`AbandonedCartScheduler.java`**) — cart-snapshot
  capture on checkout email-blur, `remindStale()` (cron-driven) emails carts idle past a threshold,
  `markRecovered()` is called from `CheckoutServiceImpl` on order placement.
- **`AddressBookService.java`** / **`PaymentMethodService.java`** (#9) — account-page saved
  addresses/cards; `PaymentMethodService` wraps Stripe SetupIntents, gracefully degrading without a
  Stripe key.
- **`InventoryService.java`** (#15) — the merged product+variant stock view, single-edit, and the
  hand-rolled CSV import/export.
- **`ContentService.java`** (#17) — the CMS reads/writes (`currentBanner()`/FAQ list), tenant-scoped
  since Milestone C.
- **`ReviewService.java`** — CRUD + moderation for `Review`, keeps `Product.averageRating`/
  `reviewCount` in sync on every write.
- **`FulfillmentService.java`** (#20) — warehouse ranking (`fulfillmentOptions`), shipment creation/
  status advance (forward-only order-status ladder), and the customer-facing `trackShipments()`
  lookup (derives tenant identity from the resolved, email-verified `Order`, not ambient
  `TenantContext` — same principle as `ReturnService.createReturn`).
- **`ReturnService.java`** (#3) — customer-initiated returns (email must match the order) + admin
  approve/deny, issuing a real Stripe refund when configured.
- **`SystemHealthService.java`** — backs the admin dashboard's "System health" card (wraps actuator
  health data for display).
- **`SitemapService.java`** — generates `sitemap.xml` from the live catalog (roadmap #11); the
  backend does this because the plain-SPA frontend has no server-side rendering to do it from.
- **`PlatformTenantService.java`** (#21 Milestone B) — `SuperAdmin`-only tenant CRUD
  (create/list/edit/deactivate a `Tenant` row).
- **`TenantResolutionService.java`** (#21 Milestone A) — the actual slug/subdomain→`Tenant` lookup
  logic used by `TenantResolutionFilter` (kept as a separate `@Cacheable` service so the filter itself
  stays thin).
- **`NewsletterService.java`** (M6) — subscribe/unsubscribe + `WeeklyAdScheduler`'s cron-driven send.

### `controller/` — thin `@RestController`s

Controllers do request/response mapping and delegate everything else to a `service/` — none of them
touch a repository directly (confirmed by grep during Milestone C). Grouped by area:

- **Public storefront**: `ProductFilterController` (faceted search + variants, `/api/catalog/**`),
  `CheckoutController` (`/api/checkout/**` — quote, gift-card check, shipping methods, payment
  intent, purchase), `ReviewController`, `CouponController` (validate), `GiftCardController`
  (check), `WishlistController`, `StockNotificationController`, `AbandonedCartController`,
  `ReferralController`, `LoyaltyController`, `ContentController` (banner/FAQ reads),
  `NewsletterController`, `ReturnController` (customer-side create), `ShipmentController`
  (customer-side track), `SitemapController` (`sitemap.xml`, outside `/api`).
- **Account (Okta-gated)**: `AccountController` (profile/preferences), `AccountAddressController`,
  `AccountPaymentMethodController`.
- **Admin (`/api/admin/**`, RBAC-gated per `SecurityConfig`)**: `AdminController` (dashboard
  stats/categories/review moderation), `AdminProductController`, `AdminOrderController`,
  `AdminCouponController`, `AdminPromotionController`, `AdminGiftCardController`,
  `AdminTaxShippingController`, `AdminContentController`, `AdminInventoryController`,
  `AdminAuditLogController`, `AdminAnalyticsController`, `AdminFulfillmentController`
  (warehouses), `AdminShipmentController`, `AdminReturnController`, `AdminSystemController`.
- **Platform (`/api/platform/**`, `SuperAdmin`-only, tenant-resolution-exempt)**:
  `PlatformTenantController`.
- **Cross-cutting**: `GlobalExceptionHandler` (`@RestControllerAdvice` for this codebase's own
  controllers — SDR keeps its own error handling for the auto-exposed catalog endpoints).

### `dto/` — request/response records for the hand-written controllers

Mostly small `record`s, one (or a request/response pair) per endpoint — not enumerated individually
here since their names/fields are usually self-explanatory and they carry no logic. Grouped by area
they serve: checkout (`Purchase`, `PurchaseResponse`, `QuoteRequest`/`QuoteResponse`, `PaymentInfo`),
admin CRUD requests/views (one pair per admin-managed entity — `AdminProductRequest`,
`CouponRequest`/`CouponResponse`, `PromotionRequest`, `AdminGiftCardRequest`/`GiftCardView`,
`TaxRateRequest`, `ShippingMethodRequest`/`ShippingMethodView`, `FaqEntryRequest`,
`SiteBannerRequest`, `WarehouseRequest`, `CreateShipmentRequest`/`ShipmentView`,
`CreateReturnRequest`/`ReturnRequestView`, `ReturnDecisionRequest`, `InventoryAdjustmentRequest`/
`InventoryAdjustmentView`/`InventoryItemView`, `CsvImportResult`, `CategoryRequest`,
`AdminVariantRequest`, `ProductVariantView`), account (`AccountUpdateRequest`, `AccountPreferences`,
`SavedAddressRequest`, `RecordPaymentMethodRequest`, `SetupIntentResponse`), analytics
(`AnalyticsSummary`, `RevenuePoint`, `TopProduct`, `StatusCount`), loyalty/referral
(`LoyaltySummary`, `LoyaltyTransactionView`, `ReferralSummary`), audit/platform (`AuditLogView`,
`CurrentAdminView`, `PlatformTenantRequest`), and small standalone shapes
(`ProductCardView`, `PageResponse`, `AppliedPromotion`, `ReviewRequest`/`ReviewView`/`ReviewSummary`,
`StockNotificationRequest`, `SubscribeRequest`, `WishlistSyncRequest`, `AbandonedCartRequest`,
`SystemHealth`, `StockQuantity`, `WarehouseStockRow`, `FulfillmentOption`, `AdminStats`,
`AdminOrderView`).

---

## Cross-cutting concerns (span many files — don't fit a single-file entry)

- **Multi-tenancy (roadmap #21)**: `TenantContext` (thread-local) + `TenantResolutionFilter` resolve a
  tenant once per request; every tenant-scoped `service/` method reads
  `TenantContext.currentTenantId()` and every corresponding `dao/` repository has a matching
  `...AndTenantId` derived-query method. Built incrementally: Milestone A scoped the 7 core
  catalog/checkout entities, B added the `SuperAdmin`/`/api/platform` tier + fixed a back-office
  scoping gap, C scoped the 7 financial/storefront-config entities (coupons/promotions/gift
  cards/tax/shipping/banner/FAQ), D scoped the final 14 customer/ops entities. See `CLAUDE.md`'s
  per-milestone bullets for the full history and the bugs each pass caught.
- **Security filter chain order** (`SecurityConfig` + the `config/` filters above): request-id →
  tenant resolution → tenant resource guard → rate limiting → Spring Security (JWT parsing, if a
  secured chain is active) → RBAC matchers → controller. A filter that needs to run before JWT
  parsing (tenant resolution) can never read a JWT claim — this is *why* `X-Tenant-Id` is a header,
  not a token claim.
- **Flyway migrations** (`src/main/resources/db/migration/V{n}__*.sql`): the sole schema-change
  mechanism, one file per entity change, never edited after being applied. `ddl-auto=validate`
  fails the app fast on any entity/schema mismatch. Tests run on H2 with Flyway disabled instead
  (`ddl-auto=create-drop`). `MySqlIntegrationTest` (Testcontainers) is what actually proves a new
  migration works against real MySQL — auto-skips without Docker.
- **The checkout pipeline**: `CheckoutServiceImpl` is the single point where nearly every commerce
  feature (coupons, promotions, gift cards, loyalty, referrals, tax/shipping, abandoned-cart
  recovery, Stripe) composes together — see its entry above.

---

## Frontend — `frontend/angular-ecommerce/src/app/`

Angular 21, **standalone components** throughout (no `NgModule`s) — every component/service is a
single self-contained `.ts` (+ template/styles), wired together by DI and lazy-loaded routes.

### Root

- **`app.ts`** — the root shell component (nav/header/footer chrome, the dynamic CMS banner fetch,
  currency/language selectors, install-prompt, toast container).
- **`app.config.ts`** — `ApplicationConfig`: HTTP client + `authInterceptor`, router, Okta auth
  provider, the service-worker registration (gated on `isDevMode()`, not `environment.production` —
  this project's `environment.ts` has no `fileReplacements`, so that flag is always `false`), and a
  `provideAppInitializer` that awaits `ConfigService.load()` before the app renders.
- **`app.routes.ts`** — the full route table; every route is `loadComponent`-lazy so each page ships
  as its own chunk. `devOrAuthGuard` gates the Okta-only routes (falls open when Okta isn't
  configured, so the app stays fully clickable in local dev).

### `auth/` — Okta wiring

- **`okta-config.ts`** — the Okta OIDC config object (issuer/clientId/redirectUri); ships with valid-
  shaped placeholders so the app boots without a real Okta org.
- **`dev-auth.guard.ts`** — `isOktaConfigured()` (true only once the placeholders are replaced) +
  `devOrAuthGuard`, which allows all access when Okta isn't configured and otherwise delegates to
  Okta's real guard.

### `common/` — plain data shapes + two small pipes

TypeScript interfaces mirroring the backend's DTOs/entities as seen by the frontend
(`product.ts`, `product-category.ts`, `product-variant.ts`, `customer.ts`, `address.ts`, `country.ts`,
`state.ts`, `order.ts`, `order-item.ts`, `order-history.ts`, `cart-item.ts`, `purchase.ts`,
`payment-info.ts`), plus `money.pipe.ts` (USD formatting) and `translate.pipe.ts` (the `t` pipe
backing `I18nService`, impure so language switches re-render live).

### `interceptors/`

- **`auth.interceptor.ts`** — attaches the Okta Bearer token to calls against the secured backend
  routes (`/api/orders`, `/api/account`, `/api/admin`, `/api/platform`), and separately attaches
  `X-Tenant-Id` (from `TenantContextService`) on `/api/admin` calls only — the superadmin "view as
  tenant" mechanism from roadmap #21 Milestone B.

### `validators/`

- **`luv2shop-validators.ts`** — shared reactive-forms validators (whitespace-only-string guard, used
  across checkout/account forms).

### `services/` — Angular injectables, one per backend feature area or cross-cutting frontend concern

Each mirrors its backend counterpart closely enough that the names line up:
`account.service.ts`↔`AccountController`, `admin.service.ts`↔the `Admin*Controller` family,
`checkout.service.ts`↔`CheckoutController`, `content.service.ts`↔`ContentController`,
`coupon.service.ts`↔`CouponController`, `loyalty.service.ts`↔`LoyaltyController`,
`newsletter.service.ts`↔`NewsletterController`, `platform.service.ts`↔`PlatformTenantController`,
`product.service.ts`↔`ProductFilterController` (+ the SDR catalog endpoints),
`referral.service.ts`↔`ReferralController`, `return.service.ts`↔`ReturnController`,
`review.service.ts`↔`ReviewController`, `shipment.service.ts`↔`ShipmentController`,
`order-history.service.ts`↔`GET /api/orders`, `favorites.service.ts`/`wishlist.service.ts`↔
`WishlistController`. Frontend-only services with no backend counterpart: `cart.service.ts`
(sessionStorage-backed cart, cart-item keyed by `id+variantSku`), `config.service.ts` (loads optional
runtime `/config.json`, e.g. the Stripe publishable key, before bootstrap — lets you set it without a
rebuild), `currency.service.ts` / `i18n.service.ts` (display-only currency conversion / en-es-fr
translation, persisted signals — checkout/admin stay USD-only, settlement is always USD),
`recently-viewed.service.ts` (localStorage), `seo.service.ts` (Angular Title/Meta + JSON-LD injection,
roadmap #11), `tenant-context.service.ts` (the superadmin "viewing as" tenant signal, read by
`auth.interceptor.ts`), `theme.service.ts` (light/dark, persisted + OS-preference fallback),
`toast.service.ts` (the app's one notification hub), `luv2shop-form.service.ts` (country/state
dropdown data).

### `components/` — one directory per UI surface (42 components)

**Storefront**: `product-list`, `product-details`, `product-category-menu`, `search` (header
typeahead, roadmap #14), `cart-status`, `cart-details`, `checkout`, `order-confirmation`,
`order-history`, `order-timeline`, `favorites`, `recently-viewed`, `star-rating`, `login-status`,
`newsletter-signup`, `install-prompt` (PWA banner, roadmap #12), `back-to-top`, `toast`,
`not-found`. **Static/info pages**: `about`, `contact`, `faq`, `info-page` (shared shell for
privacy/terms/shipping-returns). **Account**: `account-settings`.
**Admin** (`components/admin/`, one directory per back-office page, all behind `admin-layout`):
`admin-dashboard`, `admin-products`, `admin-product-form`, `admin-orders`, `admin-reviews`,
`admin-coupons`, `admin-promotions`, `admin-gift-cards`, `admin-tax-shipping`, `admin-content`,
`admin-inventory`, `admin-warehouses`, `admin-returns`, `admin-audit-log`, `admin-analytics`.
**Platform** (`components/platform/`, `SuperAdmin`-only): `platform-layout`, `platform-tenants`.

Each admin CRUD page follows the same shape (list + inline/form edit, save-per-row where applicable)
established by the earliest ones (`admin-coupons`, `admin-tax-shipping`) — a new admin page for a
future feature should crib the existing closest analog rather than inventing a new layout.

---

## Where to look next

- **"Why does this exist / what bugs did it catch?"** → `CLAUDE.md`'s "Current state" bullets (one
  per roadmap feature/milestone, in build order).
- **"How does a request flow end-to-end?"** → `docs/FLOWS.md`.
- **"What's the high-level component/pattern picture?"** → `docs/ARCHITECTURE.md`.
- **"What's left to build?"** → `docs/BUILD_PLAN.md` for the original milestones; roadmap items
  #22–24 in `CLAUDE.md` for what's still open on the 24-feature "sellable" plan.
