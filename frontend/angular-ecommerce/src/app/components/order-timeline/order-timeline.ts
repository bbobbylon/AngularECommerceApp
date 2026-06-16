import { Component, Input } from '@angular/core';

/** Horizontal fulfillment tracker: Received → Processing → Shipped → Delivered (or Cancelled). */
@Component({
  selector: 'app-order-timeline',
  templateUrl: './order-timeline.html',
})
export class OrderTimeline {

  @Input() status: string | null | undefined = 'Received';

  readonly steps = ['Received', 'Processing', 'Shipped', 'Delivered'];

  get currentIndex(): number {
    const status = (this.status || 'Received').toLowerCase();
    const index = this.steps.findIndex(s => s.toLowerCase() === status);
    return index < 0 ? 0 : index;
  }

  get cancelled(): boolean {
    return (this.status || '').toLowerCase() === 'cancelled';
  }
}
