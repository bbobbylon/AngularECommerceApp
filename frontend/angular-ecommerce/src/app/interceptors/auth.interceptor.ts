import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OKTA_AUTH } from '@okta/okta-angular';

import { environment } from '../../environments/environment';

/**
 * Attaches the Okta access token as a Bearer header on calls to the secured endpoints
 * (`/api/orders/**` order history, `/api/account/**` settings, and `/api/admin/**` back-office).
 * These match the server-side `authenticated()` rules in SecurityConfig's secured chain. Everything
 * else (catalog, cart, checkout) is public and untouched.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const oktaAuth = inject(OKTA_AUTH);
  const securedPrefixes = [
    `${environment.apiUrl}/orders`,
    `${environment.apiUrl}/account`,
    `${environment.apiUrl}/admin`,
  ];

  if (securedPrefixes.some(prefix => req.urlWithParams.startsWith(prefix))) {
    const accessToken = oktaAuth.getAccessToken();
    if (accessToken) {
      req = req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } });
    }
  }

  return next(req);
};
