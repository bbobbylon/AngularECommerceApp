# Observability & Ops

Production/enterprise-readiness for running Luv2Shop: health probes, metrics, build info, request
correlation, and a friendly admin view. Built on **Spring Boot Actuator** + **Micrometer**.

## Endpoints (Actuator)

| Endpoint | Purpose |
|---|---|
| `GET /actuator/health` | Aggregate health (`{"status":"UP"}`). 200 when healthy, 503 when down. |
| `GET /actuator/health/liveness` | **Liveness** probe — is the app process alive? (k8s `livenessProbe`) |
| `GET /actuator/health/readiness` | **Readiness** probe — can it serve traffic? (k8s `readinessProbe` / LB) |
| `GET /actuator/info` | Build info (artifact, version, build time) from `build-info.properties`. |
| `GET /actuator/metrics` | Micrometer metrics catalog (JVM, HTTP, datasource, …). |
| `GET /actuator/metrics/{name}` | A single metric, e.g. `http.server.requests`. |
| `GET /actuator/prometheus` | Prometheus scrape format for a metrics pipeline (Grafana, etc.). |

Exposure is allow-listed in `application.properties`
(`management.endpoints.web.exposure.include=health,info,metrics,prometheus`) — nothing else (env,
beans, heapdump, …) is exposed.

```bash
curl http://localhost:8585/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
curl -o /dev/null -w '%{http_code}\n' http://localhost:8585/actuator/health/readiness   # 200
curl http://localhost:8585/actuator/info        # build version + time
curl http://localhost:8585/actuator/prometheus | head
```

### Kubernetes probes (example)
```yaml
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: 8585 }
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8585 }
```

## Health philosophy — don't let optional integrations look "down"

The app **degrades gracefully** around Okta, Stripe, and email. So `/actuator/health` must reflect
only what's *critical to serving traffic* (the database) — not whether SMTP happens to be reachable.

Spring Boot auto-configures a **mail health indicator** (because `spring-boot-starter-mail` is on the
classpath) that opens an SMTP connection on every health check. With no Gmail credentials it fails and
**drags the whole health endpoint to DOWN (503)** — which in production would make Kubernetes/load
balancers evict a perfectly healthy pod. We therefore disable it:

```properties
management.health.mail.enabled=false
```

Mail/Stripe/Okta status is still visible — via the **admin System Health view** below.

## Admin System Health view

`GET /api/admin/system` (gated like the rest of `/api/admin/**`) returns a digestible summary the
back-office dashboard renders as a card:

```jsonc
{
  "status": "UP",                 // UP when the DB is reachable
  "version": "0.0.1-SNAPSHOT",    // from build-info; "dev" if not packaged
  "profile": "default",
  "uptimeSeconds": 348,
  "components": [
    { "name": "Database (MySQL)",  "ready": true,  "detail": "100 products" },
    { "name": "Email (SMTP)",      "ready": false, "detail": "Not configured — emails are no-ops" },
    { "name": "Payments (Stripe)", "ready": false, "detail": "Demo mode — no Stripe key" },
    { "name": "Auth (Okta OIDC)",  "ready": false, "detail": "Open mode — no issuer configured" }
  ]
}
```

`ready: false` on the optional integrations is **expected** in local/dev — it means "not wired up",
not "broken". Visible at **Admin → Dashboard** (the "System health" card).

## Request correlation (tracing)

`RequestIdFilter` tags every request with a correlation id:

- Reuses an inbound `X-Request-Id` header when a proxy/client supplies one; otherwise generates a
  short id.
- Puts it in the SLF4J **MDC**, so it prints on every log line (the `logging.pattern.level` config
  injects `[%X{requestId}]` right after the log level).
- Echoes it back on the response `X-Request-Id` header.

```bash
curl -D - -o /dev/null http://localhost:8585/api/products
# < X-Request-Id: 731a88fc            (generated)
curl -D - -o /dev/null -H "X-Request-Id: trace-abc123" http://localhost:8585/api/products
# < X-Request-Id: trace-abc123        (passed through)
```

Log line example: `INFO  [731a88fc] c.b.e.controller...` — grep one id to follow a request end-to-end.

## Structured (JSON) logging — opt-in

Console logs stay human-readable by default. For a log pipeline (ELK, Loki, Datadog), flip on Spring
Boot's built-in structured logging — no code change:

```properties
logging.structured.format.console=ecs   # or: logstash, gelf
```

This emits one JSON object per line (including the `requestId` from the MDC). Left off by default so
local dev logs stay readable.

## Security note

Actuator endpoints are currently open (like the rest of the API in local dev). For production, restrict
`/actuator/**` (except the probes, which should stay public) to an ops role/network — see
[SECURITY.md](SECURITY.md). This is covered in the **Security hardening** pass.
