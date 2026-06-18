import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface ReturnRequestView {
  id: number;
  orderId: number;
  orderTrackingNumber: string;
  customerEmail: string;
  reason: string;
  status: string;
  refundAmount?: number | null;
  adminNote?: string | null;
  refunded: boolean;
  dateCreated: string;
}

export interface CreateReturnRequest {
  orderTrackingNumber: string;
  email: string;
  reason: string;
}

export interface ReturnDecisionRequest {
  action: 'APPROVE' | 'DENY';
  refundAmount?: number | null;
  adminNote?: string;
}

@Injectable({ providedIn: 'root' })
export class ReturnService {

  private readonly baseUrl = `${environment.apiUrl}/returns`;
  private readonly adminUrl = `${environment.apiUrl}/admin/returns`;

  constructor(private http: HttpClient) {}

  createReturn(request: CreateReturnRequest): Observable<ReturnRequestView> {
    return this.http.post<ReturnRequestView>(this.baseUrl, request);
  }

  myReturns(email: string): Observable<ReturnRequestView[]> {
    return this.http.get<ReturnRequestView[]>(this.baseUrl, { params: new HttpParams().set('email', email) });
  }

  // ----- admin -----

  adminList(): Observable<ReturnRequestView[]> {
    return this.http.get<ReturnRequestView[]>(this.adminUrl);
  }

  decide(id: number, decision: ReturnDecisionRequest): Observable<ReturnRequestView> {
    return this.http.put<ReturnRequestView>(`${this.adminUrl}/${id}/decision`, decision);
  }
}
