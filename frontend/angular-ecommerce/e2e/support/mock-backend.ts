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

  // Faceted catalog search — backs the product list / home grid and the header typeahead.
  // Honors `keyword` (case-insensitive contains-on-name, matching the real backend) and `size`
  // so search-specific tests can assert on filtered results, not just "something came back".
  await page.route(/\/api\/catalog\/search/, route => {
    const url = new URL(route.request().url());
    const keyword = (url.searchParams.get('keyword') ?? '').trim().toLowerCase();
    const size = Number(url.searchParams.get('size') ?? 12);
    const matches = keyword
      ? products.filter(p => p.name.toLowerCase().includes(keyword))
      : products;
    const content = matches.slice(0, size);
    route.fulfill(json({
      content,
      totalElements: matches.length,
      totalPages: Math.max(1, Math.ceil(matches.length / size)),
      number: 0,
      size,
    }));
  });

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
      promotionName: null, promotionDiscount: 0,
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

  // Product-details page: the product itself, its category (for "related products"), variants,
  // and reviews (list + summary) — all empty/minimal but shaped like the real contracts.
  await page.route(/\/api\/products\/\d+$/, route => {
    const id = Number(route.request().url().match(/\/products\/(\d+)$/)?.[1]);
    const product = products.find(p => p.id === id) ?? products[0];
    route.fulfill(json(product));
  });
  await page.route(/\/api\/products\/\d+\/category/, route =>
    route.fulfill(json({ id: 1, categoryName: 'Books' })),
  );
  await page.route(/\/api\/catalog\/products\/\d+\/variants/, route => route.fulfill(json([])));
  await page.route(/\/api\/products\/search\/findByCategoryId/, route =>
    route.fulfill(json({ _embedded: { products: [] }, page: { totalElements: 0, totalPages: 1, number: 0, size: 12 } })),
  );
  await page.route(/\/api\/reviews\/summary/, route =>
    route.fulfill(json({ average: 0, count: 0, distribution: [0, 0, 0, 0, 0] })),
  );
  await page.route(/\/api\/reviews(\?.*)?$/, route =>
    route.fulfill(json({ content: [], totalElements: 0, totalPages: 1, number: 0, size: 5 })),
  );

  // Wishlist sync (favorites page) — empty for a guest session.
  await page.route(/\/api\/wishlist/, route => route.fulfill(json([])));

  // CMS content (roadmap #17) — banner shown on every page, FAQ entries on /faq.
  await page.route(/\/api\/content\/banner/, route =>
    route.fulfill(json({ id: 1, message: "This week's sale — up to 49% off.", linkUrl: '/sale', linkText: 'Shop now', active: true })),
  );
  await page.route(/\/api\/content\/faq/, route =>
    route.fulfill(json([
      { id: 1, question: 'How long does shipping take?', answer: 'Standard shipping is 3–5 business days.', sortOrder: 1, active: true },
    ])),
  );

  // Admin analytics dashboard (roadmap #18) — no storefront spec navigates here today, but stubbed
  // for consistency with the other admin surfaces in case a future E2E spec covers it.
  await page.route(/\/api\/admin\/analytics\/summary/, route =>
    route.fulfill(json({ averageOrderValue: 42.5, revenueThisMonth: 500, revenueLastMonth: 400, growthPercent: 25 })),
  );
  await page.route(/\/api\/admin\/analytics\/revenue/, route =>
    route.fulfill(json([{ date: '2026-09-04', revenue: 100, orderCount: 2 }])),
  );
  await page.route(/\/api\/admin\/analytics\/top-products/, route =>
    route.fulfill(json([{ productId: 101, name: 'The Pragmatic Programmer', unitsSold: 3, revenue: 119.97 }])),
  );
  await page.route(/\/api\/admin\/analytics\/order-status/, route =>
    route.fulfill(json([{ status: 'COMPLETED', count: 5 }])),
  );

  // Admin RBAC + audit log (roadmap #19) — no storefront spec navigates here today, but stubbed
  // for consistency with the other admin surfaces in case a future E2E spec covers it.
  await page.route(/\/api\/admin\/me/, route => route.fulfill(json({ roles: ['Admin'] })));
  await page.route(/\/api\/admin\/audit-log/, route =>
    route.fulfill(json({
      content: [
        { id: 1, actor: 'admin@example.com', action: 'PRODUCT_UPDATE', entityType: 'Product', entityId: '101', details: null, createdAt: '2026-09-04T12:00:00Z' },
      ],
      totalElements: 1, totalPages: 1, number: 0, size: 20,
    })),
  );
}
