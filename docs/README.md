# Luv2Shop — Documentation

Start here. The docs are intentionally concise and visual.

| Doc | What's inside |
|---|---|
| 🏛️ **[ARCHITECTURE.md](ARCHITECTURE.md)** | System context, backend & frontend architecture, data model (ER), and request/checkout flow diagrams — all brand-themed. |
| 🧭 **[FLOWS.md](FLOWS.md)** | **Granular, rebuild-from-this flows** — checkout (every API call, payload + DB write) and the full Okta auth / JWT authorization sequence. |
| 🔌 **[API.md](API.md)** | REST endpoint reference with `curl` examples and the pagination envelope. |
| 🛠️ **[DEVELOPMENT.md](DEVELOPMENT.md)** | Build, run, test, ports, DB, conventions, and "definition of done". |
| 🚀 **[DEPLOYMENT.md](DEPLOYMENT.md)** | CI/CD + manual deploy templates for GCP / AWS / Azure, with topology diagrams. |
| 💳 **[STRIPE.md](STRIPE.md)** | Beginner-friendly Stripe walkthrough (test mode, keys, test cards). |
| 🗺️ **[BUILD_PLAN.md](BUILD_PLAN.md)** | Milestone plan (M0–M5), locked decisions, and how to enable Okta / Stripe / HTTPS. |

New to the project? Read **ARCHITECTURE** for the mental model, then **DEVELOPMENT** to run it.
