import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CartItem } from '../../common/cart-item';
import { Product } from '../../common/product';
import { CartService } from '../../services/cart.service';
import { FavoritesService } from '../../services/favorites.service';
import { ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-favorites',
  imports: [CommonModule, RouterLink],
  templateUrl: './favorites.html',
})
export class Favorites implements OnInit {

  products: Product[] = [];
  isLoading = true;

  private favorites = inject(FavoritesService);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.productService.getProductsByIds(this.favorites.ids()).subscribe({
      next: data => {
        this.products = data;
        this.isLoading = false;
      },
      error: () => {
        this.products = [];
        this.isLoading = false;
      },
    });
  }

  removeFavorite(product: Product): void {
    this.favorites.remove(product.id);
    this.products = this.products.filter(p => p.id !== product.id);
    this.toast.info(`${product.name} removed from favorites`);
  }

  addToCart(product: Product): void {
    this.cartService.addToCart(new CartItem(product));
    this.toast.success(`${product.name} added to cart`);
  }
}
