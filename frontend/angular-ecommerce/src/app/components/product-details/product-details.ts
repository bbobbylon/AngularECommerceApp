import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CartItem } from '../../common/cart-item';
import { Product } from '../../common/product';
import { CartService } from '../../services/cart.service';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-details',
  imports: [CommonModule, RouterLink],
  templateUrl: './product-details.html',
})
export class ProductDetails implements OnInit {

  product?: Product;

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private route: ActivatedRoute,
  ) {}

  addToCart(): void {
    if (this.product) {
      this.cartService.addToCart(new CartItem(this.product));
    }
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(() => this.handleProductDetails());
  }

  private handleProductDetails(): void {
    const productId = Number(this.route.snapshot.paramMap.get('id'));
    this.productService.getProduct(productId).subscribe(data => (this.product = data));
  }
}
