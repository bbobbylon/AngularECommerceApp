import { CommonModule } from '@angular/common';
import { MoneyPipe } from '../../common/money.pipe';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CartItem } from '../../common/cart-item';
import { Product } from '../../common/product';
import { CartService } from '../../services/cart.service';
import { FavoritesService } from '../../services/favorites.service';
import { ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';
import { WishlistService } from '../../services/wishlist.service';

@Component({
  selector: 'app-favorites',
  imports: [CommonModule, MoneyPipe, FormsModule, RouterLink],
  templateUrl: './favorites.html',
})
export class Favorites implements OnInit {

  products: Product[] = [];
  isLoading = true;

  syncEmail = '';
  readonly syncing = signal(false);

  private favorites = inject(FavoritesService);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private toast = inject(ToastService);
  private wishlist = inject(WishlistService);

  ngOnInit(): void {
    this.reloadProducts();
  }

  private reloadProducts(): void {
    this.isLoading = true;
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

  syncWithAccount(): void {
    const email = this.syncEmail.trim();
    if (!email || !email.includes('@')) {
      this.toast.error('Enter a valid email address.');
      return;
    }
    this.syncing.set(true);
    this.wishlist.sync(email, this.favorites.ids()).subscribe({
      next: ids => {
        this.favorites.setAll(ids);
        this.toast.success('Wishlist synced to your account.');
        this.syncing.set(false);
        this.reloadProducts();
      },
      error: () => {
        this.syncing.set(false);
        this.toast.error('Could not sync your wishlist.');
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
