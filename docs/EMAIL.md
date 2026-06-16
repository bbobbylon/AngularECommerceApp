# 📧 Email (Gmail SMTP) — Milestone 6

Luv2Shop sends real email: a **welcome** when someone joins the list, an **order confirmation**
after checkout, a **preferences-saved** note, and a **weekly marketing blast** of handpicked deals.

Like Okta and Stripe, email **degrades gracefully**: it's off until you add credentials, so the app
builds and runs with zero secrets. When SMTP isn't configured, `EmailService` logs and no-ops —
checkout, account saves, and the scheduled blast all keep working, they just don't send anything.

---

## What gets sent

| Trigger | Email | Recipient |
|---|---|---|
| Newsletter signup box / checkout opt-in | Welcome 🎉 | the new subscriber |
| `POST /api/checkout/purchase` | Order confirmation ✅ | the customer |
| Save in the account settings portal | Preferences updated | the customer |
| Weekly schedule (`@Scheduled`, Mon 09:00) | "Your weekly edit" ✨ | **all subscribed customers ∪ all newsletter subscribers** (deduped) |

Every marketing email includes a one-click **unsubscribe** link (`GET /api/newsletter/unsubscribe?token=…`).
Order confirmations are transactional and always send.

---

## Setup (≈ 3 minutes)

You need a Gmail account with **2-Step Verification** on, then a 16-character **App Password**
(a normal Gmail password won't work for SMTP).

1. **Turn on 2-Step Verification** → <https://myaccount.google.com/security>
2. **Create an App Password** → <https://myaccount.google.com/apppasswords>
   - App = *Mail*, Device = *Other* → name it "Luv2Shop".
   - Google shows a 16-char password like `abcd efgh ijkl mnop`.
3. **Add it to `.env`** (copy from `.env.example` if you haven't). Enter the password **without spaces**:

   ```bash
   GMAIL_USERNAME=you@gmail.com
   GMAIL_APP_PASSWORD=abcdefghijklmnop
   ```

4. **Run the app** — `./run.sh` sources `.env` and exports it, so the backend picks it up
   automatically. (Running the backend directly? Set the two env vars in your shell first.)

That's it. Place an order or use the signup box and check your inbox.

### Not using Gmail?
Override the host/port via env vars (`MAIL_HOST`, `MAIL_PORT`) — any SMTP server works. For a safe
sandbox that never reaches real inboxes, point them at [Mailtrap](https://mailtrap.io).

---

## Configuration reference

All keys live in `backend/src/main/resources/application.properties` and read from env vars:

| Property | Env var | Default | Purpose |
|---|---|---|---|
| `spring.mail.username` | `GMAIL_USERNAME` | *(empty)* | **Enables email when set.** Auth user + default From. |
| `spring.mail.password` | `GMAIL_APP_PASSWORD` | *(empty)* | Gmail App Password (16 chars, no spaces). |
| `spring.mail.host` | `MAIL_HOST` | `smtp.gmail.com` | SMTP host. |
| `spring.mail.port` | `MAIL_PORT` | `587` | SMTP port (STARTTLS). |
| `app.mail.from` | `MAIL_FROM` | = username | "From" address. |
| `app.mail.from-name` | `MAIL_FROM_NAME` | `Luv2Shop` | Sender display name. |
| `app.frontend-url` | `APP_FRONTEND_URL` | `http://localhost:4250` | Builds links inside emails (shop, account). |
| `app.api-url` | `APP_API_URL` | `http://localhost:8585` | Builds the unsubscribe link. |
| `app.newsletter.cron` | `NEWSLETTER_CRON` | `0 0 9 * * MON` | Weekly blast schedule (Spring cron). |
| `app.newsletter.admin-token` | `NEWSLETTER_ADMIN_TOKEN` | *(empty)* | Guards the manual send-now endpoint. |

---

## Testing the weekly blast without waiting for Monday

The blast is driven by `WeeklyAdScheduler` → `NewsletterService.sendWeeklyBlast()`. Two ways to fire it now:

- **Change the cron** temporarily, e.g. `NEWSLETTER_CRON=0 * * * * *` (top of every minute), then watch `backend.log`.
- **Manual trigger endpoint** — set `NEWSLETTER_ADMIN_TOKEN=some-secret`, then:

  ```bash
  curl -X POST "http://localhost:8585/api/newsletter/send-now?token=some-secret"
  # -> {"message":"Weekly blast triggered.","recipients":N}
  ```

  Without the token set, the endpoint returns `403` (disabled), so it can't be abused.

---

## How it fits together

```
Signup box ─┐                         ┌─ EmailService.sendWelcome
Checkout  ──┤→ NewsletterService ─────┤
            │  (subscribe/unsub,      ├─ EmailService.sendWeeklyAd  (per recipient, w/ unsubscribe token)
@Scheduled ─┘   collect recipients)   └─ EmailTemplates (branded inline-HTML)

CheckoutService ── sendOrderConfirmation / sendWelcome
AccountController ─ sendSettingsUpdated
```

- `EmailService` — gated send; never throws (a mail failure can't break checkout).
- `EmailTemplates` — branded, inline-styled HTML built with text blocks (no extra template engine).
- `NewsletterService` — subscribe/unsubscribe + recipient collection (customers ∪ subscribers, deduped).
- `WeeklyAdScheduler` — the cron entry point.

> **Privacy note:** This is a course/demo project. The account + unsubscribe APIs trust the supplied
> email (no server-side identity check). For production, gate them behind real authentication (Okta is
> already wired for order history) and add rate limiting on the public subscribe endpoint.
