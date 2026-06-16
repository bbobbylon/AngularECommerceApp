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

## Broader security posture

| Area | Status | Notes |
|---|---|---|
| Payment data (PCI) | ✅ Delegated to Stripe | Card data is entered into Stripe Elements; it never reaches our server. |
| Secrets | ✅ Env vars | Stripe/Gmail/Okta values come from `.env`/env vars, never committed (`.env` is gitignored). |
| Transport (HTTPS/TLS) | 📄 Opt-in | Documented in BUILD_PLAN.md (M4). **Required in production** — terminate TLS at the proxy/LB or enable the Boot keystore. |
| Input validation | ✅ Added | Bean Validation on public DTOs + a global error handler (no stack traces leaked). |
| CORS | ✅ Locked | Only `localhost:4200/4250` allowed — **change to your real origin(s) for production.** |
| CSRF | ✅ N/A for the API | Disabled deliberately: the API is stateless/token-based, not cookie-session based. |
| Rate limiting | ⛔ TODO | Add a limiter (e.g. Bucket4j, or at the gateway/CDN) on public endpoints — especially `/api/newsletter/subscribe`. |
| Account/newsletter APIs | ⚠️ Trust the email | Course-faithful simplicity. Gate behind the JWT before handling real users (see below). |
| Dependency CVEs | ⛔ TODO | Run `mvn versions:display-dependency-updates` / `npm audit`; consider Dependabot. |

### Production hardening checklist
- [ ] Configure Okta (issuer + clientId) and **require MFA**; enable passkeys.
- [ ] Protect `/api/account/**` and `/api/newsletter/send-now` with the JWT chain (not just the email).
- [ ] Enforce HTTPS everywhere; set real CORS origins.
- [ ] Add rate limiting + bot protection on public POST endpoints.
- [ ] Rotate secrets; store them in a managed secrets manager (not a flat `.env`) in cloud.
- [ ] Turn on dependency/CVE scanning and a regular patch cadence (see [MAINTENANCE.md](MAINTENANCE.md)).
