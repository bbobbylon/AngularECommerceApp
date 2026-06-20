import { CommonModule } from '@angular/common';
import { MoneyPipe } from '../../common/money.pipe';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CartItem } from '../../common/cart-item';
import { Product, LOW_STOCK_THRESHOLD, discountPercent, galleryImages, isLowStock, isOnSale } from '../../common/product';
import { ProductVariant } from '../../common/product-variant';
import { CartService } from '../../services/cart.service';
import { FavoritesService } from '../../services/favorites.service';
import { ProductService } from '../../services/product.service';
import { RecentlyViewedService } from '../../services/recently-viewed.service';
import { Review, ReviewService, ReviewSummary } from '../../services/review.service';
import { ToastService } from '../../services/toast.service';
import { RecentlyViewed } from '../recently-viewed/recently-viewed';
import { StarRating } from '../star-rating/star-rating';

@Component({
  selector: 'app-product-details',
  imports: [CommonModule, MoneyPipe, FormsModule, RouterLink, StarRating, RecentlyViewed],
  templateUrl: './product-details.html',
})
export class ProductDetails implements OnInit {

  product?: Product;
  quantity = 1;
  relatedProducts: Product[] = [];

  // variants (SKU-level inventory). Empty for single-SKU products.
  readonly variants = signal<ProductVariant[]>([]);
  readonly selectedVariant = signal<ProductVariant | null>(null);

  // back-in-stock notify-me
  notifyEmail = '';
  readonly notifyRequested = signal(false);
  readonly notifying = signal(false);

  /** The large image currently shown in the gallery (defaults to the main image). */
  readonly selectedImage = signal('');

  // reviews
  readonly reviews = signal<Review[]>([]);
  readonly reviewSummary = signal<ReviewSummary | null>(null);
  readonly submittingReview = signal(false);
  reviewTotal = 0;
  reviewPageSize = 5;
  reviewForm = { authorName: '', rating: 0, comment: '' };
  hoverRating = 0;

  // sale-pricing + stock + gallery helpers exposed to the template
  protected readonly isOnSale = isOnSale;
  protected readonly discountPercent = discountPercent;
  protected readonly isLowStock = isLowStock;
  protected readonly galleryImages = galleryImages;

  protected favorites = inject(FavoritesService);
  private toast = inject(ToastService);
  private reviewService = inject(ReviewService);
  private recentlyViewed = inject(RecentlyViewedService);

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private route: ActivatedRoute,
  ) {}

  /** True when this product is sold by variant (color/size) rather than as a single SKU. */
  hasVariants(): boolean {
    return this.variants().length > 0;
  }

  /** Price for the current selection: the chosen variant's price, else the product's. */
  effectivePrice(): number {
    return this.selectedVariant()?.unitPrice ?? this.product?.unitPrice ?? 0;
  }

  /** Stock for the current selection (the chosen variant's, else the product's). */
  availableStock(): number {
    if (this.hasVariants()) {
      return this.selectedVariant()?.unitsInStock ?? 0;
    }
    return this.product?.unitsInStock ?? 0;
  }

  lowStock(): boolean {
    const s = this.availableStock();
    return s > 0 && s <= LOW_STOCK_THRESHOLD;
  }

  selectVariant(variant: ProductVariant): void {
    if (!variant.inStock) {
      return;
    }
    this.selectedVariant.set(variant);
    this.quantity = 1;
    if (variant.imageUrl) {
      this.selectedImage.set(variant.imageUrl);
    }
  }

  /** Subscribe to a back-in-stock email for the current product/variant. */
  notifyMe(): void {
    if (!this.product) {
      return;
    }
    const email = this.notifyEmail.trim();
    if (!email || !email.includes('@')) {
      this.toast.error('Please enter a valid email.');
      return;
    }
    this.notifying.set(true);
    const variantSku = this.hasVariants() ? this.selectedVariant()?.sku ?? null : null;
    this.productService.notifyWhenInStock(this.product.id, email, variantSku).subscribe({
      next: () => {
        this.notifyRequested.set(true);
        this.notifying.set(false);
        this.toast.success("We'll email you when it's back in stock.");
      },
      error: () => {
        this.notifying.set(false);
        this.toast.error('Could not sign you up. Please try again.');
      },
    });
  }

  addToCart(): void {
    if (!this.product) {
      return;
    }
    if (this.hasVariants() && !this.selectedVariant()) {
      this.toast.error('Please choose an option first.');
      return;
    }
    const variant = this.selectedVariant() ?? undefined;
    this.cartService.addToCart(new CartItem(this.product, variant), this.quantity);
    const label = variant ? `${this.product.name} (${variant.label})` : this.product.name;
    this.toast.success(`${this.quantity} × ${label} added to cart`);
    this.quantity = 1;
  }

  addProductToCart(product: Product): void {
    this.cartService.addToCart(new CartItem(product));
    this.toast.success(`${product.name} added to cart`);
  }

  increaseQuantity(): void {
    if (this.quantity < this.availableStock()) {
      this.quantity++;
    }
  }

  decreaseQuantity(): void {
    if (this.quantity > 1) {
      this.quantity--;
    }
  }

  selectImage(url: string): void {
    this.selectedImage.set(url);
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(() => this.handleProductDetails());
  }

  private handleProductDetails(): void {
    const productId = Number(this.route.snapshot.paramMap.get('id'));
    this.quantity = 1;
    this.selectedImage.set('');
    this.variants.set([]);
    this.selectedVariant.set(null);
    this.notifyRequested.set(false);
    this.notifyEmail = '';
    this.recentlyViewed.record(productId);
    this.productService.getProduct(productId).subscribe(data => {
      this.product = data;
      if (!this.selectedVariant()?.imageUrl) {
        this.selectedImage.set(data.imageUrl);
      }
    });

    this.productService.getVariants(productId).subscribe(variants => {
      this.variants.set(variants);
      const firstInStock = variants.find(v => v.inStock) ?? null;
      this.selectedVariant.set(firstInStock);
      if (firstInStock?.imageUrl) {
        this.selectedImage.set(firstInStock.imageUrl);
      }
    });

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
