import { Page } from '@playwright/test';

/**
 * Stubs the Luv2Shop backend at the network layer so the storefront E2E runs without MySQL or the
 * Spring Boot API. Responses match the real contracts: the faceted `/catalog/search` envelope, the
 * Spring Data REST HAL `_embedded` shapes for categories/countries/states, and the checkout response.
 */

export const TRACKING_NUMBER = 'TEST-TRACK-1001';

export const products = [
  {
    id: 101,
    sku: 'BOOK-1001',
    name: 'The Pragmatic Programmer',
    description: 'A classic guide to software craftsmanship.',
    unitPrice: 39.99,
    imageUrl: 'https://placehold.co/300x300?text=Book',
    active: true,
    unitsInStock: 25,
    averageRating: 4.8,
    reviewCount: 120,
    dateCreated: '2026-01-01T00:00:00Z',
    lastUpdated: '2026-01-01T00:00:00Z',
  },
  {
    id: 102,
    sku: 'MUG-2001',
    name: 'Luv2Shop Coffee Mug',
    description: 'Start your morning with a little delight.',
    unitPrice: 12.5,
    imageUrl: 'https://placehold.co/300x300?text=Mug',
    active: true,
    unitsInStock: 3,
    averageRating: 4.2,
    reviewCount: 8,
    dateCreated: '2026-01-01T00:00:00Z',
    lastUpdated: '2026-01-01T00:00:00Z',
  },
];

const json = (body: unknown) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify(body),
});

/** Register all the route handlers the storefront flow touches. Call at the start of each test. */
export async function mockBackend(page: Page): Promise<void> {
  // Category sidebar menu (HAL).
  await page.route(/\/api\/product-category(\?.*)?$/, route =>
    route.fulfill(json({
      _embedded: {
        productCategory: [
          { id: 1, categoryName: 'Books' },
          { id: 2, categoryName: 'Coffee Mugs' },
        ],
      },
    })),
  );

  // Faceted catalog search — backs the product list / home grid.
  await page.route(/\/api\/catalog\/search/, route =>
    route.fulfill(json({
      content: products,
      totalElements: products.length,
      totalPages: 1,
      number: 0,
      size: 12,
    })),
  );

  // Checkout reference data (HAL).
  await page.route(/\/api\/countries(\?.*)?$/, route =>
    route.fulfill(json({ _embedded: { countries: [{ id: 1, code: 'US', name: 'United States' }] } })),
  );
  await page.route(/\/api\/states\/search\/findByCountryCode/, route =>
    route.fulfill(json({
      _embedded: { states: [{ id: 5, name: 'California' }, { id: 32, name: 'New York' }] },
    })),
  );

  // Place order — demo mode (no Stripe key) posts straight here.
  await page.route(/\/api\/checkout\/purchase/, route =>
    route.fulfill(json({ orderTrackingNumber: TRACKING_NUMBER })),
  );
}
