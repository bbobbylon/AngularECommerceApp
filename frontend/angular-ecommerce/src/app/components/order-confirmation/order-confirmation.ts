import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

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

@Component({
  selector: 'app-order-confirmation',
  imports: [CommonModule, RouterLink],
  templateUrl: './order-confirmation.html',
})
export class OrderConfirmation {
  trackingNumber = inject(ActivatedRoute).snapshot.paramMap.get('trackingNumber') ?? '';

  // Passed via router navigation state from checkout; absent on a direct visit.
  summary = (history.state as { summary?: OrderSummary }).summary;
}
