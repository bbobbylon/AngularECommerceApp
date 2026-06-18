import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';

import { BackToTop } from './components/back-to-top/back-to-top';
import { CartStatus } from './components/cart-status/cart-status';
import { LoginStatus } from './components/login-status/login-status';
import { NewsletterSignup } from './components/newsletter-signup/newsletter-signup';
import { ProductCategoryMenu } from './components/product-category-menu/product-category-menu';
import { Search } from './components/search/search';
import { Toast } from './components/toast/toast';
import { FavoritesService } from './services/favorites.service';
import { ReferralService } from './services/referral.service';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Search, ProductCategoryMenu, CartStatus, LoginStatus, Toast, BackToTop, NewsletterSignup],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly themeService = inject(ThemeService);
  protected readonly favorites = inject(FavoritesService);
  private readonly router = inject(Router);
  // Instantiated here so it captures any ?ref= referral link parameter on first load.
  private readonly referral = inject(ReferralService);
  title = 'angular-ecommerce';

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
