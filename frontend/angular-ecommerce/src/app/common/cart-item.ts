import { Product } from './product';
import { ProductVariant } from './product-variant';

export class CartItem {
  id: number;
  name: string;
  imageUrl: string;
  unitPrice: number;
  quantity: number;
  /** Set when the line is a specific variant; drives SKU-level inventory + fulfilment. */
  variantSku?: string;
  variantLabel?: string;

  constructor(product: Product, variant?: ProductVariant) {
    this.id = product.id;
    this.name = variant ? `${product.name} — ${variant.label}` : product.name;
    this.imageUrl = (variant?.imageUrl) || product.imageUrl;
    this.unitPrice = variant ? variant.unitPrice : product.unitPrice;
    this.quantity = 1;
    this.variantSku = variant?.sku;
    this.variantLabel = variant?.label;
  }
}

/** Stable identity for a cart line: same product AND same variant collapse into one line. */
export function cartItemKey(item: Pick<CartItem, 'id' | 'variantSku'>): string {
  return `${item.id}::${item.variantSku ?? ''}`;
}
