/**
 * A purchasable variation of a product (e.g. a colour/size). The `unitPrice` and `imageUrl` returned
 * by the storefront endpoint are already resolved (the variant's own value, or the product's), so the
 * UI can render a variant directly. `id`/`sku` identify it; `inStock` mirrors `unitsInStock > 0`.
 */
export interface ProductVariant {
  id: number;
  sku: string;
  color?: string | null;
  size?: string | null;
  label: string;
  unitPrice: number;
  unitsInStock: number;
  inStock: boolean;
  imageUrl?: string | null;
}
