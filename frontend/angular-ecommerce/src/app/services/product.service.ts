import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { environment } from '../../environments/environment';
import { Product } from '../common/product';
import { ProductCategory } from '../common/product-category';
import { ProductVariant } from '../common/product-variant';

export interface CatalogFilters {
  categoryId?: number;
  keyword?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  onSale?: boolean;
  minRating?: number;
  sort?: string;
  page?: number;
  size?: number;
}

/** Stable pagination envelope returned by the faceted /catalog/search endpoint. */
export interface CatalogPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/**
 * The catalog read surface: `searchCatalog()` (the faceted, tenant-scoped `/api/catalog/search`
 * endpoint backing `ProductQueryService`) is the one path for browsing/filtering products. It replaced
 * three older `*Paginate` methods that hit Spring Data REST's auto-generated `findBy*` search
 * resources directly (by category, on-sale, by keyword) — those took no tenant predicate at all, so
 * removing the last live caller and closing the endpoints server-side fixed a real cross-tenant catalog
 * leak (roadmap #21 gap; see `ProductRepository`).
 */
@Injectable({ providedIn: 'root' })
export class ProductService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private httpClient: HttpClient) {}

  /** Faceted catalog search (category, keyword, price, in-stock, on-sale, rating, sort). */
  searchCatalog(filters: CatalogFilters): Observable<CatalogPage> {
    let params = new HttpParams();
    const set = (k: string, v: unknown) => {
      if (v !== undefined && v !== null && v !== '') {
        params = params.set(k, String(v));
      }
    };
    set('categoryId', filters.categoryId);
    set('keyword', filters.keyword);
    set('minPrice', filters.minPrice);
    set('maxPrice', filters.maxPrice);
    set('inStock', filters.inStock);
    set('onSale', filters.onSale);
    set('minRating', filters.minRating);
    set('sort', filters.sort);
    set('page', filters.page ?? 0);
    set('size', filters.size ?? 12);
    return this.httpClient.get<CatalogPage>(`${this.baseUrl}/catalog/search`, { params });
  }

  getProduct(productId: number): Observable<Product> {
    return this.httpClient.get<Product>(`${this.baseUrl}/products/${productId}`);
  }

  /** Active, purchasable variants for a product (empty for single-SKU products). */
  getVariants(productId: number): Observable<ProductVariant[]> {
    return this.httpClient
      .get<ProductVariant[]>(`${this.baseUrl}/catalog/products/${productId}/variants`)
      .pipe(catchError(() => of([])));
  }

  /** Subscribe an email to be notified when a product (or variant) is back in stock. */
  notifyWhenInStock(productId: number, email: string, variantSku?: string | null): Observable<void> {
    return this.httpClient.post<void>(`${this.baseUrl}/stock-notifications`, {
      productId, email, variantSku: variantSku ?? null,
    });
  }

  /** Fetches several products by id (for the wishlist); skips any that fail to load. */
  getProductsByIds(ids: number[]): Observable<Product[]> {
    if (ids.length === 0) {
      return of([]);
    }
    return forkJoin(
      ids.map(id => this.getProduct(id).pipe(catchError(() => of(null)))),
    ).pipe(map(list => list.filter((p): p is Product => p !== null)));
  }

  /**
   * "You might also like" — other products in the same category as the given product.
   * Resolves the product's category via its Spring Data REST association link, then pulls a page of
   * that category through the tenant-scoped faceted search (previously used the raw, unscoped
   * `/api/products/search/findByCategoryId` directly — the last live caller of that legacy path;
   * closed as part of roadmap #21's tenant-scoping audit, see `ProductRepository`).
   */
  getRelatedProducts(productId: number, limit = 4): Observable<Product[]> {
    return this.httpClient
      .get<ProductCategory>(`${this.baseUrl}/products/${productId}/category`)
      .pipe(
        switchMap(category =>
          this.searchCatalog({ categoryId: category.id, size: limit + 1 }).pipe(
            map(response => response.content
              .filter(product => product.id !== productId)
              .slice(0, limit)),
          ),
        ),
      );
  }

  getProductCategories(): Observable<ProductCategory[]> {
    return this.httpClient
      .get<GetResponseProductCategory>(`${this.baseUrl}/product-category`)
      .pipe(map(response => response._embedded.productCategory));
  }
}

interface GetResponseProductCategory {
  _embedded: {
    productCategory: ProductCategory[];
  };
}
