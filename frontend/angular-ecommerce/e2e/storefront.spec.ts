import { expect, test } from '@playwright/test';

import { TRACKING_NUMBER, mockBackend } from './support/mock-backend';

test.beforeEach(async ({ page }) => {
  await mockBackend(page);
});

test('storefront: browse → add to cart → checkout → order confirmation (demo mode)', async ({ page }) => {
  await page.goto('/products');

  // Catalog renders from the mocked /catalog/search.
  await expect(page.getByRole('heading', { name: 'Products', exact: true })).toBeVisible();
  const firstCard = page.locator('.product-card').first();
  await expect(firstCard).toBeVisible();

  // Add the first product to the cart; the header cart badge should reflect it.
  await firstCard.getByRole('button', { name: /add to cart/i }).click();
  await expect(page.locator('.cart-badge')).toContainText('1');

  // Open the cart and proceed to checkout.
  await page.locator('a[href="/cart-details"]').first().click();
  await expect(page.getByRole('heading', { name: 'Shopping Cart' })).toBeVisible();
  await page.getByRole('link', { name: /proceed to checkout/i }).click();
  await expect(page.getByRole('heading', { name: 'Checkout' })).toBeVisible();

  // Stripe is not configured → the card step is replaced by the demo notice.
  await expect(page.getByText(/Demo checkout/i)).toBeVisible();

  // Customer details.
  const customer = page.locator('[formgroupname="customer"]');
  await customer.locator('[formcontrolname="firstName"]').fill('Ada');
  await customer.locator('[formcontrolname="lastName"]').fill('Lovelace');
  await customer.locator('[formcontrolname="email"]').fill('ada@example.com');

  // Shipping address — fill fully (country change loads the mocked states), then copy to billing.
  const shipping = page.locator('[formgroupname="shippingAddress"]');
  await shipping.locator('[formcontrolname="street"]').fill('123 Analytical Ave');
  await shipping.locator('[formcontrolname="city"]').fill('Testville');
  await shipping.locator('[formcontrolname="zipCode"]').fill('90210');
  await shipping.locator('[formcontrolname="country"]').selectOption({ label: 'United States' });
  await shipping.locator('[formcontrolname="state"]').selectOption({ label: 'California' });

  await page.locator('#copyShipping').check();

  // Place the order (demo mode skips Stripe and posts /checkout/purchase).
  await page.getByRole('button', { name: 'Place order' }).click();

  // Lands on the confirmation page with the mocked tracking number.
  await expect(page).toHaveURL(new RegExp(`/order-confirmation/${TRACKING_NUMBER}`));
  await expect(page.getByRole('heading', { name: /thank you for your order/i })).toBeVisible();
  await expect(page.getByText(TRACKING_NUMBER)).toBeVisible();
});

test('storefront: app shell and static pages render', async ({ page }) => {
  await page.goto('/about');
  // Brand + primary nav come from the always-present app shell.
  await expect(page.getByRole('link', { name: /Luv2.*Shop/i }).first()).toBeVisible();
  await expect(page.getByRole('link', { name: 'Shop all' })).toBeVisible();
  await expect(page.locator('main h1, main h2').first()).toBeVisible();

  await page.goto('/faq');
  await expect(page.locator('main h1, main h2').first()).toBeVisible();
});
