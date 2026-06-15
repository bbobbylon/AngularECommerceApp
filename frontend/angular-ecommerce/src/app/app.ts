import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

import { CartStatus } from './components/cart-status/cart-status';
import { LoginStatus } from './components/login-status/login-status';
import { ProductCategoryMenu } from './components/product-category-menu/product-category-menu';
import { Search } from './components/search/search';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, Search, ProductCategoryMenu, CartStatus, LoginStatus],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  title = 'angular-ecommerce';
}
