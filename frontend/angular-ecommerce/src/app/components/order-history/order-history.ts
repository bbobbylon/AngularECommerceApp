import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { OktaAuthStateService } from '@okta/okta-angular';
import { filter, take } from 'rxjs';

import { OrderHistory as OrderHistoryModel } from '../../common/order-history';
import { OrderHistoryService } from '../../services/order-history.service';

@Component({
  selector: 'app-order-history',
  imports: [CommonModule],
  templateUrl: './order-history.html',
})
export class OrderHistory implements OnInit {

  orderHistoryList: OrderHistoryModel[] = [];
  loaded = false;

  private orderHistoryService = inject(OrderHistoryService);
  private authStateService = inject(OktaAuthStateService);

  ngOnInit(): void {
    this.authStateService.authState$
      .pipe(
        filter(state => !!state.isAuthenticated),
        take(1),
      )
      .subscribe(state => {
        const email = state.idToken?.claims?.email as string | undefined;
        if (email) {
          this.orderHistoryService.getOrderHistory(email).subscribe(data => {
            this.orderHistoryList = data;
            this.loaded = true;
          });
        } else {
          this.loaded = true;
        }
      });
  }
}
