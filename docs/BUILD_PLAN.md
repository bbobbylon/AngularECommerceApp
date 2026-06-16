# Full-Stack E-Commerce (Angular + Spring Boot) — Build Plan

## Context
We're building the "Full Stack: Angular and Java Spring Boot E-Commerce Website" Udemy
course project as a vibe-coding exercise. We're on a newer stack than the videos
(**Spring Boot 4.1, Angular 21 standalone**), so we implement the course's concepts in
modern idiom; code won't match the videos line-for-line.

**Goal:** a working full-stack app — product catalog → cart → checkout → order save →
security → HTTPS → Stripe — built in milestones mirroring the course releases.

## Locked decisions
- **Backend:** Spring Boot 4.1.0, Java 21 (pinned in `backend/pom.xml`).
- **Repo layout:** `backend/` (Spring Boot) + `frontend/angular-ecommerce/` (Angular).
- **Database:** MySQL only, course-faithful. Docker MySQL via `backend/compose.yaml`;
  `spring-boot-docker-compose` auto-starts it on `spring-boot:run`; Hibernate auto-creates
  tables (`ddl-auto=update`); products/categories seeded via a `CommandLineRunner`.
- **API base path:** `/api` (Spring Data REST). The Angular app reads `environment.apiUrl`.

## Local toolchain (this machine — Windows 11)
- **JDK 24** installed. The pom pins `<java.version>21</java.version>`; javac 24 compiles to
  the 21 release target, so the build is reproducible against Java 21 semantics.
- **Node 24 / npm 11**, Angular CLI via `npx @angular/cli@21`.
- **Docker 29** available locally, so MySQL runs and full end-to-end verification works here
  (unlike the planning sandbox, where image pulls were blocked).
- Resolved versions: Spring Boot 4.1.0; Angular 21.2; ng-bootstrap 20; Bootstrap 5.3;
  Font Awesome 7.

## Build & dependency notes
- `spring-boot-docker-compose` and `mysql-connector-j` are runtime scope, so `mvn package`
  works without a DB. The default `contextLoads` test needs a datasource, so the in-sandbox
  verification uses `./mvnw -DskipTests clean package`.
- Bleeding-edge caveat: exact Boot 4.1 / Spring Data REST / Security 7 API names may differ
  from older course docs — verify against the resolved versions while coding and adapt.

---

## Milestone 0 — Project setup & cleanup ✅
- **0.1** Reorganized the accidental kitchen-sink starter into `backend/` (`git mv` of
  `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `src/`, `compose.yaml`). `.git`/`.gitattributes`
  stay at root. Added `backend/.gitignore` (standard Spring Boot Maven) and trimmed the root
  `.gitignore` to general IDE/OS entries. `ng new` writes the frontend's own `.gitignore`.
- **0.2** Rewrote `backend/pom.xml` to a minimal set: `spring-boot-starter-data-jpa`,
  `-data-rest`, `-webmvc`, `-validation`; `mysql-connector-j` (runtime); `lombok` (optional);
  `spring-boot-devtools` + `spring-boot-docker-compose` (runtime/optional);
  `spring-boot-starter-test` (test). **Removed** actuator, batch, all ldap/mail/restclient/
  security (oauth2/webauthn/saml2), webclient, webflux, postgresql, restdocs and the
  asciidoctor plugin + Shibboleth repo. Set `<java.version>21</java.version>`. Kept the
  spring-boot-maven-plugin Lombok exclude and the compiler Lombok annotation-processor path.
  > **Update (M6):** `spring-boot-starter-mail` is now **intentionally back in** — the app sends
  > transactional + weekly marketing email. This supersedes the "removed mail" note above. The
  > other removed starters stay removed.
- **0.3** Trimmed `backend/compose.yaml` to a single MySQL service
  (`3306:3306`, `MYSQL_DATABASE=full-stack-ecommerce`).
- **0.4** `backend/src/main/resources/application.properties`: datasource (matching compose),
  `ddl-auto=update`, `spring.data.rest.base-path=/api`, `show-sql=true`.
- **0.5** Scaffolded Angular: `npx @angular/cli@21 new angular-ecommerce` (standalone, routing,
  CSS, zone.js). Added Bootstrap + Font Awesome + `@ng-bootstrap/ng-bootstrap` and referenced
  the Bootstrap/FA CSS in `angular.json` styles (bumped the initial bundle budget for them).

## Milestone 1 — Product catalog end-to-end ✅

### Backend (`com.bob.ecommerceangularapp`)
- `entity/ProductCategory` — `id`, `categoryName`; `@OneToMany(mappedBy="category")`
  `Set<Product>` with `@JsonIgnore`. Table `product_category`.
- `entity/Product` — `id`, `sku`, `name`, `description`, `unitPrice` (BigDecimal), `imageUrl`,
  `active`, `unitsInStock`, `dateCreated`, `lastUpdated`; `@ManyToOne` `category`. Table
  `product`. (`@CreationTimestamp`/`@UpdateTimestamp` for the dates.)
- `dao/ProductRepository extends JpaRepository<Product,Long>` (Spring Data REST exported) with
  `findByCategoryId(...)` and `findByNameContaining(...)`, both `Page<Product>`.
- `dao/ProductCategoryRepository extends JpaRepository<ProductCategory,Long>`
  (`path = "product-category"`).
- `config/MyDataRestConfig implements RepositoryRestConfigurer` — disables
  POST/PUT/DELETE/PATCH on both domain types, `exposeIdsFor(...)`, and CORS for
  `http://localhost:4200`.
- `bootstrap/DataLoader implements CommandLineRunner` — idempotent seed (4 categories: Books,
  Coffee Mugs, Mouse Pads, Luggage; 20 products with `placehold.co` image URLs).

**Exposed endpoints:** `/api/products`, `/api/products/{id}`,
`/api/products/search/findByCategoryId?id=&page=&size=`,
`/api/products/search/findByNameContaining?name=&page=&size=`, `/api/product-category`.

### Frontend (`frontend/angular-ecommerce`, standalone Angular 21)
- `common/product.ts`, `common/product-category.ts` — models.
- `services/product.service.ts` — `HttpClient`, base URL from `environments/environment.ts`
  (`apiUrl='http://localhost:8080/api'`); paginated list/search, categories, single product;
  response interfaces unwrap `_embedded` + `page`.
- `components/` — `ProductList` (grid + `NgbPagination` + page-size dropdown + empty state;
  reads `category/:id` and `search/:keyword`), `ProductCategoryMenu` (sidebar),
  `Search` (search box → `/search/:keyword`), `ProductDetails` (master-detail at `products/:id`).
- `app.routes.ts`: `products/:id`, `search/:keyword`, `category/:id`, `category`, `products`,
  `'' → /products`, `** → /products`.
- Root `App`: header (logo + `app-search` + cart placeholder), sidebar
  (`app-product-category-menu`), `<router-outlet>`. `app.config.ts` adds `provideHttpClient()`.

---

## Roadmap (later milestones — outline only)
- **M2 — Cart, Checkout, Save Order:** `CartService` (BehaviorSubject totals, sessionStorage),
  cart components, reactive checkout form, form service, Country/State entities + REST,
  Customer/Address/Order/OrderItem + Purchase/PurchaseResponse DTOs, `CheckoutController`
  `POST /api/checkout/purchase`.
- **M3 — Security (Okta OIDC):** re-add `oauth2-resource-server`, Spring Security 7 config,
  protect `/api/orders`, Okta Angular SDK, HTTP interceptor Bearer token.
- **M4 — HTTPS:** self-signed certs, `ng serve --ssl`, Boot PKCS12 on 8443.
- **M5 — Stripe:** `stripe-java`, PaymentInfo DTO, payment-intent endpoint, Stripe Elements.

External accounts (Okta M3, Stripe M5) are needed later — not blockers now.

---

## Verification (Milestone 1)
In-repo (no DB needed):
- Backend builds + tests: `cd backend && ./mvnw clean package` → BUILD SUCCESS. Tests run against
  in-memory **H2** (`backend/src/test/resources/application.properties`), so `contextLoads` boots
  the full Spring context without Docker, alongside a `CheckoutServiceImpl` unit test and a
  `ProductRepository` `@DataJpaTest`.
- Frontend builds + tests: `cd frontend/angular-ecommerce && npm install && npx ng build`, and
  `CI=true npx ng test --watch=false` (CartService + validator specs) → success.

Full end-to-end (local, requires Docker):
1. `cd backend && docker compose up -d` (or let `spring-boot-docker-compose` auto-start it).
2. `cd backend && ./mvnw spring-boot:run` → http://localhost:8080/api/products returns seeded
   products with `id` + pagination metadata; test both `/search/...` endpoints.
3. `cd frontend/angular-ecommerce && npm start` → http://localhost:4200: product grid renders;
   category sidebar filters; keyword search works; pagination + page-size dropdown work;
   clicking a product opens the detail view; empty searches show the "no products found" message.

---

## Implemented beyond M1 (status)

All of the following are **build-verified** (`./mvnw -DskipTests clean package` + `npx ng build`).
Runtime for M3/M5 needs external accounts; the app is designed to build and run without them.

### Milestone 2 — Cart, Checkout, Save Order ✅
- Backend: `Country`/`State`/`Customer`/`Address`/`Order`/`OrderItem` entities; `Purchase`/
  `PurchaseResponse` DTOs; `CountryRepository`, `StateRepository.findByCountryCode`,
  `CustomerRepository` (`exported=false`); `CheckoutService(Impl)` + `CheckoutController`
  `POST /api/checkout/purchase`; seeder extended with 6 countries + states. `Order` maps to the
  `orders` table.
- Frontend: `CartService` (BehaviorSubject totals + `sessionStorage`), cart-status (header),
  cart-details (qty +/-/remove), reactive `checkout` (country/state cascade, copy-shipping-to-billing,
  order summary), add-to-cart wired on product list + details.

### Milestone 3 — Security (Okta OIDC) ✅ (build) / needs Okta to run
- Backend: `spring-boot-starter-security-oauth2-resource-server`; `OrderRepository`
  (`findByCustomerEmailOrderByDateCreatedDesc`, exposed read-only); `SecurityConfig` with **two
  conditional chains** — a JWT-secured chain (active only when
  `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set) that requires auth for
  `GET /api/orders/**`, and an open fallback chain otherwise.
- Frontend: `@okta/okta-angular@8` + `@okta/okta-auth-js@8`; `provideOktaAuth(withOktaConfig(...))`
  in `app.config.ts`; placeholder config in `src/app/auth/okta-config.ts`; `auth.interceptor`
  adds the Bearer token on `/api/orders`; `login-status` header component; `members/orders` route
  guarded by `canActivateAuthGuard`; `login/callback` → `OktaCallbackComponent`.
- **To enable:** create an Okta SPA app, then set the Angular `issuer`/`clientId` in
  `okta-config.ts` and the backend `spring.security.oauth2.resourceserver.jwt.issuer-uri`.

### Milestone 4 — HTTPS 📄 (documented, opt-in)
Kept off by default so local dev stays on plain HTTP. To enable TLS:
1. Generate a self-signed PKCS12 keystore into `backend/src/main/resources/`:
   `keytool -genkeypair -alias ecommerce -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore luv2shop-ssl.p12 -validity 365 -storepass secret -dname "CN=localhost"`
2. Uncomment the `server.ssl.*` block in `application.properties` (serves on `https://localhost:8443`).
3. Serve the frontend over TLS with `ng serve --ssl` and point `environment.apiUrl` at
   `https://localhost:8443/api`. (Do **not** commit the keystore.)

### Milestone 5 — Stripe payments ✅ (build) / needs Stripe to run
- Backend: `stripe-java`; `PaymentInfo` DTO; `CheckoutService.createPaymentIntent` (sets
  `Stripe.apiKey` from `stripe.key.secret`); `CheckoutController` `POST /api/checkout/payment-intent`
  returns the PaymentIntent JSON (with `client_secret`).
- Frontend: `@stripe/stripe-js`; checkout mounts a Stripe **card element**, calls the payment-intent
  endpoint, then `stripe.confirmCardPayment(...)` and only then `POST /api/checkout/purchase`.
- **To enable:** set `STRIPE_SECRET_KEY` (backend) and `environment.stripePublishableKey` (frontend)
  to your Stripe **test** keys. Test card `4242 4242 4242 4242`, any future expiry / CVC / ZIP.
- **Without Stripe:** checkout runs in *demo mode* (skips the card step, saves the order directly), so
  the full flow works with no account. Full beginner walkthrough: **[STRIPE.md](STRIPE.md)**.

### Milestone 6 — Email, account settings & storefront polish ✅ (build) / needs Gmail to send
Adds real email plus the "online-store goodies" (sale section, About, marketing sections).

- **Backend:** re-added `spring-boot-starter-mail`; `@EnableScheduling`.
  - `email/EmailService` (gated on `spring.mail.username` — no-op + log when unset, never throws) +
    `email/EmailTemplates` (branded inline-HTML via Java text blocks, no extra template engine).
  - `NewsletterSubscriber` entity + repo; `Customer` extended with `newsletterSubscribed` +
    `unsubscribeToken`; `Product` extended with nullable `originalPrice` (sale "was" price) and
    `ProductRepository.findByOriginalPriceNotNull` (exposed at `/api/products/search/...`).
  - `NewsletterService` (subscribe/unsubscribe, weekly recipient collection = subscribed customers ∪
    standalone subscribers, deduped) + `WeeklyAdScheduler` (`@Scheduled`, `app.newsletter.cron`,
    default Mon 09:00). `NewsletterController` (`/subscribe`, `/unsubscribe`, guarded `/send-now`) and
    `AccountController` (`GET`/`PUT /api/account`). Checkout `Purchase` gained `subscribeToNewsletter`;
    `CheckoutServiceImpl` sets the preference + sends order-confirmation / welcome email.
  - Seeder now marks ~⅓ of products on sale (sets `originalPrice`).
- **Frontend:** `/sale` (reuses `ProductList` in "sale mode"), `/about`, guarded `/account` settings
  portal; reusable `newsletter-signup` (band + footer); sale pricing (strikethrough + % off) on cards
  & details; home value-prop/trust tiles, sale teaser & newsletter CTA; promo bar, secondary nav,
  expanded footer; checkout "create account & subscribe" opt-in.
- **To enable real sending:** set `GMAIL_USERNAME` + `GMAIL_APP_PASSWORD` (App Password). Full
  walkthrough: **[EMAIL.md](EMAIL.md)**. **Without it:** everything works; email is silently skipped.
