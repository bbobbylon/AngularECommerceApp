import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CartItem } from '../../common/cart-item';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-cart-details',
  imports: [CommonModule, RouterLink],
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
