import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { PaymentInfo } from '../common/payment-info';
import { Purchase } from '../common/purchase';

@Injectable({ providedIn: 'root' })
export class CheckoutService {

  private readonly purchaseUrl = `${environment.apiUrl}/checkout/purchase`;
  private readonly paymentIntentUrl = `${environment.apiUrl}/checkout/payment-intent`;

  constructor(private httpClient: HttpClient) {}

  placeOrder(purchase: Purchase): Observable<PurchaseResponse> {
    return this.httpClient.post<PurchaseResponse>(this.purchaseUrl, purchase);
  }

  createPaymentIntent(paymentInfo: PaymentInfo): Observable<PaymentIntentResponse> {
    return this.httpClient.post<PaymentIntentResponse>(this.paymentIntentUrl, paymentInfo);
  }
}

export interface PurchaseResponse {
  orderTrackingNumber: string;
}

export interface PaymentIntentResponse {
  client_secret: string;
}
