import { expect, test } from '@playwright/test';

import { mockBackend } from './support/mock-backend';

/**
 * Header search-box autocomplete (roadmap #14). Exercises the debounced suggestion dropdown
 * against the keyword-aware /catalog/search mock (see support/mock-backend.ts).
 */

test.describe('search typeahead', () => {
  test.beforeEach(async ({ page }) => {
    await mockBackend(page);
    await page.goto('/products');
    // The CMS-driven announcement banner (roadmap #17) fetches after first paint and can shift the
    // sticky header down; let it settle before tests click at fixed page coordinates.
    await page.waitForLoadState('networkidle');
  });

  test('shows matching suggestions while typing and navigates to the selected product', async ({ page }) => {
    const input = page.getByPlaceholder('Search products...');
    await input.fill('mug');

    const listbox = page.locator('#search-listbox');
    await expect(listbox).toBeVisible();
    await expect(listbox.getByRole('option', { name: /Luv2Shop Coffee Mug/ })).toBeVisible();
    await expect(listbox.getByRole('option')).toHaveCount(2); // 1 product + "see all" row

    await listbox.getByRole('option', { name: /Luv2Shop Coffee Mug/ }).click();
    await expect(page).toHaveURL(/\/products\/102$/);
  });

  test('keyboard: ArrowDown highlights a suggestion and Enter navigates to it', async ({ page }) => {
    const input = page.getByPlaceholder('Search products...');
    await input.fill('mug');
    await expect(page.getByRole('option', { name: /Luv2Shop Coffee Mug/ })).toBeVisible();

    await input.press('ArrowDown');
    await expect(input).toHaveAttribute('aria-activedescendant', 'search-option-102');

    await input.press('Enter');
    await expect(page).toHaveURL(/\/products\/102$/);
  });

  test('keyboard: Enter with no suggestion highlighted goes to the full results page', async ({ page }) => {
    const input = page.getByPlaceholder('Search products...');
    await input.fill('mug');
    await expect(page.locator('#search-listbox')).toBeVisible();

    await input.press('Enter');
    await expect(page).toHaveURL(/\/search\/mug$/);
  });

  test('shows a "no products found" state for an unmatched query', async ({ page }) => {
    const input = page.getByPlaceholder('Search products...');
    await input.fill('zzznope');

    await expect(page.locator('#search-listbox').getByText('No products found for "zzznope"')).toBeVisible();
  });

  test('Escape closes the dropdown', async ({ page }) => {
    const input = page.getByPlaceholder('Search products...');
    await input.fill('mug');
    await expect(page.locator('#search-listbox')).toBeVisible();

    await input.press('Escape');
    await expect(page.locator('#search-listbox')).toBeHidden();
  });

  test('clicking outside the search box closes the dropdown', async ({ page }) => {
    const input = page.getByPlaceholder('Search products...');
    await input.fill('mug');
    await expect(page.locator('#search-listbox')).toBeVisible();

    await page.locator('main').click({ position: { x: 10, y: 10 } });
    await expect(page.locator('#search-listbox')).toBeHidden();
  });
});
