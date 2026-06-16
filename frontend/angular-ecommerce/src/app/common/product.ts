export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string;
  unitPrice: number;
  /** Pre-sale ("was") price. Present only for items on sale; otherwise null/undefined. */
  originalPrice?: number | null;
  imageUrl: string;
  active: boolean;
  unitsInStock: number;
  /** Denormalized rating aggregates (null/0 when the product has no reviews yet). */
  averageRating?: number | null;
  reviewCount?: number | null;
  dateCreated: Date;
  lastUpdated: Date;
}

/** True when the product has a higher pre-sale price than its current unit price. */
export function isOnSale(product: Product): boolean {
  return product.originalPrice != null && product.originalPrice > product.unitPrice;
}

/** Whole-number discount percentage (e.g. 30 for 30% off); 0 when not on sale. */
export function discountPercent(product: Product): number {
  if (!isOnSale(product)) {
    return 0;
  }
  return Math.round((1 - product.unitPrice / product.originalPrice!) * 100);
}
