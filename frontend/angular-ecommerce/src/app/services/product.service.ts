import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Product } from '../common/product';
import { ProductCategory } from '../common/product-category';

@Injectable({ providedIn: 'root' })
export class ProductService {

  private readonly baseUrl = environment.apiUrl;
  private readonly productsUrl = `${this.baseUrl}/products`;
  private readonly categoryUrl = `${this.baseUrl}/product-category`;

  constructor(private httpClient: HttpClient) {}

  getProductListPaginate(page: number, pageSize: number, categoryId: number): Observable<GetResponseProducts> {
    const url = `${this.productsUrl}/search/findByCategoryId`
      + `?id=${categoryId}&page=${page}&size=${pageSize}`;
    return this.httpClient.get<GetResponseProducts>(url);
  }

  searchProductsPaginate(page: number, pageSize: number, keyword: string): Observable<GetResponseProducts> {
    const url = `${this.productsUrl}/search/findByNameContaining`
      + `?name=${keyword}&page=${page}&size=${pageSize}`;
    return this.httpClient.get<GetResponseProducts>(url);
  }

  getProduct(productId: number): Observable<Product> {
    const url = `${this.productsUrl}/${productId}`;
    return this.httpClient.get<Product>(url);
  }

  getProductCategories(): Observable<ProductCategory[]> {
    return this.httpClient.get<GetResponseProductCategory>(this.categoryUrl).pipe(
      map(response => response._embedded.productCategory)
    );
  }
}

interface GetResponseProducts {
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
