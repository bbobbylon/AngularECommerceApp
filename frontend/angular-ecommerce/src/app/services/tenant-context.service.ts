import { Injectable, signal } from '@angular/core';

/**
 * Which tenant the admin back-office is currently "viewing as" (roadmap #21, Milestone B). Set by a
 * superadmin's tenant switcher on the platform tenants page; read by `authInterceptor` to attach
 * `X-Tenant-Id` on `/api/admin/**` calls. `null` (the default) means "no override" — the backend falls
 * back to its own header/subdomain/default-slug resolution, so this is a no-op for every deployment
 * that never uses the platform tier.
 */
@Injectable({ providedIn: 'root' })
export class TenantContextService {
  readonly activeTenantSlug = signal<string | null>(null);

  viewAs(slug: string): void {
    this.activeTenantSlug.set(slug);
  }

  reset(): void {
    this.activeTenantSlug.set(null);
  }
}
