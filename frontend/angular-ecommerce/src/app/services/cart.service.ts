import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { CartItem } from '../common/cart-item';

@Injectable({ providedIn: 'root' })
export class CartService {

  cartItems: CartItem[] = [];

  totalPrice = new BehaviorSubject<number>(0);
  totalQuantity = new BehaviorSubject<number>(0);

  private storage: Storage = sessionStorage;

  constructor() {
    const data = this.storage.getItem('cartItems');
    if (data) {
      this.cartItems = JSON.parse(data) as CartItem[];
      this.computeCartTotals();
    }
  }

  addToCart(theCartItem: CartItem): void {
    const existing = this.cartItems.find(item => item.id === theCartItem.id);
    if (existing) {
      existing.quantity++;
    } else {
      this.cartItems.push(theCartItem);
    }
    this.computeCartTotals();
  }

  decrementQuantity(theCartItem: CartItem): void {
    theCartItem.quantity--;
    if (theCartItem.quantity === 0) {
      this.remove(theCartItem);
    } else {
      this.computeCartTotals();
    }
  }

  remove(theCartItem: CartItem): void {
    this.cartItems = this.cartItems.filter(item => item.id !== theCartItem.id);
    this.computeCartTotals();
  }

  clear(): void {
    this.cartItems = [];
    this.computeCartTotals();
  }

  computeCartTotals(): void {
    let totalPriceValue = 0;
    let totalQuantityValue = 0;

    for (const item of this.cartItems) {
      totalPriceValue += item.quantity * item.unitPrice;
      totalQuantityValue += item.quantity;
    }

    this.totalPrice.next(totalPriceValue);
    this.totalQuantity.next(totalQuantityValue);
    this.persistCartItems();
  }

  private persistCartItems(): void {
    this.storage.setItem('cartItems', JSON.stringify(this.cartItems));
  }
}
