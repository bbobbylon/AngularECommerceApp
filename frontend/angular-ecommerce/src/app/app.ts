import { DOCUMENT } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';

import { BackToTop } from './components/back-to-top/back-to-top';
import { CartStatus } from './components/cart-status/cart-status';
import { InstallPrompt } from './components/install-prompt/install-prompt';
import { LoginStatus } from './components/login-status/login-status';
import { NewsletterSignup } from './components/newsletter-signup/newsletter-signup';
import { ProductCategoryMenu } from './components/product-category-menu/product-category-menu';
import { Search } from './components/search/search';
import { Toast } from './components/toast/toast';
import { TranslatePipe } from './common/translate.pipe';
import { CurrencyService } from './services/currency.service';
import { FavoritesService } from './services/favorites.service';
import { I18nService } from './services/i18n.service';
import { ReferralService } from './services/referral.service';
import { SeoService } from './services/seo.service';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Search, ProductCategoryMenu, CartStatus, LoginStatus, Toast, BackToTop, InstallPrompt, NewsletterSignup, TranslatePipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly themeService = inject(ThemeService);
  protected readonly favorites = inject(FavoritesService);
  protected readonly currencyService = inject(CurrencyService);
  protected readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  // Instantiated here so it captures any ?ref= referral link parameter on first load.
  private readonly referral = inject(ReferralService);
  private readonly seo = inject(SeoService);
  private readonly document = inject(DOCUMENT);
  title = 'angular-ecommerce';

  constructor() {
    // Site-wide structured data (roadmap #11 — SEO), set once; per-page JSON-LD (e.g. Product) is
    // managed separately by SeoService callers under a different id so this doesn't get clobbered.
    this.seo.setJsonLd('organization', {
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'Organization',
          name: 'Luv2Shop',
          url: this.document.location.origin,
          logo: `${this.document.location.origin}/favicon.ico`,
        },
        {
          '@type': 'WebSite',
          name: 'Luv2Shop',
          url: this.document.location.origin,
        },
      ],
    });
  }

  /** Admin routes get a full-width canvas — hide the customer category sidebar there. */
  protected readonly isAdminRoute = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(() => this.router.url.startsWith('/admin')),
      startWith(this.router.url.startsWith('/admin')),
    ),
    { initialValue: this.router.url.startsWith('/admin') },
  );
}
