import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CartItem } from '../../common/cart-item';
import { Product } from '../../common/product';
import { CartService } from '../../services/cart.service';
import { FavoritesService } from '../../services/favorites.service';
import { ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-product-details',
  imports: [CommonModule, RouterLink],
  templateUrl: './product-details.html',
})
export class ProductDetails implements OnInit {

  product?: Product;
  quantity = 1;
  relatedProducts: Product[] = [];

  protected favorites = inject(FavoritesService);
  private toast = inject(ToastService);

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private route: ActivatedRoute,
  ) {}

  addToCart(): void {
    if (this.product) {
      this.cartService.addToCart(new CartItem(this.product), this.quantity);
      this.toast.success(`${this.quantity} × ${this.product.name} added to cart`);
      this.quantity = 1;
    }
  }

  addProductToCart(product: Product): void {
    this.cartService.addToCart(new CartItem(product));
    this.toast.success(`${product.name} added to cart`);
  }

  increaseQuantity(): void {
    if (this.product && this.quantity < this.product.unitsInStock) {
      this.quantity++;
    }
  }

  decreaseQuantity(): void {
    if (this.quantity > 1) {
      this.quantity--;
    }
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(() => this.handleProductDetails());
  }

  private handleProductDetails(): void {
    const productId = Number(this.route.snapshot.paramMap.get('id'));
    this.quantity = 1;
    this.productService.getProduct(productId).subscribe(data => (this.product = data));

    this.relatedProducts = [];
    this.productService.getRelatedProducts(productId).subscribe({
      next: data => (this.relatedProducts = data),
      error: () => (this.relatedProducts = []),
    });
  }
}
