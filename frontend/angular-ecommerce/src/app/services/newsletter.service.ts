import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface SubscribeResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NewsletterService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** Join the weekly-deals list. Backend sends a welcome email + creates/reactivates the subscriber. */
  subscribe(email: string, name?: string): Observable<SubscribeResponse> {
    return this.http.post<SubscribeResponse>(`${this.baseUrl}/newsletter/subscribe`, { email, name });
  }
}
