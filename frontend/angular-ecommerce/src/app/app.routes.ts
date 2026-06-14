import { Routes } from '@angular/router';
import { ProductDetailsComponent } from './components/product-details/product-details';
import { ProductListComponent } from './components/product-list/product-list';

export const routes: Routes = [
  { path: 'products/:id', component: ProductDetailsComponent },
  { path: 'search/:keyword', component: ProductListComponent },
  { path: 'category/:id', component: ProductListComponent },
  { path: 'category', component: ProductListComponent },
  { path: 'products', component: ProductListComponent },
  { path: '', redirectTo: '/products', pathMatch: 'full' },
  { path: '**', redirectTo: '/products', pathMatch: 'full' }
];
