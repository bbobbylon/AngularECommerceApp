import { DOCUMENT } from '@angular/common';
import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';

import { BackToTop } from './components/back-to-top/back-to-top';
import { CartStatus } from './components/cart-status/cart-status';
import { CookieConsent } from './components/cookie-consent/cookie-consent';
import { InstallPrompt } from './components/install-prompt/install-prompt';
import { LoginStatus } from './components/login-status/login-status';
import { NewsletterSignup } from './components/newsletter-signup/newsletter-signup';
import { ProductCategoryMenu } from './components/product-category-menu/product-category-menu';
import { Search } from './components/search/search';
import { Toast } from './components/toast/toast';
import { TranslatePipe } from './common/translate.pipe';
import { ContentService, SiteBanner } from './services/content.service';
import { ConsentService } from './services/consent.service';
import { CurrencyService } from './services/currency.service';
import { FavoritesService } from './services/favorites.service';
import { I18nService } from './services/i18n.service';
import { ReferralService } from './services/referral.service';
import { SeoService } from './services/seo.service';
import { ThemeService } from './services/theme.service';

/**
 * The root shell: header (search, category menu, cart, login status), footer, and every
 * globally-mounted overlay (`Toast`, `BackToTop`, `InstallPrompt`, `CookieConsent`). Fetches the
 * CMS banner (#17) once on load and injects the site-wide Organization/WebSite JSON-LD (#11).
 * `ReferralService` and `SeoService` are injected here purely for their side effects (capturing a
 * `?ref=` param and setting per-route meta tags respectively) — neither is read from the template.
 */
@Component({
  selector: 'app-root',
  imports: [
    FormsModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    Search,
    ProductCategoryMenu,
    CartStatus,
    LoginStatus,
    Toast,
    BackToTop,
    InstallPrompt,
    CookieConsent,
    NewsletterSignup,
    TranslatePipe,
  ],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
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
  private readonly contentService = inject(ContentService);
  protected readonly consentService = inject(ConsentService);
  title = 'angular-ecommerce';

  /** CMS-managed announcement banner (roadmap #17). Null hides the bar entirely. */
  protected readonly banner = signal<SiteBanner | null>(null);

  constructor() {
    this.contentService.getBanner().subscribe((banner) => this.banner.set(banner));

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
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url.startsWith('/admin')),
      startWith(this.router.url.startsWith('/admin')),
    ),
    { initialValue: this.router.url.startsWith('/admin') },
  );
}
