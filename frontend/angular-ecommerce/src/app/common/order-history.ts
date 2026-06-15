export interface OrderHistory {
  id: number;
  orderTrackingNumber: string;
  totalQuantity: number;
  totalPrice: number;
  status: string;
  dateCreated: Date;
}
