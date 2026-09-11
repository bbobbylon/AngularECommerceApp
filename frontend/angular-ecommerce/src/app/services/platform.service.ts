import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface PlatformTenant {
  id: number;
  slug: string;
  displayName: string;
  contactEmail?: string | null;
  active: boolean;
  dateCreated: string;
  planId?: number | null;
  planName?: string | null;
  billingStatus: string;
}

export interface PlatformTenantPayload {
  id?: number | null;
  slug: string;
  displayName: string;
  contactEmail?: string | null;
  active: boolean;
}

export interface BillingPlan {
  id: number;
  name: string;
  monthlyPrice: number;
  currency: string;
  features?: string | null;
  active: boolean;
  sortOrder: number;
  dateCreated: string;
}

export interface BillingPlanPayload {
  id?: number | null;
  name: string;
  monthlyPrice: number;
  currency?: string | null;
  features?: string | null;
  sortOrder?: number | null;
  active: boolean;
}

/**
 * Platform-level tenant + billing-plan management (roadmap #21 Milestone B, roadmap #22) — separate
 * from {@link AdminService} since it hits a different, tenant-agnostic base path
 * (`/api/platform/**`, `SuperAdmin`-gated) rather than the tenant-scoped `/api/admin/**`.
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

  assignTenantPlan(tenantId: number, planId: number | null): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/tenants/${tenantId}/billing-plan`, { planId });
  }

  getBillingPlans(): Observable<BillingPlan[]> {
    return this.http.get<BillingPlan[]>(`${this.baseUrl}/billing-plans`);
  }

  saveBillingPlan(payload: BillingPlanPayload): Observable<BillingPlan> {
    return this.http.post<BillingPlan>(`${this.baseUrl}/billing-plans`, payload);
  }

  deactivateBillingPlan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/billing-plans/${id}`);
  }
}
