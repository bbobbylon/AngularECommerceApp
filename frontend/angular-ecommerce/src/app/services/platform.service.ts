import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface PlatformTenant {
  id: number;
  slug: string;
  displayName: string;
  contactEmail?: string | null;
  plan?: string | null;
  active: boolean;
  dateCreated: string;
}

export interface PlatformTenantPayload {
  id?: number | null;
  slug: string;
  displayName: string;
  contactEmail?: string | null;
  plan?: string | null;
  active: boolean;
}

/**
 * Platform-level tenant management (roadmap #21, Milestone B) — separate from {@link AdminService}
 * since it hits a different, tenant-agnostic base path (`/api/platform/**`, `SuperAdmin`-gated) rather
 * than the tenant-scoped `/api/admin/**`.
 */
@Injectable({ providedIn: 'root' })
export class PlatformService {

  private readonly baseUrl = `${environment.apiUrl}/platform`;

  constructor(private http: HttpClient) {}

  getTenants(): Observable<PlatformTenant[]> {
    return this.http.get<PlatformTenant[]>(`${this.baseUrl}/tenants`);
  }

  saveTenant(payload: PlatformTenantPayload): Observable<PlatformTenant> {
    return this.http.post<PlatformTenant>(`${this.baseUrl}/tenants`, payload);
  }

  deactivateTenant(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tenants/${id}`);
  }
}
