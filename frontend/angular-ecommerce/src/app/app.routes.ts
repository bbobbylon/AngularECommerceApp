import { Routes } from '@angular/router';
import { OktaCallbackComponent } from '@okta/okta-angular';

import { devOrAuthGuard } from './auth/dev-auth.guard';
import { CartDetails } from './components/cart-details/cart-details';
import { Checkout } from './components/checkout/checkout';
import { NotFound } from './components/not-found/not-found';
import { OrderConfirmation } from './components/order-confirmation/order-confirmation';
import { OrderHistory } from './components/order-history/order-history';
import { ProductDetails } from './components/product-details/product-details';
import { ProductList } from './components/product-list/product-list';

export const routes: Routes = [
  { path: 'login/callback', component: OktaCallbackComponent },
  { path: 'members/orders', component: OrderHistory, canActivate: [devOrAuthGuard] },
  { path: 'order-confirmation/:trackingNumber', component: OrderConfirmation },
  { path: 'checkout', component: Checkout },
  { path: 'cart-details', component: CartDetails },
  { path: 'products/:id', component: ProductDetails },
  { path: 'search/:keyword', component: ProductList },
  { path: 'category/:id', component: ProductList },
  { path: 'category', component: ProductList },
  { path: 'products', component: ProductList },
  { path: '', redirectTo: '/products', pathMatch: 'full' },
  { path: '**', component: NotFound },
];
