import { Address } from './address';
import { Customer } from './customer';
import { Order } from './order';
import { OrderItem } from './order-item';

export class Purchase {
  customer!: Customer;
  shippingAddress!: Address;
  billingAddress!: Address;
  order!: Order;
  orderItems!: OrderItem[];
  /** Checkout opt-in: create the account on the weekly-deals list. */
  subscribeToNewsletter = true;
  /** Applied coupon (optional) + the pre-discount subtotal, so the server can re-validate. */
  couponCode?: string;
  subtotal?: number;
  /** Chosen shipping method code; the server recomputes shipping + tax authoritatively from it. */
  shippingMethodCode?: string;
  /** Stripe PaymentIntent id (card payments) so an approved return can refund the charge. */
  paymentIntentId?: string;
  /** Gift card code to redeem as store credit (server applies it against the order total). */
  giftCardCode?: string;
  /** Loyalty points to redeem as store credit (server-validated against the customer's balance). */
  pointsToRedeem?: number;
  /** Referral code the buyer arrived with (rewards both parties on their first order). */
  referralCode?: string;
}
