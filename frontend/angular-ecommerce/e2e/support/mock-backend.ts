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

  // Shipping options for the checkout selector.
  await page.route(/\/api\/checkout\/shipping-methods/, route =>
    route.fulfill(json([
      { id: 1, code: 'STANDARD', name: 'Standard shipping', baseRate: 5.99, freeOverThreshold: 50, estimatedDays: '3–5 business days' },
      { id: 2, code: 'EXPRESS', name: 'Express shipping', baseRate: 14.99, freeOverThreshold: null, estimatedDays: '1–2 business days' },
    ])),
  );

  // Totals quote — echoes the posted subtotal with free shipping + no tax (good enough for the smoke flow).
  await page.route(/\/api\/checkout\/quote/, route => {
    const body = (route.request().postDataJSON() ?? {}) as { subtotal?: number; shippingMethodCode?: string };
    const subtotal = Number(body.subtotal ?? 0);
    route.fulfill(json({
      subtotal, discount: 0, shippingAmount: 0, taxAmount: 0, taxRatePercent: 0,
      total: subtotal, shippingMethodCode: body.shippingMethodCode ?? 'STANDARD',
    }));
  });

  // Abandoned-cart snapshot (captured on email blur) — accept and ignore.
  await page.route(/\/api\/abandoned-cart/, route => route.fulfill({ status: 202, body: '' }));

  // Saved addresses (loaded on email blur in checkout) — none for the guest flow.
  await page.route(/\/api\/account\/addresses(\?.*)?$/, route => route.fulfill(json([])));

  // Place order — demo mode (no Stripe key) posts straight here.
  await page.route(/\/api\/checkout\/purchase/, route =>
    route.fulfill(json({ orderTrackingNumber: TRACKING_NUMBER })),
  );
}
