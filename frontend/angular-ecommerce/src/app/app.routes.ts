import { Routes } from '@angular/router';
import { OktaCallbackComponent } from '@okta/okta-angular';

import { devOrAuthGuard } from './auth/dev-auth.guard';

// Local components are lazy-loaded so each route ships as its own chunk, keeping the
// initial bundle lean (checkout's Stripe/forms and the catalog's pagination split out).
export const routes: Routes = [
  { path: 'login/callback', component: OktaCallbackComponent },
  {
    path: 'members/orders',
    loadComponent: () => import('./components/order-history/order-history').then(m => m.OrderHistory),
    canActivate: [devOrAuthGuard],
  },
  {
    path: 'account',
    loadComponent: () => import('./components/account-settings/account-settings').then(m => m.AccountSettings),
    canActivate: [devOrAuthGuard],
  },
  {
    path: 'about',
    loadComponent: () => import('./components/about/about').then(m => m.About),
  },
  {
    // Reuses ProductList in "sale mode" (fetches on-sale products) — same grid, cart & favorites.
    path: 'sale',
    loadComponent: () => import('./components/product-list/product-list').then(m => m.ProductList),
    data: { mode: 'sale' },
  },
  {
    path: 'order-confirmation/:trackingNumber',
    loadComponent: () => import('./components/order-confirmation/order-confirmation').then(m => m.OrderConfirmation),
  },
  {
    path: 'checkout',
    loadComponent: () => import('./components/checkout/checkout').then(m => m.Checkout),
  },
  {
    path: 'cart-details',
    loadComponent: () => import('./components/cart-details/cart-details').then(m => m.CartDetails),
  },
  {
    path: 'favorites',
    loadComponent: () => import('./components/favorites/favorites').then(m => m.Favorites),
  },
  {
    path: 'products/:id',
    loadComponent: () => import('./components/product-details/product-details').then(m => m.ProductDetails),
  },
  {
    path: 'search/:keyword',
    loadComponent: () => import('./components/product-list/product-list').then(m => m.ProductList),
  },
  {
    path: 'category/:id',
    loadComponent: () => import('./components/product-list/product-list').then(m => m.ProductList),
  },
  {
    path: 'category',
    loadComponent: () => import('./components/product-list/product-list').then(m => m.ProductList),
  },
  {
    path: 'products',
    loadComponent: () => import('./components/product-list/product-list').then(m => m.ProductList),
  },
  { path: '', redirectTo: '/products', pathMatch: 'full' },
  {
    path: '**',
    loadComponent: () => import('./components/not-found/not-found').then(m => m.NotFound),
  },
];
