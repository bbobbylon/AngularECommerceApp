import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WishlistService {

  private readonly baseUrl = `${environment.apiUrl}/wishlist`;

  constructor(private http: HttpClient) {}

  get(email: string): Observable<number[]> {
    const params = new HttpParams().set('email', email);
    return this.http.get<number[]>(this.baseUrl, { params });
  }

  /** Merge local ids into the account wishlist; returns the merged id list. */
  sync(email: string, productIds: number[]): Observable<number[]> {
    return this.http.post<number[]>(`${this.baseUrl}/sync`, { email, productIds });
  }

  remove(email: string, productId: number): Observable<void> {
    const params = new HttpParams().set('email', email).set('productId', productId);
    return this.http.delete<void>(this.baseUrl, { params });
  }
}
