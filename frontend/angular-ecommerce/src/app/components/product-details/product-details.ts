import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CartItem } from '../../common/cart-item';
import { Product, discountPercent, isOnSale } from '../../common/product';
import { CartService } from '../../services/cart.service';
import { FavoritesService } from '../../services/favorites.service';
import { ProductService } from '../../services/product.service';
import { Review, ReviewService, ReviewSummary } from '../../services/review.service';
import { ToastService } from '../../services/toast.service';
import { StarRating } from '../star-rating/star-rating';

@Component({
  selector: 'app-product-details',
  imports: [CommonModule, FormsModule, RouterLink, StarRating],
  templateUrl: './product-details.html',
})
export class ProductDetails implements OnInit {

  product?: Product;
  quantity = 1;
  relatedProducts: Product[] = [];

  // reviews
  readonly reviews = signal<Review[]>([]);
  readonly reviewSummary = signal<ReviewSummary | null>(null);
  readonly submittingReview = signal(false);
  reviewTotal = 0;
  reviewPageSize = 5;
  reviewForm = { authorName: '', rating: 0, comment: '' };
  hoverRating = 0;

  // sale-pricing helpers exposed to the template
  protected readonly isOnSale = isOnSale;
  protected readonly discountPercent = discountPercent;

  protected favorites = inject(FavoritesService);
  private toast = inject(ToastService);
  private reviewService = inject(ReviewService);

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

    this.reviewForm = { authorName: '', rating: 0, comment: '' };
    this.loadReviews(productId);
    this.reviewService.summary(productId).subscribe({
      next: summary => this.reviewSummary.set(summary),
      error: () => this.reviewSummary.set(null),
    });
  }

  private loadReviews(productId: number): void {
    this.reviewService.list(productId, 0, this.reviewPageSize).subscribe({
      next: res => {
        this.reviews.set(res.content);
        this.reviewTotal = res.totalElements;
      },
      error: () => this.reviews.set([]),
    });
  }

  setRating(value: number): void {
    this.reviewForm.rating = value;
  }

  submitReview(): void {
    if (!this.product) {
      return;
    }
    if (!this.reviewForm.authorName.trim() || this.reviewForm.rating < 1) {
      this.toast.error('Please add your name and a star rating.');
      return;
    }
    this.submittingReview.set(true);
    this.reviewService
      .create({
        productId: this.product.id,
        authorName: this.reviewForm.authorName.trim(),
        rating: this.reviewForm.rating,
        comment: this.reviewForm.comment.trim(),
      })
      .subscribe({
        next: () => {
          this.toast.success('Thanks for your review!');
          this.reviewForm = { authorName: '', rating: 0, comment: '' };
          this.submittingReview.set(false);
          // refresh list, summary, and the product's denormalized aggregate
          this.loadReviews(this.product!.id);
          this.reviewService.summary(this.product!.id).subscribe(s => this.reviewSummary.set(s));
          this.productService.getProduct(this.product!.id).subscribe(p => (this.product = p));
        },
        error: () => {
          this.submittingReview.set(false);
          this.toast.error('Could not submit your review. Please try again.');
        },
      });
  }
}
