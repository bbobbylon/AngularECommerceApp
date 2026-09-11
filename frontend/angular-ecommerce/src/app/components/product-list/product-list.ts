import { CommonModule } from '@angular/common';
import { MoneyPipe } from '../../common/money.pipe';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

import { CartItem } from '../../common/cart-item';
import { ProductCategory } from '../../common/product-category';
import { Product, discountPercent, isLowStock, isOnSale } from '../../common/product';
import { CartService } from '../../services/cart.service';
import { FavoritesService } from '../../services/favorites.service';
import { CatalogFilters, CatalogPage, ProductService } from '../../services/product.service';
import { SeoService } from '../../services/seo.service';
import { ToastService } from '../../services/toast.service';
import { NewsletterSignup } from '../newsletter-signup/newsletter-signup';
import { RecentlyViewed } from '../recently-viewed/recently-viewed';
import { StarRating } from '../star-rating/star-rating';

@Component({
  selector: 'app-product-list',
  imports: [CommonModule, MoneyPipe, FormsModule, RouterLink, NgbPaginationModule, NewsletterSignup, RecentlyViewed, StarRating],
  templateUrl: './product-list.html',
})
export class ProductList implements OnInit {

  products: Product[] = [];
  isLoading = false;

  currentCategoryId = 1;
  searchMode = false;
  saleMode = false;
  private previousScopeKey = '';

  // facet filters
  filterMinPrice: number | null = null;
  filterMaxPrice: number | null = null;
  filterInStock = false;
  filterOnSale = false;
  filterMinRating = 0;
  showFilters = false;

  // sale-pricing + stock helpers exposed to the template
  protected readonly isOnSale = isOnSale;
  protected readonly discountPercent = discountPercent;
  protected readonly isLowStock = isLowStock;

  /** The landing page: the plain product grid with no category, search, or sale filter. */
  get isHome(): boolean {
    return !this.searchMode && !this.saleMode && !this.route.snapshot.paramMap.has('id');
  }

  // pagination (NgbPagination is 1-based; Spring Data REST is 0-based)
  pageNumber = 1;
  pageSize = 12;
  totalElements = 0;

  readonly pageSizeOptions = [6, 12, 24, 48];

  sortBy = '';
  readonly sortOptions = [
    { label: 'Featured', value: '' },
    { label: 'Top rated', value: 'averageRating,desc' },
    { label: 'Price: Low to High', value: 'unitPrice,asc' },
    { label: 'Price: High to Low', value: 'unitPrice,desc' },
    { label: 'Name: A–Z', value: 'name,asc' },
  ];

  get hasActiveFilters(): boolean {
    return this.filterMinPrice != null || this.filterMaxPrice != null
      || this.filterInStock || this.filterOnSale || this.filterMinRating > 0;
  }

  protected favorites = inject(FavoritesService);
  private toast = inject(ToastService);
  private seo = inject(SeoService);
  private categories: ProductCategory[] = [];

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private route: ActivatedRoute,
  ) {}

  addToCart(product: Product): void {
    this.cartService.addToCart(new CartItem(product));
    this.toast.success(`${product.name} added to cart`);
  }

  toggleFavorite(product: Product): void {
    this.favorites.toggle(product.id);
  }

  ngOnInit(): void {
    this.saleMode = this.route.snapshot.data['mode'] === 'sale';
    this.productService.getProductCategories().subscribe(data => (this.categories = data));
    this.route.paramMap.subscribe(() => this.listProducts());
  }

  listProducts(): void {
    this.isLoading = true;
    this.searchMode = this.route.snapshot.paramMap.has('keyword');
    const hasCategoryId = this.route.snapshot.paramMap.has('id');
    const keyword = this.route.snapshot.paramMap.get('keyword') ?? '';
    this.currentCategoryId = hasCategoryId ? Number(this.route.snapshot.paramMap.get('id')) : 1;

    // reset to first page whenever the scope (home/category/search/sale) changes
    const scopeKey = this.saleMode ? 'sale'
      : this.searchMode ? `search:${keyword}`
      : hasCategoryId ? `category:${this.currentCategoryId}`
      : 'home';
    if (this.previousScopeKey !== scopeKey) {
      this.pageNumber = 1;
    }
    this.previousScopeKey = scopeKey;

    const filters: CatalogFilters = {
      page: this.pageNumber - 1,
      size: this.pageSize,
      sort: this.sortBy,
      minPrice: this.filterMinPrice ?? undefined,
      maxPrice: this.filterMaxPrice ?? undefined,
      inStock: this.filterInStock || undefined,
      onSale: this.saleMode ? true : (this.filterOnSale || undefined),
      minRating: this.filterMinRating || undefined,
    };
    if (this.searchMode) {
      filters.keyword = keyword;
    } else if (hasCategoryId) {
      filters.categoryId = this.currentCategoryId;
    }

    this.productService.searchCatalog(filters).subscribe({
      next: data => this.processResult(data),
      error: () => {
        this.products = [];
        this.totalElements = 0;
        this.isLoading = false;
      },
    });
  }

  private processResult(data: CatalogPage): void {
    this.products = data.content;
    this.pageNumber = data.number + 1;
    this.pageSize = data.size;
    this.totalElements = data.totalElements;
    this.isLoading = false;
    this.updateSeo();
  }

  /** Title/meta/OG for the current scope (home/category/search/sale — roadmap #11). */
  private updateSeo(): void {
    if (this.saleMode) {
      this.seo.update({
        title: 'Sale',
        description: 'Shop today\'s sale items and limited-time discounts at Luv2Shop.',
      });
      return;
    }
    if (this.searchMode) {
      const keyword = this.route.snapshot.paramMap.get('keyword') ?? '';
      this.seo.update({
        title: `Search results for "${keyword}"`,
        description: `${this.totalElements} result(s) for "${keyword}" at Luv2Shop.`,
      });
      return;
    }
    if (this.isHome) {
      this.seo.update({
        title: 'Shop All Products',
        description: 'Browse the full Luv2Shop catalog — books, mugs, mouse pads, luggage, and more.',
      });
      return;
    }
    const categoryName = this.categories.find(c => c.id === this.currentCategoryId)?.categoryName ?? 'Category';
    this.seo.update({
      title: categoryName,
      description: `Shop ${categoryName} at Luv2Shop.`,
    });
  }

  applyFilters(): void {
    this.pageNumber = 1;
    this.listProducts();
  }

  clearFilters(): void {
    this.filterMinPrice = null;
    this.filterMaxPrice = null;
    this.filterInStock = false;
    this.filterOnSale = false;
    this.filterMinRating = 0;
    this.pageNumber = 1;
    this.listProducts();
  }

  updatePageSize(value: string): void {
    this.pageSize = Number(value);
    this.pageNumber = 1;
    this.listProducts();
  }

  updateSort(value: string): void {
    this.sortBy = value;
    this.pageNumber = 1;
    this.listProducts();
  }
}
