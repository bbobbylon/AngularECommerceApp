import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { PageResponse } from './admin.service';

export interface Review {
  id: number;
  productId: number;
  authorName: string;
  rating: number;
  comment: string;
  verifiedBuyer: boolean;
  dateCreated: string;
}

export interface ReviewSummary {
  average: number;
  count: number;
  distribution: number[]; // index 0 = 1★ … index 4 = 5★
}

export interface ReviewPayload {
  productId: number;
  authorName: string;
  rating: number;
  comment: string;
}

@Injectable({ providedIn: 'root' })
export class ReviewService {

  private readonly baseUrl = `${environment.apiUrl}/reviews`;

  constructor(private http: HttpClient) {}

  list(productId: number, page = 0, size = 5): Observable<PageResponse<Review>> {
    const params = new HttpParams().set('productId', productId).set('page', page).set('size', size);
    return this.http.get<PageResponse<Review>>(this.baseUrl, { params });
  }

  summary(productId: number): Observable<ReviewSummary> {
    const params = new HttpParams().set('productId', productId);
    return this.http.get<ReviewSummary>(`${this.baseUrl}/summary`, { params });
  }

  create(payload: ReviewPayload): Observable<Review> {
    return this.http.post<Review>(this.baseUrl, payload);
  }
}
