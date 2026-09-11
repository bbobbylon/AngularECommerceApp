import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface AccountPreferences {
  firstName: string;
  lastName: string;
  email: string;
  newsletterSubscribed: boolean;
}

export interface AccountUpdate {
  email: string;
  firstName?: string;
  lastName?: string;
  newsletterSubscribed?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AccountService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getPreferences(email: string): Observable<AccountPreferences> {
    const params = new HttpParams().set('email', email);
    return this.http.get<AccountPreferences>(`${this.baseUrl}/account`, { params });
  }

  updatePreferences(update: AccountUpdate): Observable<AccountPreferences> {
    return this.http.put<AccountPreferences>(`${this.baseUrl}/account`, update);
  }

  // ----- address book -----

  getAddresses(email: string): Observable<SavedAddress[]> {
    return this.http.get<SavedAddress[]>(`${this.baseUrl}/account/addresses`, { params: new HttpParams().set('email', email) });
  }

  saveAddress(email: string, address: SavedAddress): Observable<SavedAddress> {
    return this.http.post<SavedAddress>(`${this.baseUrl}/account/addresses`, address, { params: new HttpParams().set('email', email) });
  }

  deleteAddress(email: string, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/account/addresses/${id}`, { params: new HttpParams().set('email', email) });
  }

  // ----- saved payment methods -----

  getPaymentMethods(email: string): Observable<SavedPaymentMethod[]> {
    return this.http.get<SavedPaymentMethod[]>(`${this.baseUrl}/account/payment-methods`, { params: new HttpParams().set('email', email) });
  }

  createSetupIntent(email: string): Observable<SetupIntentResponse> {
    return this.http.post<SetupIntentResponse>(`${this.baseUrl}/account/payment-methods/setup-intent`, null, { params: new HttpParams().set('email', email) });
  }

  recordPaymentMethod(email: string, paymentMethodId: string): Observable<SavedPaymentMethod> {
    return this.http.post<SavedPaymentMethod>(`${this.baseUrl}/account/payment-methods`, { paymentMethodId }, { params: new HttpParams().set('email', email) });
  }

  deletePaymentMethod(email: string, id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/account/payment-methods/${id}`, { params: new HttpParams().set('email', email) });
  }
}

export interface SavedAddress {
  id?: number | null;
  label?: string | null;
  recipientName?: string | null;
  street: string;
  city: string;
  state: string;
  country: string;
  zipCode: string;
  defaultAddress: boolean;
}

export interface SavedPaymentMethod {
  id: number;
  brand?: string | null;
  last4?: string | null;
  expMonth?: number | null;
  expYear?: number | null;
  defaultMethod: boolean;
}

export interface SetupIntentResponse {
  enabled: boolean;
  clientSecret?: string | null;
}
