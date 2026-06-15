import { CanActivateFn } from '@angular/router';
import { canActivateAuthGuard } from '@okta/okta-angular';

import { oktaConfig } from './okta-config';

/** True only when okta-config points at a real Okta org (not the shipped placeholder). */
export function isOktaConfigured(): boolean {
  return !oktaConfig.issuer.includes('dev-00000000') && !oktaConfig.clientId.includes('placeholder');
}

/**
 * When Okta isn't configured (local/dev), allow access so every page is viewable without an
 * identity provider. Once a real Okta org is wired up, delegate to Okta's real auth guard.
 */
export const devOrAuthGuard: CanActivateFn = (route, state) => {
  if (!isOktaConfigured()) {
    return true;
  }
  return canActivateAuthGuard(route, state);
};
