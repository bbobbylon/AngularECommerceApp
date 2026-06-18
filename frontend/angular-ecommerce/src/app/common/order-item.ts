import { CartItem } from './cart-item';

export class OrderItem {
  imageUrl: string;
  unitPrice: number;
  quantity: number;
  productId: number;
  /** Carried through to the order line so the backend draws down SKU-level inventory. */
  variantSku?: string;
  variantLabel?: string;

  constructor(cartItem: CartItem) {
    this.imageUrl = cartItem.imageUrl;
    this.unitPrice = cartItem.unitPrice;
    this.quantity = cartItem.quantity;
    this.productId = cartItem.id;
    this.variantSku = cartItem.variantSku;
    this.variantLabel = cartItem.variantLabel;
  }
}
