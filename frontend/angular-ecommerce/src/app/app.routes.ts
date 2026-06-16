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
    path: 'admin',
    loadComponent: () => import('./components/admin/admin-layout/admin-layout').then(m => m.AdminLayout),
    canActivate: [devOrAuthGuard],
    children: [
      { path: '', loadComponent: () => import('./components/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      { path: 'products', loadComponent: () => import('./components/admin/admin-products/admin-products').then(m => m.AdminProducts) },
      { path: 'products/new', loadComponent: () => import('./components/admin/admin-product-form/admin-product-form').then(m => m.AdminProductForm) },
      { path: 'products/:id/edit', loadComponent: () => import('./components/admin/admin-product-form/admin-product-form').then(m => m.AdminProductForm) },
      { path: 'orders', loadComponent: () => import('./components/admin/admin-orders/admin-orders').then(m => m.AdminOrders) },
      { path: 'reviews', loadComponent: () => import('./components/admin/admin-reviews/admin-reviews').then(m => m.AdminReviews) },
      { path: 'coupons', loadComponent: () => import('./components/admin/admin-coupons/admin-coupons').then(m => m.AdminCoupons) },
    ],
  },
  {
    path: 'about',
    loadComponent: () => import('./components/about/about').then(m => m.About),
  },
  {
    path: 'faq',
    loadComponent: () => import('./components/faq/faq').then(m => m.Faq),
  },
  {
    path: 'contact',
    loadComponent: () => import('./components/contact/contact').then(m => m.Contact),
  },
  {
    path: 'shipping-returns',
    loadComponent: () => import('./components/info-page/info-page').then(m => m.InfoPage),
    data: { page: 'shipping' },
  },
  {
    path: 'privacy',
    loadComponent: () => import('./components/info-page/info-page').then(m => m.InfoPage),
    data: { page: 'privacy' },
  },
  {
    path: 'terms',
    loadComponent: () => import('./components/info-page/info-page').then(m => m.InfoPage),
    data: { page: 'terms' },
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
