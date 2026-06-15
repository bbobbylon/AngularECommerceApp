import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

import { BackToTop } from './components/back-to-top/back-to-top';
import { CartStatus } from './components/cart-status/cart-status';
import { LoginStatus } from './components/login-status/login-status';
import { ProductCategoryMenu } from './components/product-category-menu/product-category-menu';
import { Search } from './components/search/search';
import { Toast } from './components/toast/toast';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, Search, ProductCategoryMenu, CartStatus, LoginStatus, Toast, BackToTop],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly themeService = inject(ThemeService);
  title = 'angular-ecommerce';
}
