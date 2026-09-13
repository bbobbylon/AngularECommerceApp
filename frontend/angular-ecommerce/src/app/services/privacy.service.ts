import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../environments/environment';

export interface ConsentDecision {
  id: number;
  necessary: boolean;
  functional: boolean;
  analytics: boolean;
  marketing: boolean;
  policyVersion: string;
  source: string;
  dateCreated: string;
}

export interface ConsentSubmission {
  visitorId: string;
  email?: string | null;
  functional: boolean;
  analytics: boolean;
  marketing: boolean;
  source: 'BANNER' | 'ACCOUNT';
}

export interface DataRequestResult {
  message: string;
  request: {
    id: number;
    requestType: 'EXPORT' | 'ERASURE';
    status: string;
    resultSummary: string | null;
    dateCreated: string;
    completedAt: string | null;
  };
}

/**
 * HTTP client for `/api/privacy/**` (roadmap #24): cookie/storage consent and the self-service
 * data-subject export/erasure flow. Every call here is unauthenticated by design — see
 * `PrivacyController`'s javadoc — so failures degrade to "nothing changes" rather than surfacing
 * as broken UI, the same graceful-degradation contract `ContentService` established.
 */
@Injectable({ providedIn: 'root' })
export class PrivacyService {

  private readonly baseUrl = `${environment.apiUrl}/privacy`;

  constructor(private http: HttpClient) {}

  /** The policy version currently in force. Falls back to an empty string on failure. */
  config(): Observable<{ policyVersion: string }> {
    return this.http.get<{ policyVersion: string }>(`${this.baseUrl}/config`)
      .pipe(catchError(() => of({ policyVersion: '' })));
  }

  /** Null when this visitor has never recorded a choice (the server returns 204). */
  currentConsent(visitorId: string): Observable<ConsentDecision | null> {
    return this.http.get<ConsentDecision>(`${this.baseUrl}/consent`, { params: { visitorId } })
      .pipe(catchError(() => of(null)));
  }

  recordConsent(submission: ConsentSubmission): Observable<ConsentDecision> {
    return this.http.post<ConsentDecision>(`${this.baseUrl}/consent`, submission);
  }

  /** Raises an export or erasure request; the server always mails a confirmation link. */
  submitDataRequest(email: string, requestType: 'EXPORT' | 'ERASURE'): Observable<DataRequestResult> {
    return this.http.post<DataRequestResult>(`${this.baseUrl}/data-requests`, { email, requestType });
  }
}
