import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OKTA_AUTH } from '@okta/okta-angular';

import { environment } from '../../environments/environment';
import { TenantContextService } from '../services/tenant-context.service';

/**
 * Attaches the Okta access token as a Bearer header on calls to the secured endpoints
 * (`/api/orders/**` order history, `/api/account/**` settings, `/api/admin/**` back-office, and
 * `/api/platform/**` — the superadmin tier, roadmap #21 Milestone B). These match the server-side
 * `authenticated()`/`hasAuthority()` rules in SecurityConfig's secured chain. Everything else
 * (catalog, cart, checkout) is public and untouched.
 *
 * <p>When a superadmin has picked a tenant to "view as" via the platform tenant switcher
 * ({@link TenantContextService}), also attach `X-Tenant-Id` on `/api/admin/**` calls so the regular
 * back-office reflects that tenant's data instead of whichever tenant this deployment resolves to by
 * default. Scoped to `/admin` only — `/api/platform/**` itself is tenant-agnostic, so the header would
 * be meaningless there. The signal defaults to `null`, so this is a no-op for every existing caller.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const oktaAuth = inject(OKTA_AUTH);
  const tenantContext = inject(TenantContextService);
  const adminPrefix = `${environment.apiUrl}/admin`;
  const securedPrefixes = [
    `${environment.apiUrl}/orders`,
    `${environment.apiUrl}/account`,
    adminPrefix,
    `${environment.apiUrl}/platform`,
  ];

  if (securedPrefixes.some(prefix => req.urlWithParams.startsWith(prefix))) {
    const accessToken = oktaAuth.getAccessToken();
    if (accessToken) {
      req = req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } });
    }
  }

  const activeTenantSlug = tenantContext.activeTenantSlug();
  if (activeTenantSlug && req.urlWithParams.startsWith(adminPrefix)) {
    req = req.clone({ setHeaders: { 'X-Tenant-Id': activeTenantSlug } });
  }

  return next(req);
};
