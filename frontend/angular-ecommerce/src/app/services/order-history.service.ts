import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../environments/environment';
import { OrderHistory } from '../common/order-history';

/**
 * Reads past orders via the tenant-scoped `AccountController`/`OrderHistoryController` endpoints
 * (plain paged JSON, not HAL). Previously read the Spring Data REST `Order` resource directly
 * (`GET /api/orders/**`) via its `findByCustomerEmailOrderByDateCreatedDesc` search projection and a
 * raw, unfiltered collection GET — both took no tenant predicate at all, so any caller could read any
 * tenant's order history; closed as part of roadmap #21's tenant-scoping audit. `getAllOrders()` is
 * still a demo-mode fallback for when no one is signed in, so the order-history page still renders
 * something — just scoped to the current tenant now instead of every tenant.
 */
@Injectable({ providedIn: 'root' })
export class OrderHistoryService {

  private readonly apiUrl = environment.apiUrl;

  constructor(private httpClient: HttpClient) {}

  getOrderHistory(email: string): Observable<OrderHistory[]> {
    const url = `${this.apiUrl}/account/orders?email=${encodeURIComponent(email)}`;
    return this.httpClient
      .get<GetResponseOrderHistory>(url)
      .pipe(map(response => response.content ?? []));
  }

  /** Demo fallback when no user is signed in — shows recent orders so the page renders. */
  getAllOrders(): Observable<OrderHistory[]> {
    const url = `${this.apiUrl}/order-history/recent`;
    return this.httpClient
      .get<GetResponseOrderHistory>(url)
      .pipe(map(response => response.content ?? []));
  }
}

interface GetResponseOrderHistory {
  content?: OrderHistory[];
}
