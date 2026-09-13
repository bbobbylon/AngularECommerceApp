import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, effect } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { ConsentService } from './consent.service';

export interface ReferralSummary {
  email: string;
  code?: string | null;
  completedReferrals: number;
  pointsEarned: number;
  referrerReward: number;
  refereeReward: number;
}

/**
 * Referral tracking. Captures a `?ref=CODE` link parameter on first load (the service is
 * instantiated early by the root component), but only *persists* it to localStorage once
 * "marketing" cookie consent (roadmap #24) is granted — a link clicked before that decision is
 * held in memory only and is lost on refresh if the visitor declines, rather than being written
 * to storage up front and only checked afterward.
 */
@Injectable({ providedIn: 'root' })
export class ReferralService {

  private readonly storageKey = 'referralCode';
  private readonly baseUrl = `${environment.apiUrl}/referrals`;
  private pendingCode: string | null = null;

  constructor(private http: HttpClient, private consentService: ConsentService) {
    this.pendingCode = this.readFromUrl();
    effect(() => {
      if (this.pendingCode && this.consentService.isAllowed('marketing')) {
        this.persist(this.pendingCode);
        this.pendingCode = null;
      }
    });
  }

  private readFromUrl(): string | null {
    try {
      const ref = new URLSearchParams(window.location.search).get('ref');
      return ref && ref.trim() ? ref.trim().toUpperCase() : null;
    } catch {
      return null; // no window (SSR)
    }
  }

  private persist(code: string): void {
    try {
      localStorage.setItem(this.storageKey, code);
    } catch {
      /* storage unavailable (e.g. private mode) — the referral capture is just lost */
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
