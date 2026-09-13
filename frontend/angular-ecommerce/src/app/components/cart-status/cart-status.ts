import { CommonModule } from '@angular/common';
import { MoneyPipe } from '../../common/money.pipe';
import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CartService } from '../../services/cart.service';

/**
 * Header cart-icon widget (price/quantity badge) reading `CartService`'s totals. The "bump" animation
 * fires only when quantity grows after the initial load, so restoring a saved cart on page refresh
 * doesn't falsely animate.
 */
@Component({
  selector: 'app-cart-status',
  imports: [CommonModule, MoneyPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './cart-status.html',
})
export class CartStatus implements OnInit {
  totalPrice = 0;
  totalQuantity = 0;
  bump = false;

  private initialised = false;

  constructor(private cartService: CartService) {}

  ngOnInit(): void {
    this.cartService.totalPrice.subscribe((data) => (this.totalPrice = data));
    this.cartService.totalQuantity.subscribe((data) => {
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
