import { ApplicationConfig, inject, isDevMode, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { provideOktaAuth, withOktaConfig } from '@okta/okta-angular';
import { OktaAuth } from '@okta/okta-auth-js';

import { routes } from './app.routes';
import { oktaConfig } from './auth/okta-config';
import { authInterceptor } from './interceptors/auth.interceptor';
import { ConfigService } from './services/config.service';

const oktaAuth = new OktaAuth(oktaConfig);

/**
 * App-wide DI providers. The service worker is gated on `isDevMode()` rather than
 * `environment.production` — this project's `environment.ts` has no `fileReplacements` wired in
 * `angular.json`, so that flag is always `false` and would never actually enable the worker
 * (roadmap #12). `provideAppInitializer` blocks first render on `ConfigService.load()` so a
 * runtime-supplied Stripe key (from `/config.json`) is available before any component reads it.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // load /config.json (runtime Stripe key etc.) before the app starts
    provideAppInitializer(() => inject(ConfigService).load()),
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideOktaAuth(withOktaConfig({ oktaAuth })),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
  ],
};
