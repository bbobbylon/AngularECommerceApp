import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideOktaAuth, withOktaConfig } from '@okta/okta-angular';
import { OktaAuth } from '@okta/okta-auth-js';

import { routes } from './app.routes';
import { oktaConfig } from './auth/okta-config';
import { authInterceptor } from './interceptors/auth.interceptor';
import { ConfigService } from './services/config.service';

const oktaAuth = new OktaAuth(oktaConfig);

export const appConfig: ApplicationConfig = {
  providers: [
    // load /config.json (runtime Stripe key etc.) before the app starts
    provideAppInitializer(() => inject(ConfigService).load()),
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideOktaAuth(withOktaConfig({ oktaAuth })),
  ],
};
