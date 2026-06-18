import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface LoyaltyTransactionView {
  type: string;
  points: number;
  description: string;
  dateCreated: string;
}

export interface LoyaltySummary {
  email: string;
  balance: number;
  lifetimePoints: number;
  tier: string;
  nextTier?: string | null;
  pointsToNextTier: number;
  redeemableValue: number;
  history: LoyaltyTransactionView[];
}

@Injectable({ providedIn: 'root' })
export class LoyaltyService {

  private readonly baseUrl = `${environment.apiUrl}/loyalty`;

  constructor(private http: HttpClient) {}

  summary(email: string): Observable<LoyaltySummary> {
    return this.http.get<LoyaltySummary>(this.baseUrl, { params: new HttpParams().set('email', email) });
  }
}
