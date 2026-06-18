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
  private readonly shippingMethodsUrl = `${environment.apiUrl}/checkout/shipping-methods`;
  private readonly quoteUrl = `${environment.apiUrl}/checkout/quote`;

  constructor(private httpClient: HttpClient) {}

  placeOrder(purchase: Purchase): Observable<PurchaseResponse> {
    return this.httpClient.post<PurchaseResponse>(this.purchaseUrl, purchase);
  }

  createPaymentIntent(paymentInfo: PaymentInfo): Observable<PaymentIntentResponse> {
    return this.httpClient.post<PaymentIntentResponse>(this.paymentIntentUrl, paymentInfo);
  }

  /** Active shipping options for the checkout selector. */
  getShippingMethods(): Observable<ShippingMethodView[]> {
    return this.httpClient.get<ShippingMethodView[]>(this.shippingMethodsUrl);
  }

  /** Server-computed totals breakdown (discount + shipping + tax + total). */
  quote(request: QuoteRequest): Observable<QuoteResponse> {
    return this.httpClient.post<QuoteResponse>(this.quoteUrl, request);
  }
}

export interface PurchaseResponse {
  orderTrackingNumber: string;
}

export interface PaymentIntentResponse {
  client_secret: string;
}

export interface ShippingMethodView {
  id: number;
  code: string;
  name: string;
  baseRate: number;
  freeOverThreshold?: number | null;
  estimatedDays?: string;
}

export interface QuoteRequest {
  subtotal: number;
  country?: string;
  state?: string;
  couponCode?: string;
  shippingMethodCode?: string;
}

export interface QuoteResponse {
  subtotal: number;
  discount: number;
  shippingAmount: number;
  taxAmount: number;
  taxRatePercent: number;
  total: number;
  shippingMethodCode?: string;
}
