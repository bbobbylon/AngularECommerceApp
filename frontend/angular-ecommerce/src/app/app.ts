import { Component, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ProductCategoryMenuComponent } from './components/product-category-menu/product-category-menu';
import { SearchComponent } from './components/search/search';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, SearchComponent, ProductCategoryMenuComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('angular-ecommerce');
}
