import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

import { CartItem } from '../../common/cart-item';
import { Product, discountPercent, isOnSale } from '../../common/product';
import { CartService } from '../../services/cart.service';
import { FavoritesService } from '../../services/favorites.service';
import { GetResponseProducts, ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';
import { NewsletterSignup } from '../newsletter-signup/newsletter-signup';

@Component({
  selector: 'app-product-list',
  imports: [CommonModule, RouterLink, NgbPaginationModule, NewsletterSignup],
  templateUrl: './product-list.html',
})
export class ProductList implements OnInit {

  products: Product[] = [];
  isLoading = false;

  currentCategoryId = 1;
  previousCategoryId = 1;
  searchMode = false;
  saleMode = false;
  previousKeyword = '';

  // sale-pricing helpers exposed to the template
  protected readonly isOnSale = isOnSale;
  protected readonly discountPercent = discountPercent;

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
    { label: 'Price: Low to High', value: 'unitPrice,asc' },
    { label: 'Price: High to Low', value: 'unitPrice,desc' },
    { label: 'Name: A–Z', value: 'name,asc' },
  ];

  protected favorites = inject(FavoritesService);
  private toast = inject(ToastService);

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
    this.route.paramMap.subscribe(() => this.listProducts());
  }

  listProducts(): void {
    this.isLoading = true;
    this.searchMode = this.route.snapshot.paramMap.has('keyword');
    if (this.saleMode) {
      this.handleSaleProducts();
    } else if (this.searchMode) {
      this.handleSearchProducts();
    } else {
      this.handleListProducts();
    }
  }

  private handleSaleProducts(): void {
    this.productService
      .getProductsOnSalePaginate(this.pageNumber - 1, this.pageSize, this.sortBy)
      .subscribe({
        next: data => this.processResult(data),
        error: () => {
          this.products = [];
          this.isLoading = false;
        },
      });
  }

  private handleListProducts(): void {
    const hasCategoryId = this.route.snapshot.paramMap.has('id');
    this.currentCategoryId = hasCategoryId
      ? Number(this.route.snapshot.paramMap.get('id'))
      : 1;

    // reset to first page whenever the category changes
    if (this.previousCategoryId !== this.currentCategoryId) {
      this.pageNumber = 1;
    }
    this.previousCategoryId = this.currentCategoryId;

    this.productService
      .getProductListPaginate(this.pageNumber - 1, this.pageSize, this.currentCategoryId, this.sortBy)
      .subscribe({
        next: data => this.processResult(data),
        error: () => {
          this.products = [];
          this.isLoading = false;
        },
      });
  }

  private handleSearchProducts(): void {
    const keyword = this.route.snapshot.paramMap.get('keyword') ?? '';

    // reset to first page whenever the keyword changes
    if (this.previousKeyword !== keyword) {
      this.pageNumber = 1;
    }
    this.previousKeyword = keyword;

    this.productService
      .searchProductsPaginate(this.pageNumber - 1, this.pageSize, keyword, this.sortBy)
      .subscribe({
        next: data => this.processResult(data),
        error: () => {
          this.products = [];
          this.isLoading = false;
        },
      });
  }

  private processResult(data: GetResponseProducts): void {
    this.products = data._embedded.products;
    this.pageNumber = data.page.number + 1;
    this.pageSize = data.page.size;
    this.totalElements = data.page.totalElements;
    this.isLoading = false;
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
