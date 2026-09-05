import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { environment } from '../../environments/environment';

export interface SiteBanner {
  id: number;
  message: string;
  linkUrl?: string | null;
  linkText?: string | null;
  active: boolean;
}

export interface FaqEntry {
  id: number;
  question: string;
  answer: string;
  sortOrder: number;
  active: boolean;
}

/** Public reads for the CMS-managed content (roadmap #17): the site banner and the FAQ list. */
@Injectable({ providedIn: 'root' })
export class ContentService {

  private readonly baseUrl = `${environment.apiUrl}/content`;

  constructor(private http: HttpClient) {}

  /** Null when no banner is configured or it's turned off (the server returns 204 for both). */
  getBanner(): Observable<SiteBanner | null> {
    return this.http.get<SiteBanner>(`${this.baseUrl}/banner`).pipe(catchError(() => of(null)));
  }

  getFaq(): Observable<FaqEntry[]> {
    return this.http.get<FaqEntry[]>(`${this.baseUrl}/faq`).pipe(catchError(() => of([])));
  }
}
