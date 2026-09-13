# 🔌 Headless API & Webhooks — Roadmap #23

Luv2Shop's back office is usable **without the Angular UI**. A tenant issues itself an **API key**,
presents it as `X-Api-Key`, and drives `/api/admin/**` programmatically — and registers **webhook
subscriptions** so its own systems get pushed a signed HTTP callback when something happens, instead
of polling.

The two halves are deliberately symmetric: the API key is how *you call us*, the webhook is how
*we call you*.

| Milestone | What it added |
|---|---|
| **A** | `ApiKey` (`V21`) — issue/list/revoke, SHA-256 hashed, resolves both tenant **and** authorities |
| **B** | `WebhookSubscription` (`V22`) — register a URL + event types, HMAC secret shown once |
| **C** | `WebhookEvent` (`V23`) — a transactional **outbox**, written in the publisher's own transaction |
| **D** | `WebhookDeliveryAttempt` (`V23`) — delivery with retry/backoff + a per-attempt ledger |

---

## Part 1 — Headless API (API keys)

### Issue a key

Admin → **API keys** (`/admin/api-keys`), or programmatically:

```bash
curl -X POST http://localhost:8585/api/admin/api-keys \
  -H 'Content-Type: application/json' \
  -d '{"name":"Warehouse sync","authorities":["Admin"],"expiresAt":null}'
```

```jsonc
{
  "id": 1,
  "name": "Warehouse sync",
  "keyPrefix": "lsk_9f2a1c4e8b3d",               // first 16 chars — the only part ever shown again
  "authorities": "Admin",
  "rawKey": "lsk_9f2a1c4e8b3d…<64 hex chars>",   // shown EXACTLY ONCE — store it now
  "expiresAt": null,
  "dateCreated": "2026-09-12T10:04:11.000+00:00"
}
```

The raw key is generated from `SecureRandom` (32 bytes as hex, `lsk_`-tagged so a leaked key is
recognizable in a log scan) and **only the SHA-256 hash is persisted**. Every later read returns the
masked prefix only. Lose it and you issue a new one — there is no recovery path, by design.

### Use it

```bash
curl http://localhost:8585/api/admin/stats -H 'X-Api-Key: lsk_9f2a1c4e8b...'
```

One header does two jobs:

1. **Tenant identity** — `TenantResolutionFilter` resolves the key's tenant at
   `HIGHEST_PRECEDENCE + 5`, *ahead of* `X-Tenant-Id`/subdomain/default. A headless caller never
   needs to send a tenant header; its key already says who it is.
2. **Authorization** — `ApiKeyAuthenticationFilter` turns the key's stored authority list into a
   Spring Security `Authentication`, so the same RBAC tiers from roadmap #19 apply unchanged
   (`Admin` / `OrderManager` / `Viewer`).

An unknown, revoked, or expired key gets the **same generic 404** as an unknown tenant slug — no
user-enumeration signal about whether the key or the tenant was the problem.

> **Why two filters, not one?** `TenantResolutionFilter` runs before `FilterChainProxy` even starts,
> so anything it set on the `SecurityContextHolder` would be discarded the moment the security chain
> installs the request's context. `ApiKeyAuthenticationFilter` is therefore registered *inside*
> `SecurityConfig`'s `HttpSecurity` (`addFilterBefore(..., BearerTokenAuthenticationFilter.class)`),
> exactly where `BasicAuthenticationFilter` and X.509 client-cert auth set authentication.

### Revoke

`DELETE /api/admin/api-keys/{id}` soft-revokes (`active=false` + `revokedAt`) and **evicts the whole
`apiKeyLookup` cache** — a revoked key stops working on the next request, not when a TTL lapses.

| Config | Default | Meaning |
|---|---|---|
| `app.api-key.header` | `X-Api-Key` | Header the key is read from |

---

## Part 2 — Webhooks

### Register a subscription

Admin → **Webhooks** (`/admin/webhooks`), or `POST /api/admin/webhooks`:

```bash
curl -X POST http://localhost:8585/api/admin/webhooks \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://your-app.example.com/hooks/luv2shop","eventTypes":["order.created","shipment.shipped"],"active":true}'
```

The response carries a **signing secret** — like the API key, shown **exactly once**; every later
read (including the admin list) returns the masked form.

### Event types

| Event | Fired when |
|---|---|
| `order.created` | An order is placed (`CheckoutServiceImpl`) |
| `shipment.shipped` | A shipment is created with carrier/tracking, or advanced to SHIPPED |
| `shipment.delivered` | A shipment is advanced to DELIVERED |
| `return.approved` | An admin approves an RMA (refund issued if Stripe is configured) |
| `return.denied` | An admin denies an RMA |

These are exactly the mutation points `AuditLogService.record(...)` already instruments.

### What you receive

```http
POST /hooks/luv2shop HTTP/1.1
Content-Type: application/json
X-Webhook-Event: order.created
X-Webhook-Signature: 9c1185a5c5e9fc54612808977ee8f548b2258d31…

{"orderId":42,"orderTrackingNumber":"a8a78b09-…","totalPrice":129.99,"status":null,"customerEmail":"ada@example.com"}
```

Payload fields per event type:

| Event | Payload |
|---|---|
| `order.created` | `orderId`, `orderTrackingNumber`, `totalPrice`, `status`, `customerEmail` |
| `shipment.shipped` / `shipment.delivered` | `shipmentId`, `orderId`, `orderTrackingNumber`, `carrier`, `trackingNumber`, `status` |
| `return.approved` | `returnId`, `orderTrackingNumber`, `status`, `refundAmount` |
| `return.denied` | `returnId`, `orderTrackingNumber`, `reason` |

Treat these as **notifications, not a data feed** — they carry enough to identify the thing that
changed, then you call back through the headless API for the full record. Fields may be `null`
(a freshly placed order has no `status` until an admin sets one).

### Verify the signature

`X-Webhook-Signature` is **HMAC-SHA256 over the raw request body**, keyed with your subscription's
secret, hex-encoded — the same scheme Stripe documents for its own webhooks. Verify against the raw
bytes *before* parsing JSON (re-serializing changes the bytes and breaks the comparison):

```js
const expected = crypto.createHmac('sha256', SECRET).update(rawBody).digest('hex');
const ok = crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(received));
```

Reject anything that doesn't match — the signature is the only thing proving the callback came from
Luv2Shop and wasn't tampered with in transit.

### Delivery, retry & backoff

Publishing is a **transactional outbox**, not a live HTTP call: `WebhookEventPublisher.publish()` is
`@Transactional` and *joins the caller's transaction*, writing one `webhook_event` row per matching
active subscription. A rolled-back order therefore never emits a webhook — the event row rolls back
with it.

`WebhookDeliveryScheduler` then sweeps due events (default **every 30 seconds**) and
`WebhookDeliveryService` attempts each one independently:

```
attempt 1 fails → retry in 1m → 5m → 15m → 60m → 360m → ABANDONED
```

| Event status | Meaning |
|---|---|
| `PENDING` | Written, not yet attempted |
| `DELIVERED` | Your endpoint returned 2xx |
| `FAILED` | Last attempt failed; `nextAttemptAt` is set |
| `ABANDONED` | All 5 attempts exhausted, or the subscription was deactivated |

**Every** branch — 2xx, non-2xx, network error, deactivated subscription — writes a
`webhook_delivery_attempt` row and returns normally. One subscriber's dead endpoint never blocks
another's delivery (the same per-item isolation `AbandonedCartService.remindStale()` and
`BillingService.chargeDueAccounts()` use).

Your endpoint should **return 2xx quickly** and do its real work asynchronously; a slow endpoint
burns the 5-second read timeout and counts as a failure.

### Inspect deliveries

The admin Webhooks page has a **Deliveries** toggle per subscription — an expand-in-place row showing
recent attempts (event, attempt #, outcome, HTTP status, error, timestamp), paginated. Same data via
`GET /api/admin/webhooks/{id}/deliveries?page=0&size=10`.

This is the first place to look when a subscriber says "we never got it": the ledger distinguishes
*we never tried* (no rows) from *we tried and your endpoint 500'd* (rows with the status code).

| Config | Default | Meaning |
|---|---|---|
| `app.webhook.delivery-cron` | `*/30 * * * * *` | Delivery sweep cadence |

---

## Endpoint summary

| Method | Path | Auth |
|---|---|---|
| GET/POST | `/api/admin/api-keys` | 🔒 Admin |
| DELETE | `/api/admin/api-keys/{id}` | 🔒 Admin |
| GET/POST | `/api/admin/webhooks` | 🔒 Admin |
| DELETE | `/api/admin/webhooks/{id}` | 🔒 Admin |
| GET | `/api/admin/webhooks/{id}/deliveries` | 🔒 Admin |

All are under `/api/admin/**`, so they inherit the existing role gate (roadmap #19) and tenant
scoping (roadmap #21) with no extra wiring.

---

## Security notes

- **Both secrets are shown once.** API keys are stored as SHA-256 hashes; webhook secrets are
  returned masked on every read after creation.
- **Always verify the signature**, and prefer an HTTPS callback URL — the payload carries order and
  customer data.
- **Keys are tenant-scoped.** A key issued by tenant A resolves to tenant A and can never read or
  mutate tenant B's data, enforced by the same `TenantContext` predicates as every other surface.
- Related: [SECURITY.md](SECURITY.md) (roles, sessions & tokens) · [API.md](API.md) (full REST
  surface) · [ARCHITECTURE.md](ARCHITECTURE.md).
