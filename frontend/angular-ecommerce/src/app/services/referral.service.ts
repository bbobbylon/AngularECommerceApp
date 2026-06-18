import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface ReferralSummary {
  email: string;
  code?: string | null;
  completedReferrals: number;
  pointsEarned: number;
  referrerReward: number;
  refereeReward: number;
}

/**
 * Referral tracking. Captures a `?ref=CODE` link parameter into localStorage on first load (the
 * service is instantiated early by the root component) so it can be attached to the buyer's first
 * checkout. Also fetches a customer's referral standing for the account page.
 */
@Injectable({ providedIn: 'root' })
export class ReferralService {

  private readonly storageKey = 'referralCode';
  private readonly baseUrl = `${environment.apiUrl}/referrals`;

  constructor(private http: HttpClient) {
    this.captureFromUrl();
  }

  private captureFromUrl(): void {
    try {
      const ref = new URLSearchParams(window.location.search).get('ref');
      if (ref && ref.trim()) {
        localStorage.setItem(this.storageKey, ref.trim().toUpperCase());
      }
    } catch {
      /* no window (SSR) — ignore */
    }
  }

  getStoredCode(): string | null {
    try {
      return localStorage.getItem(this.storageKey);
    } catch {
      return null;
    }
  }

  clear(): void {
    try {
      localStorage.removeItem(this.storageKey);
    } catch {
      /* ignore */
    }
  }

  summary(email: string): Observable<ReferralSummary> {
    return this.http.get<ReferralSummary>(this.baseUrl, { params: new HttpParams().set('email', email) });
  }
}
