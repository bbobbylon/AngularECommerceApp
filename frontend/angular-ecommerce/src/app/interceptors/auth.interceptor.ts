import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OKTA_AUTH } from '@okta/okta-angular';

import { environment } from '../../environments/environment';

/**
 * Attaches the Okta access token as a Bearer header on calls to the secured
 * order-history endpoints (`/api/orders/**`). Everything else is left untouched.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const oktaAuth = inject(OKTA_AUTH);
  const securedPrefix = `${environment.apiUrl}/orders`;

  if (req.urlWithParams.startsWith(securedPrefix)) {
    const accessToken = oktaAuth.getAccessToken();
    if (accessToken) {
      req = req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } });
    }
  }

  return next(req);
};
