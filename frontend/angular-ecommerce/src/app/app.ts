import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

import { ProductCategoryMenu } from './components/product-category-menu/product-category-menu';
import { Search } from './components/search/search';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, Search, ProductCategoryMenu],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  title = 'angular-ecommerce';
}
