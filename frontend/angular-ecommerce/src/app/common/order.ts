export class Order {
  totalQuantity: number;
  totalPrice: number;
  /** Set at checkout from the server quote; recorded on the order for history/receipts. */
  shippingAmount?: number;
  taxAmount?: number;
  shippingMethod?: string;

  constructor(totalQuantity: number, totalPrice: number) {
    this.totalQuantity = totalQuantity;
    this.totalPrice = totalPrice;
  }
}
