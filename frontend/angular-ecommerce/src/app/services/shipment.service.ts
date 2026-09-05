import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface ShipmentView {
  id: number;
  orderId: number;
  orderTrackingNumber: string;
  warehouseId?: number | null;
  warehouseCode?: string | null;
  warehouseName?: string | null;
  carrier?: string | null;
  trackingNumber?: string | null;
  status: string;
  shippedAt?: string | null;
  deliveredAt?: string | null;
  note?: string | null;
  dateCreated: string;
}

/** Customer-facing shipment lookup (roadmap #20) — keyed like returns: order tracking number + email. */
@Injectable({ providedIn: 'root' })
export class ShipmentService {

  private readonly baseUrl = `${environment.apiUrl}/shipments`;

  constructor(private http: HttpClient) {}

  track(orderTrackingNumber: string, email: string): Observable<ShipmentView[]> {
    const params = new HttpParams().set('orderTrackingNumber', orderTrackingNumber).set('email', email);
    return this.http.get<ShipmentView[]>(this.baseUrl, { params });
  }
}
