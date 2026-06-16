# 🛠️ Maintenance, Costs & Upgrades

How to keep Luv2Shop healthy over time: what it costs to run, what to do on a schedule, how to
upgrade safely, and how to handle the database.

---

## 💰 Forecasted running costs

**Local / development: $0.** Everything runs on your machine (Docker MySQL, Maven, Node). The only
"costs" are external accounts you opt into, all of which have free tiers:

| Service | Free tier | When you'd pay |
|---|---|---|
| **Gmail SMTP** (email) | ~500 emails/day | Heavy volume → move to SendGrid/Resend/SES (see below). |
| **Stripe** | No monthly fee | Per-transaction: ~2.9% + $0.30 (test mode is free). |
| **Okta** (auth) | Free dev org | Workforce/customer plans scale with MAU. |

**If you deploy to the cloud** (rough monthly estimate for a small store — varies by provider/region):

| Component | Typical small-scale | Notes |
|---|---|---|
| App compute (backend+frontend) | $0–25 | Free/hobby tiers exist (Render, Railway, Fly.io, Azure App Service B1). |
| Managed MySQL | $0–30 | Free tiers (PlanetScale, Aiven trials) → ~$15–30 for an entry instance (RDS/Cloud SQL). |
| Object/static hosting (Angular) | $0–5 | Static hosting/CDN is often free (Netlify, Cloudflare Pages, S3+CloudFront). |
| Transactional email | $0–20 | SendGrid/Resend free tiers (~3k/mo) → paid as volume grows. |
| Domain + TLS | ~$1–2/mo | Domain ~$12/yr; TLS free via Let's Encrypt/provider. |
| **Ballpark total** | **~$0–80/mo** | Starts near $0 on free tiers; ~$40–80 for modest always-on infra. |

> Email at scale: Gmail SMTP is great to start but rate-limited and not built for bulk marketing.
> When the weekly blast grows, switch `MAIL_HOST`/credentials to a transactional provider — the code
> already reads SMTP settings from env vars, so it's a config change, not a rewrite.

---

## 🔁 Routine maintenance

| Cadence | Task |
|---|---|
| **Weekly** | Skim app logs for errors/`WARN`; confirm the newsletter blast ran (logs: "Weekly newsletter blast sent"). |
| **Monthly** | `npm audit` (frontend) + `mvn versions:display-dependency-updates` (backend); apply patch updates. Verify a fresh `./run.sh` boots cleanly. Check Stripe/Okta dashboards. |
| **Quarterly** | Apply minor framework updates (see Upgrades). Rotate secrets (Gmail App Password, Stripe keys, Okta client). Review/prune Docker images & volumes. Test a DB backup restore. |
| **Yearly / on release** | Plan major version bumps (Spring Boot, Angular, Java LTS, MySQL). Review the security checklist in [SECURITY.md](SECURITY.md). |

---

## ⬆️ Upgrades (when newer versions ship)

**Golden rule:** bump **one** thing at a time, then run both builds + tests + a manual smoke test
before moving on. Use a branch.

- **Backend deps:** `cd backend && ./mvnw versions:display-dependency-updates`. For the framework,
  bump the `spring-boot-starter-parent` `<version>` (it manages most transitive versions). Read the
  Spring Boot release notes / migration guide for the target version.
- **Java:** the pom pins `<java.version>21</java.version>` (an LTS). Move to the next LTS (25, etc.)
  deliberately; update the Dockerfile base image (`eclipse-temurin`) to match.
- **Frontend:** `cd frontend/angular-ecommerce && npx ng update` lists safe upgrades; run
  `npx ng update @angular/core @angular/cli` (one major at a time). Re-run `npm audit fix` after.
- **ng-bootstrap / Bootstrap / Font Awesome:** check peer-dependency ranges against the Angular version.
- **MySQL:** stay on a supported series (currently 8.4 LTS). Test against a copy before upgrading prod.

After **any** upgrade:
```bash
cd backend && ./mvnw clean package            # backend build + tests
cd frontend/angular-ecommerce && npx ng build && CI=true npx ng test --watch=false
```

---

## 🗄️ Database — schema changes & the `ddl-auto` gotcha

This project uses Hibernate `spring.jpa.hibernate.ddl-auto=update`, which **auto-applies entity
changes** to the schema. It's convenient for a course/demo but has sharp edges:

- ⚠️ **Adding a `NOT NULL` column to a table that already has rows fails** on MySQL strict mode
  (`Incorrect datetime value: '0000-00-00...'`). `update` logs the error and *continues*, leaving the
  column missing — which then breaks queries at runtime. (This exact issue once crashed the backend on
  boot via a `date_created` column — see git history.) **Make new columns nullable**, or give them a
  default, when the table may already contain data.
- `update` **never drops** columns/tables and won't always reconcile type changes.
- **Recommendation for production:** adopt a migration tool — **Flyway** (simplest) or Liquibase — and
  set `ddl-auto=validate`. Versioned SQL migrations are reviewable, repeatable, and safe on populated
  tables. This is the single biggest step toward a production-grade database lifecycle.

### Reset the database (dev)
The catalog is reseeded automatically by `DataLoader` when empty, and one-time **backfills** keep an
existing DB consistent (newsletter defaults + sale prices). If the schema ever drifts badly, start fresh:
```bash
cd backend
docker compose down -v     # ⚠️ deletes the MySQL volume (all orders/customers); catalog reseeds next boot
docker compose up -d
```

### Backups
- **Local:** `docker exec backend-mysql-1 mysqldump -uecommerceapp -pecommerceapp full-stack-ecommerce > backup.sql`
- **Production:** use the managed DB's automated backups + point-in-time recovery; test a restore quarterly.

---

## 📈 Monitoring & health (recommended for production)

Not enabled by default (kept minimal for the course), but add before going live:
- **Spring Boot Actuator** — re-add `spring-boot-starter-actuator`, expose `/actuator/health` (and
  `/info`), and wire it to your platform's health checks / uptime monitoring.
- **Centralized logs** — ship `backend.log` (and frontend errors) to your platform's logging.
- **Uptime + alerts** — a simple external ping on `/api/products` catches "storefront is down" fast
  (the symptom we want to never miss).

---

## ✅ Definition of "still healthy"
1. `./run.sh` boots both servers with no errors in `backend.log`.
2. `http://localhost:8585/api/products` returns 100 seeded products; `/sale` is populated.
3. Both build + test suites pass.
4. A test order completes; (if email is configured) confirmation + welcome emails arrive.
