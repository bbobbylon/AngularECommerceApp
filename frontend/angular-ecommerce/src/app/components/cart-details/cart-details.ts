import { CommonModule } from '@angular/common';
import { MoneyPipe } from '../../common/money.pipe';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CartItem } from '../../common/cart-item';
import { CartService } from '../../services/cart.service';

/**
 * The full `/cart-details` page — line-item list with quantity +/- and remove, backed entirely by
 * `CartService`'s sessionStorage-held state. `cart-status.ts` is this same data shown as a small
 * header widget rather than a full page.
 */
@Component({
  selector: 'app-cart-details',
  imports: [CommonModule, MoneyPipe, RouterLink],
  templateUrl: './cart-details.html',
})
export class CartDetails implements OnInit {

  cartItems: CartItem[] = [];
  totalPrice = 0;
  totalQuantity = 0;

  constructor(private cartService: CartService) {}

  ngOnInit(): void {
    this.listCartDetails();
  }

  private listCartDetails(): void {
    this.cartItems = this.cartService.cartItems;
    this.cartService.totalPrice.subscribe(data => (this.totalPrice = data));
    this.cartService.totalQuantity.subscribe(data => (this.totalQuantity = data));
    this.cartService.computeCartTotals();
  }

  incrementQuantity(cartItem: CartItem): void {
    this.cartService.addToCart(cartItem);
  }

  decrementQuantity(cartItem: CartItem): void {
    this.cartService.decrementQuantity(cartItem);
  }

  remove(cartItem: CartItem): void {
    this.cartService.remove(cartItem);
  }
}
