import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface CouponResponse {
  valid: boolean;
  code: string;
  description: string | null;
  discount: number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class CouponService {

  private readonly baseUrl = `${environment.apiUrl}/coupons`;

  constructor(private http: HttpClient) {}

  validate(code: string, subtotal: number): Observable<CouponResponse> {
    return this.http.post<CouponResponse>(`${this.baseUrl}/validate`, { code, subtotal });
  }
}
