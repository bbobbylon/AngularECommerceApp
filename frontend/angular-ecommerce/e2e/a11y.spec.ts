import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

import { mockBackend } from './support/mock-backend';

/**
 * Automated WCAG 2.1 AA accessibility audit (roadmap #13) across the storefront's main reachable
 * pages, using the same hermetic API mocks as the storefront smoke test. This is a floor, not a
 * ceiling — axe-core catches programmatically-detectable issues (missing labels, contrast, ARIA
 * misuse, landmark structure...) but not everything WCAG cares about (e.g. meaningful reading order,
 * whether alt text is actually *descriptive*); see docs/ACCESSIBILITY.md for the manual checks.
 */

const pages = [
  { path: '/products', name: 'product list' },
  { path: '/products/101', name: 'product details' },
  { path: '/cart-details', name: 'cart' },
  { path: '/checkout', name: 'checkout' },
  { path: '/favorites', name: 'favorites' },
  { path: '/sale', name: 'sale' },
  { path: '/about', name: 'about' },
  { path: '/faq', name: 'faq' },
  { path: '/contact', name: 'contact' },
  { path: '/privacy', name: 'privacy' },
  { path: '/terms', name: 'terms' },
  { path: '/shipping-returns', name: 'shipping & returns' },
];

const themes = ['light', 'dark'] as const;

for (const theme of themes) {
  for (const { path, name } of pages) {
    test(`a11y: ${name} (${path}) [${theme}] has no automatically-detectable WCAG 2.1 AA violations`, async ({ page }) => {
      await page.addInitScript(t => localStorage.setItem('theme', t), theme);
      await mockBackend(page);
      await page.goto(path);
      await expect(page.locator('main')).toBeVisible();
      // Let the CMS-driven announcement banner (roadmap #17) settle before scanning — it fetches
      // after first paint and its arrival shifts the sticky header down.
      await page.waitForLoadState('networkidle');

      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze();

      const summary = results.violations.map(v => ({
        id: v.id,
        impact: v.impact,
        help: v.help,
        nodes: v.nodes.map(n => n.target.join(' ')),
      }));
      expect(summary, JSON.stringify(summary, null, 2)).toEqual([]);
    });
  }
}
