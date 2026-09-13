import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { OrderTimeline } from '../order-timeline/order-timeline';

interface OrderSummaryItem {
  name: string;
  imageUrl: string;
  quantity: number;
  unitPrice: number;
}

interface OrderSummary {
  totalQuantity: number;
  totalPrice: number;
  items: OrderSummaryItem[];
}

/**
 * Post-checkout landing page (`/order-confirmation/:trackingNumber`). The order summary is read from
 * router navigation state set by `checkout.ts` on submit — present on the flow that placed the order,
 * `undefined` on a direct visit/refresh, which the template must handle gracefully.
 */
@Component({
  selector: 'app-order-confirmation',
  imports: [CommonModule, RouterLink, OrderTimeline],
  templateUrl: './order-confirmation.html',
})
export class OrderConfirmation {
  trackingNumber = inject(ActivatedRoute).snapshot.paramMap.get('trackingNumber') ?? '';

  // Passed via router navigation state from checkout; absent on a direct visit.
  summary = (history.state as { summary?: OrderSummary }).summary;
}
