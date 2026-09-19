# 🔐 Security — auth model, MFA/OTP & passkeys

## How authentication works today

Luv2Shop **delegates identity to an OIDC provider (Okta)** and runs the backend as a stateless
**OAuth2 resource server** that validates JWTs. There is **no local password store** in this app.

```
Browser ──(login)──► Okta (hosted sign-in)
   │  id/access token (JWT)
   ▼
Angular ──Authorization: Bearer <JWT>──► Spring Boot (validates JWT) ──► /api/orders/**
```

- **Frontend:** `@okta/okta-angular`; the `authInterceptor` attaches the bearer token to `/api/orders`.
- **Backend:** `spring-boot-starter-security-oauth2-resource-server`. `SecurityConfig` has two chains —
  a **secured** chain (active only when `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set)
  that requires auth for `GET /api/orders/**`, and an **open** chain otherwise so local dev needs no IdP.
- Gating means: **no Okta configured → app runs fully open for development**; configure Okta → order
  history (and anything else you protect) requires a valid token. Running the **`prod` profile without
  an issuer** logs a loud startup warning (the API would be open) — see `SecurityConfig`.

## Sessions, tokens & cookies

**There is no server-side session and no application cookie.** This is the deliberate design of a
token-based SPA + stateless resource-server, and it's worth being precise about because it changes which
classic risks apply.

- **Stateless backend.** `SecurityConfig` sets `SessionCreationPolicy.STATELESS` on **both** filter
  chains, so Spring **never creates an `HttpSession` and never issues a `JSESSIONID`**. There is no
  server session store to secure, expire, replicate, or pin — every request is authenticated solely from
  its `Authorization: Bearer <JWT>` header. (This also means horizontal scaling needs no sticky sessions.)
- **Where the "session" actually lives.** The only session state is the **OIDC token set** (id/access
  tokens) held by the browser, managed by `@okta/okta-auth-js` using the **Authorization Code flow with
  PKCE** (`pkce: true` in `okta-config.ts`). By default okta-auth-js keeps the token set in
  `localStorage`. The Angular `authInterceptor` attaches the access token **only** to the secured
  prefixes (`/api/orders`, `/api/account`, `/api/admin`) — never to public catalog/cart/checkout calls,
  and never cross-site.
- **Threat model for browser-stored tokens.** Because the token is readable by JavaScript, the relevant
  risk is **XSS** (not CSRF). Mitigations that ship: a **strict Content-Security-Policy on the SPA**
  (nginx — `script-src 'self' https://js.stripe.com`, no `unsafe-eval`), Angular's built-in contextual
  auto-escaping/sanitization (the app never binds untrusted HTML via `innerHTML`/bypass APIs), and
  short-lived access tokens rotated by Okta. **Upgrade path for the highest assurance:** a
  *backend-for-frontend (BFF)* that holds tokens server-side and exposes them to the SPA only via an
  `HttpOnly; Secure; SameSite=Strict` cookie, proxying API calls — this removes tokens from JS entirely.
  It's intentionally not built here (it adds a stateful tier and only matters once Okta is live).
- **Why CSRF is disabled and safe.** CSRF exploits *ambient* credentials the browser sends
  automatically (i.e. cookies). This API has **no auth cookie** and authenticates with a Bearer header
  that the browser does **not** attach on cross-site requests, so CSRF doesn't apply — hence
  `csrf().disable()`. ⚠️ If you ever switch to cookie-based auth (e.g. the BFF above), you **must**
  re-enable CSRF protection and set `SameSite` on the cookie.
- **Logout** is therefore client-side: okta-auth-js clears the stored token set and ends the Okta
  session; there's no server session to invalidate.

## Do we have MFA / OTP / passkeys?

**Not in app code — and that's the correct design here.** Because identity lives in Okta, the
*strongest* place to enforce MFA, one-time passcodes (OTP), and **passkeys (WebAuthn/FIDO2)** is the
identity provider. Okta supports all of these natively; you enable them with **configuration, no code**.
The account portal already surfaces a **Security & sign-in** card that links users to manage these
factors once Okta is configured.

> Why not build app-native WebAuthn? It would mean introducing a parallel password/credential store
> and a second auth system that isn't wired to the JWT model the API already uses — more attack
> surface, less security. Delegating to Okta is the production-correct choice for this architecture.

### Enable MFA + OTP in Okta (≈ 5 min)
1. **Admin console → Security → Authenticators.** Add/enable the factors you want: *Okta Verify*
   (push/TOTP), *Google Authenticator* (TOTP/OTP), *Phone* (SMS/voice OTP), *Email*.
2. **Security → Authentication policies** (or *Sign-on policies*). Add a rule for your app that
   requires **2 factors** (password **+** any of the above) — optionally only for higher-risk sign-ins.
3. **Enrollment policy:** set whether MFA enrollment is *optional* or *required* at first sign-in.

### Enable passkeys (WebAuthn / FIDO2) in Okta
1. **Security → Authenticators → Add authenticator → "FIDO2 (WebAuthn)".**
2. Set it to allow **platform** authenticators (Face ID / Touch ID / Windows Hello) and/or
   **cross-platform** security keys (YubiKey).
3. Add it to your authentication policy so users can sign in with a passkey (passwordless) or use it
   as a second factor.
4. End users then add/remove passkeys from their Okta **end-user settings** — the "Manage sign-in &
   passkeys" button in **Account settings** deep-links there.

After enabling, set the backend issuer + the Angular `okta-config.ts` (`issuer`/`clientId`) — see
[BUILD_PLAN.md](BUILD_PLAN.md) → Milestone 3.

## Hardening applied in app code

Several defense-in-depth measures ship in the backend and the SPA. All of them **preserve graceful
degradation** — the app still builds and runs fully open with no Okta/IdP for local development.

### Endpoint authorization & stateless sessions

The secured chain (active once an issuer is set) authorizes by path, most-specific first:

| Path | Rule |
|---|---|
| `/actuator/health/**` | public (load-balancer / k8s probes) |
| `/actuator/**` (metrics, info, prometheus) | `authenticated()` |
| `GET /api/orders/**`, `/api/account/**` | `authenticated()` |
| `POST /api/newsletter/send-now` | `authenticated()` |
| `/api/admin/**` | `hasAuthority(adminRole)` |
| everything else (catalog, cart, checkout, reviews, public newsletter, `/api/order-history/recent`) | public |

Both chains are `STATELESS` (no session/cookie — see [Sessions, tokens & cookies](#sessions-tokens--cookies)).
The matching frontend `authInterceptor` sends the Bearer token only to `/api/orders`, `/api/account`,
and `/api/admin`.

### Role-based admin access

`SecurityConfig`'s secured chain protects `/api/admin/**` with `hasAuthority(adminRole)` instead of a
bare `authenticated()`, so a logged-in **non-admin** customer cannot reach the back-office. Authorities
come from a configurable JWT array claim (Okta's default is `groups`) via a `JwtAuthenticationConverter`:

| Property | Default | Meaning |
|---|---|---|
| `app.security.admin-claim` | `groups` | JWT claim that lists the user's group/role memberships. |
| `app.security.admin-role` | `Admin` | Membership value required to reach `/api/admin/**`. |

Set these only if your IdP names its claim/group differently. While no issuer is configured the **open
chain** applies and `/api/admin/**` is reachable for development, exactly as before.

### Spring Data REST search resources must never take a caller-supplied `tenantId` (roadmap #21 gap)

A tenant-scoping audit (2026-09-18) found that the multi-tenancy migration (roadmap #21, Milestones
A–D) consistently stamped `tenant_id` on entities and rewrote **Java** call sites to
`findByIdAndTenantId(id, TenantContext.currentTenantId())`, but never revisited the three repositories
still exported via Spring Data REST (`Product`, `ProductCategory`, `Order` — see `Order.java`'s own
javadoc: "one of only 5 Spring Data REST-exported repos"). Spring Data REST auto-generates an HTTP
search resource for every public derived-query method unless it's marked
`@RestResource(exported = false)`, and it binds **every** method parameter — `tenantId` included —
straight from the request's query string. The result was two distinct, real, live gaps:

1. **Caller-controlled `tenantId`.** Methods like `findByIdAndTenantId`/`countByTenantId`/
   `existsByIdAndTenantId` were reachable as e.g. `GET /api/products/search/findByIdAndTenantId?id=1&tenantId=2`
   — anyone could pass *any* tenant id directly, completely bypassing `TenantContext`/
   `TenantResolutionFilter`/`TenantResourceGuardFilter`. These methods are called only from Java
   service code (which always supplies `TenantContext.currentTenantId()`), so marking them
   `@RestResource(exported = false)` closes the hole with zero behavior change.
2. **No tenant predicate at all.** `OrderRepository.findByCustomerEmailOrderByDateCreatedDesc` (backing
   the customer-facing "My Orders" page) and the plain `Order` collection resource
   (`GET /api/orders?sort=...`, used as an anonymous demo-mode fallback) took no tenant predicate
   whatsoever — reachable by anyone (unauthenticated, when Okta isn't configured; by any authenticated
   user otherwise), returning **any** tenant's customer's full order history for a known/guessed email,
   or the 50 most recent orders across **every** tenant. Three now-dead legacy `ProductRepository`
   search methods (`findByCategoryId`, `findByNameContaining`, `findByOriginalPriceNotNull` — superseded
   by the faceted, tenant-scoped `/api/catalog/search`) had the same shape.

Fixed by: unexporting every `*TenantId`-taking method across `ProductRepository`/
`ProductCategoryRepository`/`OrderRepository`; unexporting the three superseded `ProductRepository`
search methods (after confirming their one remaining live caller, "You might also like" on
product-details, could move to `searchCatalog()` instead — see `ProductService.getRelatedProducts`);
replacing `Order`'s raw email search and collection listing with two small, tenant-scoped, custom
endpoints (`GET /api/account/orders?email=` under the existing `authenticated()`-when-Okta-configured
`/api/account/**` gate, and the new public `GET /api/order-history/recent` for the anonymous fallback);
and disabling `Order`'s SDR collection `GET` outright in `MyDataRestConfig` (its item resource,
`GET /api/orders/{id}`, stays on — still tenant-guarded by `TenantResourceGuardFilter`). See
`OrderRepositoryTest` (JPA-level tenant isolation) and `OrderTenantExposureIntegrationTest`
(full-context, real-HTTP proof the old paths are gone and the new ones work) for the regression
coverage. **Lesson for future work here: any repository that stays Spring Data REST-exported needs
every derived-query method individually reviewed for exposure — a method being tenant-scoped in name
is not the same as it being tenant-scoped when a client controls every one of its parameters.**

### Platform-superadmin tier (roadmap #21, Milestone B)

`/api/platform/**` (tenant create/list/edit/deactivate) is gated on a separate, higher authority —
`app.security.superadmin-role`, default `SuperAdmin` — read through the same groups-claim mechanism as
`adminRole`/`orderManagerRole`/`viewerRole` above, not a new Okta claim. **A real superadmin needs two
Okta group memberships**: `SuperAdmin` to manage tenants, plus one of the admin-tier roles above if they
also intend to use the platform tenant switcher to browse a given tenant's `/api/admin/**` back office
(the switcher just sets `X-Tenant-Id` on those calls — it doesn't grant `/api/admin/**` access by
itself). While no issuer is configured, the open chain's `GET /api/admin/me` defaults to
`[Admin, SuperAdmin]` so local dev can reach `/platform` with zero Okta setup, matching this app's
existing graceful-degradation default.

### API-key authentication for headless callers (roadmap #23, Milestone A)

Alongside the Okta JWT, a tenant can authenticate machine-to-machine calls to its own
`/api/admin/**` with an **API key** presented as `X-Api-Key` (header configurable via
`app.api-key.header`). Full guide: [WEBHOOKS.md](WEBHOOKS.md).

- **Stored hashed.** The raw key is generated from `SecureRandom` (32 bytes hex, `lsk_`-prefixed so a
  leak is greppable) and persisted only as a **SHA-256 hash** — it is returned exactly once at issue
  time and is unrecoverable afterward. Every later read shows a 16-char masked prefix.
- **One header, two jobs.** `TenantResolutionFilter` resolves the key's **tenant** at
  `HIGHEST_PRECEDENCE + 5` (ahead of `X-Tenant-Id`/subdomain/default), and
  `ApiKeyAuthenticationFilter` — registered *inside* the Spring Security chain, before
  `BearerTokenAuthenticationFilter` — derives its **authorities**, so the role tiers above apply to
  key-authenticated callers unchanged.
- **No enumeration signal.** An unknown, revoked, or expired key gets the same generic **404** as an
  unknown tenant slug.
- **Revocation is immediate.** Revoking soft-deletes the key *and* evicts the whole `apiKeyLookup`
  cache, so it stops working on the next request rather than when a TTL lapses.
- **Outbound webhooks are signed**, not authenticated in reverse: each callback carries a hex
  `X-Webhook-Signature` (HMAC-SHA256 over the raw body, keyed with the subscription's own
  once-shown secret). Receivers must verify it against the raw bytes before parsing.

### Security response headers

`SecurityConfig.applyHardening()` runs on **both** chains, so every response carries:
`Content-Security-Policy` (locks scripts to none, frames/ancestors off, `base-uri 'none'`),
`Strict-Transport-Security` (1 year, `includeSubDomains` — takes effect once you serve over HTTPS),
`X-Frame-Options: DENY`, a strict `Referrer-Policy`, and a `Permissions-Policy` that denies
geolocation/microphone/camera.

The **SPA is also hardened at the edge**: the frontend's `nginx.conf` sends a Content-Security-Policy
tuned for the app — own bundles + Stripe.js; `style-src 'unsafe-inline'` + Google Fonts
(`fonts.googleapis.com`) for Angular's injected styles and the brand fonts; `font-src` for
`fonts.gstatic.com`; `img-src https:` for product images; `connect-src 'self' https:` for the
API/Okta/Stripe XHR (plus `http://localhost:8585` so the local `docker compose` backend works); and
`frame-src` for Stripe Elements — plus `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
`Referrer-Policy`, `Permissions-Policy`, and HSTS. The policy was verified at runtime against the live
container (0 violations). Tighten `connect-src`/`frame-src` to your exact backend + Okta origins (and
drop the localhost entry) for an even stricter production policy.

> **Build note:** the build disables Angular's `inlineCritical` CSS optimization, which would otherwise
> emit an inline `onload` handler on the stylesheet link that `script-src 'self'` blocks — leaving the
> page unstyled. Disabling it yields a plain render-blocking `<link>` and keeps the strict CSP.

### Rate limiting + body-size caps

`RateLimitFilter` guards the public, unauthenticated **write** endpoints (`/api/reviews`,
`/api/coupons`, `/api/newsletter`) with a per-IP fixed-window limit (**30 req/min**, backed by the
Caffeine cache already on the classpath) and a **64 KB body cap**, returning `429` (with `Retry-After`)
and `413` respectively. It runs just after `RequestIdFilter` so rejections still carry a correlation id;
read paths and the core checkout flow are deliberately not limited. The limiter is per-instance — for a
horizontally-scaled deployment, also enforce limits at the gateway/CDN or move shared state to
Redis/Bucket4j.

## Broader security posture

| Area | Status | Notes |
|---|---|---|
| Payment data (PCI) | ✅ Delegated to Stripe | Card data is entered into Stripe Elements; it never reaches our server. |
| Secrets | ✅ Env vars | Stripe/Gmail/Okta values come from `.env`/env vars, never committed (`.env` is gitignored). |
| Transport (HTTPS/TLS) | 📄 Opt-in | Documented in BUILD_PLAN.md (M4). **Required in production** — terminate TLS at the proxy/LB or enable the Boot keystore. The cloud deploys terminate TLS at the platform; `server.forward-headers-strategy=framework` makes the app honor `X-Forwarded-Proto`. |
| Input validation | ✅ Added | Bean Validation on public DTOs + a global error handler (no stack traces leaked). |
| Sessions / cookies | ✅ Stateless | `SessionCreationPolicy.STATELESS` — no `HttpSession`, no `JSESSIONID`, no app cookie. The "session" is the Okta token set in the browser. See [Sessions, tokens & cookies](#sessions-tokens--cookies). |
| Token storage (SPA) | ✅ PKCE + CSP | okta-auth-js (Authorization Code + PKCE); browser-stored token defended by the SPA's strict CSP + Angular auto-escaping. BFF is the documented upgrade. |
| CORS | ✅ Config-driven | `app.cors.allowed-origins` (one `CorsConfigurationSource` bean governs SDR + all controllers). Defaults to localhost; the deploy sets `APP_CORS_ALLOWED_ORIGINS` to the live frontend origin automatically. |
| CSRF | ✅ N/A for the API | Disabled deliberately: Bearer-token auth, no auth cookie → no CSRF vector. Re-enable if you adopt cookie-based auth. |
| Frontend headers | ✅ Hardened | nginx sends CSP, HSTS, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, Referrer-Policy, Permissions-Policy on the SPA. |
| Actuator exposure | ✅ Scoped | Health/liveness/readiness public (for probes); metrics/info/prometheus require auth on the secured chain. |
| Admin authorization | ✅ Role-gated | When Okta is configured, `/api/admin/**` requires the **admin role** (a JWT groups claim), not just any logged-in user. See [Role-based admin access](#role-based-admin-access). |
| Response headers | ✅ Hardened | CSP, HSTS, `X-Frame-Options: DENY`, Referrer-Policy, Permissions-Policy on every chain. See [Hardening applied in app code](#hardening-applied-in-app-code). |
| Rate limiting | ✅ Added | Per-IP fixed-window limit + body-size cap on public write endpoints (`reviews`/`coupons`/`newsletter`). For multi-instance deployments, also limit at the gateway/CDN or swap in Redis/Bucket4j for shared state. |
| Account/newsletter APIs | ✅ JWT-gated | `/api/account/**` and `POST /api/newsletter/send-now` require authentication on the secured chain (the SPA tokens them via the interceptor). Public newsletter subscribe/unsubscribe stay open. |
| Dependency CVEs | ✅ Automated | Dependabot (Maven + npm + actions) raises security/update PRs; CI gates on CVEs (see [Dependency & CVE scanning](#dependency--cve-scanning)). |

### Production hardening checklist
- [ ] Configure Okta (issuer + clientId) and **require MFA**; enable passkeys. *(Without it the `prod`
  profile logs a loud "API is OPEN" warning at startup.)*
- [x] **Stateless sessions** — `SessionCreationPolicy.STATELESS`; no `HttpSession`/`JSESSIONID`/app cookie.
- [x] **Role-gate the admin back-office** — `/api/admin/**` requires the admin role once Okta is on.
- [x] **JWT-gate account + newsletter-send** — `/api/account/**` and `POST /api/newsletter/send-now`.
- [x] **Scope actuator** — only health probes are public; metrics/info/prometheus require auth.
- [x] **Security response headers** — backend (every chain) **and** the SPA (nginx): CSP / HSTS / frame /
  referrer / permissions / nosniff.
- [x] **Rate limiting + body-size caps** on public POST endpoints (in-app; add gateway/CDN limits too).
- [x] **Config-driven CORS** — `app.cors.allowed-origins`; the deploy sets it to the live frontend origin.
- [ ] Enforce HTTPS everywhere (terminate TLS at the proxy/LB — the cloud deploys do).
- [ ] Rotate secrets; store them in a managed secrets manager (not a flat `.env`) in cloud.
- [x] **Dependency / CVE scanning + patch cadence** — Dependabot + a CI gate (see below).

## Dependency & CVE scanning

Three layers keep dependencies patched and known-vulnerable versions out:

1. **Dependabot** (`.github/dependabot.yml`) — weekly update PRs for **Maven**, **npm**, and
   **GitHub Actions**, plus immediate **security PRs** when an advisory hits a pinned version.
   Minor/patch bumps are grouped to cut review noise.
2. **Dependency review** (CI, on pull requests) — `actions/dependency-review-action` fails a PR that
   introduces a dependency with a **high+** severity advisory, across both Maven and npm, using the
   GitHub Advisory DB (no NVD API key required).
3. **`npm audit` gate** (CI) — `npm audit --omit=dev --audit-level=high` on every push/PR. It scans
   **shipped (production) dependencies**; dev-tooling advisories are left to Dependabot since they
   never reach users. Today the production tree is clean of high+ advisories, so this gate is green — the
   Angular 21→22 upgrade (2026-09-13) cleared the last two high advisories the 21.x line could not patch
   (`GHSA-jj27-h5hq-8x99`, `GHSA-hh8m-fm6v-7cvg`).
4. **SBOM** — the backend build generates a CycloneDX Software Bill of Materials
   (`cyclonedx-maven-plugin` → `target/bom.json`, also embedded in the jar under `META-INF/sbom`).
   Feed it to a scanner (Grype/Trivy) or Dependency-Track for continuous supply-chain monitoring.

> **Deeper / offline scanning (optional):** for an air-gapped or compliance-grade Maven scan, add the
> [OWASP `dependency-check-maven`](https://jeremylong.github.io/DependencyCheck/) plugin (needs an NVD
> API key and a longer CI budget for the database download). The three layers above cover the common
> case without that overhead.
