import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, switchMap } from 'rxjs';

import { environment } from '../../environments/environment';
import { Product } from '../common/product';
import { ProductCategory } from '../common/product-category';

@Injectable({ providedIn: 'root' })
export class ProductService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private httpClient: HttpClient) {}

  getProductListPaginate(page: number, pageSize: number, categoryId: number): Observable<GetResponseProducts> {
    const url =
      `${this.baseUrl}/products/search/findByCategoryId` +
      `?id=${categoryId}&page=${page}&size=${pageSize}`;
    return this.httpClient.get<GetResponseProducts>(url);
  }

  searchProductsPaginate(page: number, pageSize: number, keyword: string): Observable<GetResponseProducts> {
    const url =
      `${this.baseUrl}/products/search/findByNameContaining` +
      `?name=${encodeURIComponent(keyword)}&page=${page}&size=${pageSize}`;
    return this.httpClient.get<GetResponseProducts>(url);
  }

  getProduct(productId: number): Observable<Product> {
    return this.httpClient.get<Product>(`${this.baseUrl}/products/${productId}`);
  }

  /**
   * "You might also like" — other products in the same category as the given product.
   * Resolves the product's category via its Spring Data REST association link, then pulls
   * a page of that category and drops the product itself.
   */
  getRelatedProducts(productId: number, limit = 4): Observable<Product[]> {
    return this.httpClient
      .get<ProductCategory>(`${this.baseUrl}/products/${productId}/category`)
      .pipe(
        switchMap(category =>
          this.getProductListPaginate(0, limit + 1, category.id).pipe(
            map(response => response._embedded.products
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

/** Spring Data REST paginated response for products. */
export interface GetResponseProducts {
  _embedded: {
    products: Product[];
  };
  page: {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
  };
}

interface GetResponseProductCategory {
  _embedded: {
    productCategory: ProductCategory[];
  };
}
