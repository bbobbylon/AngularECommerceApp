# Payments with Stripe — a beginner's guide

New to Stripe? This is the whole picture, start to finish.

## TL;DR
- The app **works without Stripe**. Checkout runs in *demo mode* and saves the order directly.
- You only need Stripe to process **card payments** — and in **test mode** that moves **no real money**.
- To turn it on: grab two free test keys, paste one into the frontend and one into the backend, restart.

## What is Stripe (and why use it)?
Stripe is a payment processor. The golden rule of card payments is *your server should never touch
raw card numbers* — that's a security and legal (PCI) minefield. So with Stripe, the card details go
**straight from the shopper's browser to Stripe**, and Stripe hands back a confirmation. Your backend
only ever sees "this payment succeeded," never the card number.

You get two API keys:
- **Publishable key** — `pk_test_...` — safe to ship in the frontend; it just identifies your account
  when the browser talks to Stripe.
- **Secret key** — `sk_test_...` — **backend only, never commit it**; it authorizes charges.

## Test mode vs live mode
- The Stripe dashboard has a **Test mode** toggle. Test keys are prefixed `pk_test_` / `sk_test_`.
- In test mode you pay with **fake test cards** (listed below). Nothing is really charged.
- Live keys (`pk_live_` / `sk_live_`) are for real money — **you don't need those** for this project.

## How this app uses Stripe (the flow)
It uses Stripe's **Payment Intents + Elements** pattern:

1. **Card entry (browser → Stripe).** The checkout page mounts a Stripe **Card Element**. What you type
   goes directly to Stripe — our server never sees it.
2. **Create a Payment Intent (browser → our backend → Stripe).** The frontend posts the amount to
   `POST /api/checkout/payment-intent`. The backend (`CheckoutServiceImpl`, using `stripe-java` + the
   **secret** key) creates a Stripe PaymentIntent and returns its `client_secret`.
3. **Confirm the payment (browser → Stripe).** The frontend calls
   `stripe.confirmCardPayment(client_secret, card)`. Stripe charges the test card and reports success/failure.
4. **Save the order (browser → our backend).** On success, the frontend posts the order to
   `POST /api/checkout/purchase`, which persists it. You land on the confirmation page with a tracking number.

Where it lives in the code:
- Backend: `controller/CheckoutController.java` (`/payment-intent`), `service/CheckoutServiceImpl.java`
  (the Stripe call), `dto/PaymentInfo.java`.
- Frontend: `components/checkout/checkout.ts` (Elements + confirm), `services/checkout.service.ts`.

## Enable Stripe (about 5 minutes)
1. **Create a free account:** https://dashboard.stripe.com/register (no business details needed for test mode).
2. Make sure the dashboard is in **Test mode** (toggle, top-right).
3. Open **Developers → API keys** (https://dashboard.stripe.com/test/apikeys) and copy:
   - **Publishable key** `pk_test_...`
   - **Secret key** `sk_test_...` (click *Reveal*).
4. **Frontend** — paste the publishable key into
   `frontend/angular-ecommerce/src/environments/environment.ts`:
   ```ts
   stripePublishableKey: 'pk_test_yourKeyHere',
   ```
5. **Backend** — provide the secret key as an environment variable (preferred — keeps it out of git).
   In the same Git Bash window before running the app:
   ```bash
   export STRIPE_SECRET_KEY=sk_test_yourKeyHere
   ./run.sh
   ```
   (Or set `stripe.key.secret=sk_test_yourKeyHere` in `backend/src/main/resources/application.properties`,
   but **don't commit a real key**.)
6. Restart. The checkout now shows the card field instead of the "demo checkout" notice.

## Test cards (test mode only)
Use any **future** expiry date, any 3-digit CVC, and any ZIP.

| Card number          | What happens                          |
|----------------------|---------------------------------------|
| 4242 4242 4242 4242  | Payment succeeds                      |
| 4000 0000 0000 9995  | Declined (insufficient funds)         |
| 4000 0025 0000 3155  | Requires authentication (3-D Secure)  |

Full list: https://docs.stripe.com/testing

## Demo mode (no Stripe configured)
If the publishable key is still the shipped placeholder, the checkout page shows a **"Demo checkout"**
notice and the **Place order** button skips the payment step, saving the order directly. This lets you
walk the full catalog → cart → checkout → confirmation flow with **no Stripe account at all**. The
backend's `/payment-intent` endpoint simply stays dormant until a secret key is set.

## Security notes
- **Never commit the secret key.** Prefer the `STRIPE_SECRET_KEY` env var; the empty default in
  `application.properties` keeps git clean.
- The **publishable** key is meant to be public — shipping it in the frontend is fine.
- Everything here is **test mode** — no real charges, ever.

## Handy links
- Dashboard: https://dashboard.stripe.com
- Test API keys: https://dashboard.stripe.com/test/apikeys
- Testing & test cards: https://docs.stripe.com/testing
- Payment Intents overview: https://docs.stripe.com/payments/payment-intents
