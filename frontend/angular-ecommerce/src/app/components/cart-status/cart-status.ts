import { CommonModule } from '@angular/common';
import { MoneyPipe } from '../../common/money.pipe';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-cart-status',
  imports: [CommonModule, MoneyPipe, RouterLink],
  templateUrl: './cart-status.html',
})
export class CartStatus implements OnInit {

  totalPrice = 0;
  totalQuantity = 0;
  bump = false;

  private initialised = false;

  constructor(private cartService: CartService) {}

  ngOnInit(): void {
    this.cartService.totalPrice.subscribe(data => (this.totalPrice = data));
    this.cartService.totalQuantity.subscribe(data => {
      // pop the badge whenever the quantity grows (but not on initial load / restore)
      if (this.initialised && data > this.totalQuantity) {
        this.triggerBump();
      }
      this.totalQuantity = data;
      this.initialised = true;
    });
  }

  private triggerBump(): void {
    this.bump = false;
    // restart the CSS animation on the next frame so repeat adds re-trigger it
    requestAnimationFrame(() => (this.bump = true));
  }
}
