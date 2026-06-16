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
  history (and anything else you protect) requires a valid token.

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

Three defense-in-depth measures ship in the backend. All of them **preserve graceful degradation** —
the app still builds and runs fully open with no Okta/IdP for local development.

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

### Security response headers

`SecurityConfig.applyHardening()` runs on **both** chains, so every response carries:
`Content-Security-Policy` (locks scripts to none, frames/ancestors off, `base-uri 'none'`),
`Strict-Transport-Security` (1 year, `includeSubDomains` — takes effect once you serve over HTTPS),
`X-Frame-Options: DENY`, a strict `Referrer-Policy`, and a `Permissions-Policy` that denies
geolocation/microphone/camera.

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
| Transport (HTTPS/TLS) | 📄 Opt-in | Documented in BUILD_PLAN.md (M4). **Required in production** — terminate TLS at the proxy/LB or enable the Boot keystore. |
| Input validation | ✅ Added | Bean Validation on public DTOs + a global error handler (no stack traces leaked). |
| CORS | ✅ Locked | Only `localhost:4200/4250` allowed — **change to your real origin(s) for production.** |
| CSRF | ✅ N/A for the API | Disabled deliberately: the API is stateless/token-based, not cookie-session based. |
| Admin authorization | ✅ Role-gated | When Okta is configured, `/api/admin/**` requires the **admin role** (a JWT groups claim), not just any logged-in user. See [Role-based admin access](#role-based-admin-access). |
| Response headers | ✅ Hardened | CSP, HSTS, `X-Frame-Options: DENY`, Referrer-Policy, Permissions-Policy on every chain. See [Hardening applied in app code](#hardening-applied-in-app-code). |
| Rate limiting | ✅ Added | Per-IP fixed-window limit + body-size cap on public write endpoints (`reviews`/`coupons`/`newsletter`). For multi-instance deployments, also limit at the gateway/CDN or swap in Redis/Bucket4j for shared state. |
| Account/newsletter APIs | ⚠️ Trust the email | Course-faithful simplicity. Gate behind the JWT before handling real users (see below). |
| Dependency CVEs | ✅ Automated | Dependabot (Maven + npm + actions) raises security/update PRs; CI gates on CVEs (see [Dependency & CVE scanning](#dependency--cve-scanning)). |

### Production hardening checklist
- [ ] Configure Okta (issuer + clientId) and **require MFA**; enable passkeys.
- [x] **Role-gate the admin back-office** — `/api/admin/**` requires the admin role once Okta is on.
- [x] **Security response headers** — CSP / HSTS / frame / referrer / permissions on every chain.
- [x] **Rate limiting + body-size caps** on public POST endpoints (in-app; add gateway/CDN limits too).
- [ ] Protect `/api/account/**` and `/api/newsletter/send-now` with the JWT chain (not just the email).
- [ ] Enforce HTTPS everywhere; set real CORS origins.
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
   never reach users. Today the production tree is clean of high+ advisories, so this gate is green.

> **Deeper / offline scanning (optional):** for an air-gapped or compliance-grade Maven scan, add the
> [OWASP `dependency-check-maven`](https://jeremylong.github.io/DependencyCheck/) plugin (needs an NVD
> API key and a longer CI budget for the database download). The three layers above cover the common
> case without that overhead.
