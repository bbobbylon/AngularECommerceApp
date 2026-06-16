import { Product, discountPercent, galleryImages, isLowStock, isOnSale } from './product';

function product(overrides: Partial<Product>): Product {
  return {
    id: 1,
    sku: 'SKU',
    name: 'Test',
    description: 'desc',
    unitPrice: 20,
    imageUrl: 'main.png',
    active: true,
    unitsInStock: 10,
    dateCreated: new Date(),
    lastUpdated: new Date(),
    ...overrides,
  } as Product;
}

describe('product sale helpers', () => {
  it('isOnSale is true only when originalPrice is higher than the unit price', () => {
    expect(isOnSale(product({ unitPrice: 20, originalPrice: 30 }))).toBe(true);
    expect(isOnSale(product({ unitPrice: 20, originalPrice: 20 }))).toBe(false);
    expect(isOnSale(product({ unitPrice: 20, originalPrice: null }))).toBe(false);
    expect(isOnSale(product({ unitPrice: 20 }))).toBe(false);
  });

  it('discountPercent rounds the markdown, or returns 0 when not on sale', () => {
    expect(discountPercent(product({ unitPrice: 21, originalPrice: 30 }))).toBe(30); // 1 - 21/30 = 0.30
    expect(discountPercent(product({ unitPrice: 20 }))).toBe(0);
  });
});

describe('product stock helpers', () => {
  it('isLowStock flags 1..5 units but not 0 or healthy stock', () => {
    expect(isLowStock(product({ unitsInStock: 1 }))).toBe(true);
    expect(isLowStock(product({ unitsInStock: 5 }))).toBe(true);
    expect(isLowStock(product({ unitsInStock: 0 }))).toBe(false);
    expect(isLowStock(product({ unitsInStock: 6 }))).toBe(false);
  });
});

describe('galleryImages', () => {
  it('puts the main image first and de-duplicates', () => {
    const p = product({ imageUrl: 'main.png', additionalImages: ['main.png', 'a.png', 'b.png'] });
    expect(galleryImages(p)).toEqual(['main.png', 'a.png', 'b.png']);
  });

  it('drops blanks and works with no gallery', () => {
    expect(galleryImages(product({ imageUrl: 'main.png', additionalImages: ['', 'a.png'] })))
      .toEqual(['main.png', 'a.png']);
    expect(galleryImages(product({ imageUrl: 'main.png' }))).toEqual(['main.png']);
  });
});
