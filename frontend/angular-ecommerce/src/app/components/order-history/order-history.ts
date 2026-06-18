import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { OKTA_AUTH } from '@okta/okta-angular';

import { OrderHistory as OrderHistoryModel } from '../../common/order-history';
import { OrderHistoryService } from '../../services/order-history.service';
import { ReturnRequestView, ReturnService } from '../../services/return.service';
import { ToastService } from '../../services/toast.service';
import { OrderTimeline } from '../order-timeline/order-timeline';

@Component({
  selector: 'app-order-history',
  imports: [CommonModule, FormsModule, RouterLink, OrderTimeline],
  templateUrl: './order-history.html',
})
export class OrderHistory implements OnInit {

  orderHistoryList: OrderHistoryModel[] = [];
  loaded = false;
  demoMode = false;

  /** The signed-in email (from Okta), if any — used to authenticate returns + load their status. */
  email?: string;

  // returns
  private returnsByTracking = new Map<string, ReturnRequestView>();
  openReturnFor: string | null = null;
  returnReason = '';
  returnEmail = '';
  submittingReturn = false;

  private orderHistoryService = inject(OrderHistoryService);
  private returnService = inject(ReturnService);
  private toast = inject(ToastService);
  private oktaAuth = inject(OKTA_AUTH);

  async ngOnInit(): Promise<void> {
    try {
      if (await this.oktaAuth.isAuthenticated()) {
        const user = await this.oktaAuth.getUser();
        this.email = user.email;
      }
    } catch {
      // Okta not configured / not signed in — fall through to demo mode.
    }

    if (this.email) {
      this.orderHistoryService.getOrderHistory(this.email).subscribe(data => this.setOrders(data));
      this.loadReturns(this.email);
    } else {
      this.demoMode = true;
      this.orderHistoryService.getAllOrders().subscribe(data => this.setOrders(data));
    }
  }

  private setOrders(data: OrderHistoryModel[]): void {
    this.orderHistoryList = data;
    this.loaded = true;
  }

  private loadReturns(email: string): void {
    this.returnService.myReturns(email).subscribe({
      next: returns => returns.forEach(r => this.returnsByTracking.set(r.orderTrackingNumber, r)),
      error: () => { /* non-fatal — returns just won't show a status badge */ },
    });
  }

  returnFor(order: OrderHistoryModel): ReturnRequestView | undefined {
    return this.returnsByTracking.get(order.orderTrackingNumber);
  }

  toggleReturn(order: OrderHistoryModel): void {
    this.openReturnFor = this.openReturnFor === order.orderTrackingNumber ? null : order.orderTrackingNumber;
    this.returnReason = '';
    this.returnEmail = this.email ?? '';
  }

  submitReturn(order: OrderHistoryModel): void {
    const email = (this.email ?? this.returnEmail).trim();
    if (!email) {
      this.toast.error('Please enter the email used on this order.');
      return;
    }
    if (!this.returnReason.trim()) {
      this.toast.error('Please tell us why you’re returning the item(s).');
      return;
    }
    this.submittingReturn = true;
    this.returnService.createReturn({
      orderTrackingNumber: order.orderTrackingNumber,
      email,
      reason: this.returnReason.trim(),
    }).subscribe({
      next: view => {
        this.returnsByTracking.set(order.orderTrackingNumber, view);
        this.toast.success('Return requested — we’ll review it shortly.');
        this.openReturnFor = null;
        this.submittingReturn = false;
      },
      error: err => {
        this.submittingReturn = false;
        this.toast.error(err?.error?.message ?? 'Could not request a return for this order.');
      },
    });
  }

  returnBadgeClass(status: string): string {
    switch (status) {
      case 'REFUNDED': return 'bg-success-subtle text-success-emphasis';
      case 'APPROVED': return 'bg-info-subtle text-info-emphasis';
      case 'DENIED': return 'bg-danger-subtle text-danger-emphasis';
      default: return 'bg-warning-subtle text-warning-emphasis';
    }
  }
}
