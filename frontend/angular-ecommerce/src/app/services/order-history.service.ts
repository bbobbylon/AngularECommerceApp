import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../environments/environment';
import { OrderHistory } from '../common/order-history';

/**
 * Reads past orders from the Spring Data REST `Order` resource directly (`GET /api/orders/**`, no
 * custom controller) via its `findByCustomerEmailOrderByDateCreatedDesc` search projection. `getAllOrders()`
 * is a demo-mode fallback for when no one is signed in, so the order-history page still renders something.
 */
@Injectable({ providedIn: 'root' })
export class OrderHistoryService {

  private readonly ordersUrl = `${environment.apiUrl}/orders`;

  constructor(private httpClient: HttpClient) {}

  getOrderHistory(email: string): Observable<OrderHistory[]> {
    const url =
      `${this.ordersUrl}/search/findByCustomerEmailOrderByDateCreatedDesc` +
      `?email=${encodeURIComponent(email)}`;
    return this.httpClient
      .get<GetResponseOrderHistory>(url)
      .pipe(map(response => response._embedded?.orders ?? []));
  }

  /** Demo fallback when no user is signed in — shows recent orders so the page renders. */
  getAllOrders(): Observable<OrderHistory[]> {
    const url = `${this.ordersUrl}?sort=dateCreated,desc&size=50`;
    return this.httpClient
      .get<GetResponseOrderHistory>(url)
      .pipe(map(response => response._embedded?.orders ?? []));
  }
}

interface GetResponseOrderHistory {
  _embedded?: {
    orders: OrderHistory[];
  };
}
