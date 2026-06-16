import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OKTA_AUTH } from '@okta/okta-angular';

import { OrderHistory as OrderHistoryModel } from '../../common/order-history';
import { OrderHistoryService } from '../../services/order-history.service';
import { OrderTimeline } from '../order-timeline/order-timeline';

@Component({
  selector: 'app-order-history',
  imports: [CommonModule, RouterLink, OrderTimeline],
  templateUrl: './order-history.html',
})
export class OrderHistory implements OnInit {

  orderHistoryList: OrderHistoryModel[] = [];
  loaded = false;
  demoMode = false;

  private orderHistoryService = inject(OrderHistoryService);
  private oktaAuth = inject(OKTA_AUTH);

  async ngOnInit(): Promise<void> {
    let email: string | undefined;

    try {
      if (await this.oktaAuth.isAuthenticated()) {
        const user = await this.oktaAuth.getUser();
        email = user.email;
      }
    } catch {
      // Okta not configured / not signed in — fall through to demo mode.
    }

    if (email) {
      this.orderHistoryService.getOrderHistory(email).subscribe(data => this.setOrders(data));
    } else {
      this.demoMode = true;
      this.orderHistoryService.getAllOrders().subscribe(data => this.setOrders(data));
    }
  }

  private setOrders(data: OrderHistoryModel[]): void {
    this.orderHistoryList = data;
    this.loaded = true;
  }
}
