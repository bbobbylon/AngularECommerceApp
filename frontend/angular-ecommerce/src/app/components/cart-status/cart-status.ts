import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-cart-status',
  imports: [CommonModule, RouterLink],
  templateUrl: './cart-status.html',
})
export class CartStatus implements OnInit {

  totalPrice = 0;
  totalQuantity = 0;

  constructor(private cartService: CartService) {}

  ngOnInit(): void {
    this.cartService.totalPrice.subscribe(data => (this.totalPrice = data));
    this.cartService.totalQuantity.subscribe(data => (this.totalQuantity = data));
  }
}
