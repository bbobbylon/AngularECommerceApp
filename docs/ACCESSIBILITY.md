# Accessibility (WCAG 2.1 AA)

Luv2Shop targets **WCAG 2.1 Level AA** across the storefront (browse, cart, checkout, favorites,
sale, and the static info pages). This is a frontend-only concern — no backend changes were needed.

## Automated coverage

`frontend/angular-ecommerce/e2e/a11y.spec.ts` runs [axe-core](https://github.com/dequelabs/axe-core)
(via `@axe-core/playwright`) against every reachable storefront page, **in both the light and dark
theme** (24 checks total: 12 pages × 2 themes), using the same hermetic API mocks as the Playwright
smoke suite (`e2e/support/mock-backend.ts` — no backend/MySQL needed).

```bash
cd frontend/angular-ecommerce
npx playwright install chromium   # one-time
npx playwright test e2e/a11y.spec.ts
```

Pages covered: product list, product details, cart, checkout, favorites, sale, about, FAQ, contact,
privacy, terms, shipping & returns. Each assertion fails on **any** violation tagged `wcag2a`,
`wcag2aa`, `wcag21a`, or `wcag21aa` — the tag set is deliberately broad rather than allow-listing
known-fixed rules, so a regression anywhere trips the suite.

This is a **floor, not a ceiling**. axe-core catches programmatically-detectable issues — missing
form labels, insufficient color contrast, ARIA misuse, missing landmarks/`alt` text, invalid
heading order — but it can't judge whether alt text is *meaningful*, whether reading order makes
sense to a screen-reader user, or whether custom keyboard interactions actually work. See
[Manual checks](#manual-checks-not-automated) below.

## What was fixed to get here

Two shared bug classes, found by running the suite and reading the exact violations (not just
"contrast failed" — the `fgColor`/`bgColor`/`contrastRatio` axe reports per node):

### 1. Design-system colors that were fine as decoration, not as text

Several CSS custom properties (`--teal`, `--accent-600`) were tuned to look good as icons, borders,
and button backgrounds, but got reused directly as **text color** somewhere that didn't clear
4.5:1 — e.g. `--teal` (`#10b6a6`) only cleared ~2.5:1 as text on white. The fix was **not** to
darken the base color (that would ruin the icon/badge/button uses it was designed for) — it was to
add a second, text-safe variant scoped to the *reading* contexts:

```css
--teal: #10b6a6;        /* icons, borders, badge backgrounds */
--teal-text: #0d6d63;   /* .text-success-emphasis, .alert-info text — clears >=5.4:1 */
```

The same pattern applies to `--accent-600` → `--accent-text`, `--muted` (the general "secondary
text" color, which also needed a footer-scoped override since the footer is unconditionally dark
regardless of site theme — see `footer { --muted: #8b93ab; }` in `styles.css`), and Bootstrap's
default `code` pink (`#d63384`, only ~4.2:1 on the page background) via a `--code-color` variable.

Each theme (`:root` = light, `[data-theme='dark']`) defines its own value for these text-safe
variants, since a color that reads fine on a light background usually needs to go the *opposite*
direction (lighter, not darker) to read on a dark one — `--accent-text` is `--accent-600` in light
mode but a lighter `#9a86ff` in dark mode.

### 2. Bootstrap defaults that were never bridged to the dark theme

The app uses its own `[data-theme='dark']` attribute (not Bootstrap 5.3's `data-bs-theme`), so any
Bootstrap component color that Bootstrap itself only adapts via `data-bs-theme` — `.form-text`,
`.breadcrumb-item.active` — kept using Bootstrap's light-mode default (a near-black rgba) even when
the rest of the page went dark. `.form-text` was the worst case: ~1.06:1 contrast, i.e. functionally
invisible helper text under coupon/gift-card/rewards inputs at checkout. Fixed by explicitly setting
these to the app's own themed color variables instead of leaving them to Bootstrap:

```css
.form-text { color: var(--muted) !important; }
.breadcrumb-item.active { color: var(--ink-soft) !important; }
```

### 3. A component locked to one background regardless of theme

`product-category-menu`'s header uses Bootstrap's `bg-white` utility (intentionally — it's meant to
stay white even in dark mode, like the footer is meant to stay dark in light mode). Bootstrap's
`bg-white` carries `!important`, so it wins regardless of our own `background: var(--surface)` rule;
the header's `.text-secondary` text, however, was still resolving the *themed* (dark-mode-lightened)
`--muted`, which is unreadable on a hard-coded white background. Fixed the same way as the footer —
scope `--muted` back to its light-mode value for just that element:

```css
.category-menu .card-header { --muted: #64707a; }
```

### 4. Missing label/id association on checkout

The checkout form's shipping and billing sections repeat identical label text ("Street", "City",
"Country", "State", "Zip code") in two different `formGroupName` blocks, with no `for`/`id` pairing
— a screen reader had no way to tell which field belonged to which section (and no way to associate
the visible label with the input at all). Fixed with unique `customer-*` / `shipping-*` / `billing-*`
prefixed `id`s and matching `for` attributes throughout `checkout.html`.

### 5. A transparent-looking search button

`search.html`'s Search button used `btn-outline-primary`, which renders with a transparent
background — sitting on the dark navbar, its purple text measured ~2.56:1. Switched to the solid
`btn-primary` (already contrast-verified elsewhere).

## Manual checks (not automated)

Axe-core can't check these; do them by hand when touching the relevant areas:

- **Keyboard navigation** — tab through checkout, the cart, and any modal/dropdown; every
  interactive element should be reachable and show a visible focus ring (the design system's
  `.form-control:focus` box-shadow and default browser outlines cover most of this).
- **Screen reader smoke test** — VoiceOver (Mac) or NVDA (Windows, free) through the checkout flow
  and a product page; confirm price, stock status, and add-to-cart feedback are actually announced,
  not just visually present (e.g. toast notifications use `role="status"`/`aria-live` — verify new
  toast types keep that).
- **Zoom to 200%** — the layout should reflow without horizontal scrolling or clipped content
  (Bootstrap's grid mostly handles this, but check any fixed-width elements you add).
- **Alt text quality** — automated tools only check that `alt` exists, not that it's useful. Product
  images should describe the product, not say "image" or repeat the product name verbatim if a
  screen reader already announces the name alongside it.
- **Motion sensitivity** — if you add new animation, respect `prefers-reduced-motion`.

## Adding a new page

Add it to the `pages` array in `e2e/a11y.spec.ts` — it's automatically checked in both themes. If it
needs authenticated/populated state (like checkout's cart), see how the `checkout` case is exercised
in that file for the pattern (mock the backend, drive the UI into the needed state, then assert).
