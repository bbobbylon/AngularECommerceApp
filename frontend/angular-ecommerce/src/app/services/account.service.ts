import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface AccountPreferences {
  firstName: string;
  lastName: string;
  email: string;
  newsletterSubscribed: boolean;
}

export interface AccountUpdate {
  email: string;
  firstName?: string;
  lastName?: string;
  newsletterSubscribed?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AccountService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getPreferences(email: string): Observable<AccountPreferences> {
    const params = new HttpParams().set('email', email);
    return this.http.get<AccountPreferences>(`${this.baseUrl}/account`, { params });
  }

  updatePreferences(update: AccountUpdate): Observable<AccountPreferences> {
    return this.http.put<AccountPreferences>(`${this.baseUrl}/account`, update);
  }
}
