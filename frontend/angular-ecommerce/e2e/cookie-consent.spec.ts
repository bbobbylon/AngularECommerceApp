import AxeBuilder from '@axe-core/playwright';
import { Page, expect, test } from '@playwright/test';

import { mockBackend } from './support/mock-backend';

/**
 * Cookie/storage consent banner + preferences panel (roadmap #24). The default mock-backend stubs
 * `GET /api/privacy/consent` as "already consented" so the other 30+ specs never see the banner —
 * these tests override it to simulate a first-time visitor with no consent record on file.
 */

async function mockNoConsentYet(page: Page): Promise<void> {
  await mockBackend(page);
  await page.route(/\/api\/privacy\/consent/, route => {
    if (route.request().method() === 'GET') {
      route.fulfill({ status: 204, body: '' });
      return;
    }
    route.fallback();
  });
}

test.describe('cookie consent', () => {
  test('shows the banner on first visit and Accept all dismisses it', async ({ page }) => {
    await mockNoConsentYet(page);
    await page.goto('/products');
    await page.waitForLoadState('networkidle');

    await expect(page.getByRole('dialog', { name: 'Cookie consent' })).toBeVisible();
    await page.getByRole('button', { name: 'Accept all' }).click();
    await expect(page.getByRole('dialog', { name: 'Cookie consent' })).toBeHidden();
  });

  test('Reject all dismisses the banner', async ({ page }) => {
    await mockNoConsentYet(page);
    await page.goto('/products');
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: 'Reject all' }).click();
    await expect(page.getByRole('dialog', { name: 'Cookie consent' })).toBeHidden();
  });

  test('Customize opens the preferences panel and Save preferences submits the chosen categories', async ({ page }) => {
    await mockNoConsentYet(page);
    await page.goto('/products');
    await page.waitForLoadState('networkidle');

    await page.getByRole('button', { name: 'Customize' }).click();
    const panel = page.getByRole('dialog', { name: 'Cookie preferences' });
    await expect(panel).toBeVisible();

    await panel.getByLabel('Functional').check();
    const [request] = await Promise.all([
      page.waitForRequest(req => req.url().includes('/api/privacy/consent') && req.method() === 'POST'),
      panel.getByRole('button', { name: 'Save preferences' }).click(),
    ]);
    const body = request.postDataJSON() as { functional: boolean; analytics: boolean };
    expect(body.functional).toBe(true);
    expect(body.analytics).toBe(false);
    await expect(panel).toBeHidden();
  });

  test('footer "Cookie preferences" link reopens the panel even after a decision was already made', async ({ page }) => {
    await mockBackend(page); // default: already consented, banner never shows
    await page.goto('/products');
    await page.waitForLoadState('networkidle');

    await expect(page.getByRole('dialog', { name: 'Cookie consent' })).toBeHidden();
    await page.getByRole('button', { name: 'Cookie preferences' }).click();
    await expect(page.getByRole('dialog', { name: 'Cookie preferences' })).toBeVisible();
  });

  test('has no automatically-detectable WCAG 2.1 AA violations while the banner is open', async ({ page }) => {
    await mockNoConsentYet(page);
    await page.goto('/products');
    await page.waitForLoadState('networkidle');
    await expect(page.getByRole('dialog', { name: 'Cookie consent' })).toBeVisible();

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();
    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
  });
});
