import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Product } from '../common/product';
import { ProductCategory } from '../common/product-category';
import { Review } from './review.service';

export interface AdminStats {
  totalProducts: number;
  activeProducts: number;
  lowStockProducts: number;
  productsOnSale: number;
  totalOrders: number;
  totalRevenue: number;
  totalCustomers: number;
  newsletterSubscribers: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ComponentStatus {
  name: string;
  ready: boolean;
  detail: string;
}

export interface SystemHealth {
  status: string;
  version: string;
  profile: string;
  uptimeSeconds: number;
  components: ComponentStatus[];
}

export interface AdminOrderView {
  id: number;
  orderTrackingNumber: string;
  status: string;
  totalQuantity: number;
  totalPrice: number;
  dateCreated: string;
  customerName: string;
  customerEmail: string;
}

/** A product as returned by the admin endpoints — includes the embedded category object. */
export interface AdminProduct extends Product {
  category?: { id: number; categoryName: string };
}

/** Variant create/update shape — mirrors the backend AdminVariantRequest (id null for new ones). */
export interface AdminVariant {
  id?: number | null;
  sku: string;
  color?: string | null;
  size?: string | null;
  unitPrice?: number | null;
  unitsInStock: number;
  imageUrl?: string | null;
  sortOrder: number;
  active: boolean;
}

export interface AdminProductPayload {
  sku: string;
  name: string;
  description: string;
  unitPrice: number;
  originalPrice?: number | null;
  imageUrl?: string;
  additionalImages?: string[];
  active: boolean;
  unitsInStock: number;
  categoryId: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {

  private readonly baseUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.baseUrl}/stats`);
  }

  getSystemHealth(): Observable<SystemHealth> {
    return this.http.get<SystemHealth>(`${this.baseUrl}/system`);
  }

  getProducts(page: number, size: number): Observable<PageResponse<AdminProduct>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminProduct>>(`${this.baseUrl}/products`, { params });
  }

  getProduct(id: number): Observable<AdminProduct> {
    return this.http.get<AdminProduct>(`${this.baseUrl}/products/${id}`);
  }

  createProduct(payload: AdminProductPayload): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/products`, payload);
  }

  updateProduct(id: number, payload: AdminProductPayload): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/products/${id}`, payload);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/products/${id}`);
  }

  // ----- variants (SKU-level inventory) -----

  getVariants(productId: number): Observable<AdminVariant[]> {
    return this.http.get<AdminVariant[]>(`${this.baseUrl}/products/${productId}/variants`);
  }

  /** Replaces a product's full variant set (upsert by id, delete omitted). */
  replaceVariants(productId: number, variants: AdminVariant[]): Observable<AdminVariant[]> {
    return this.http.put<AdminVariant[]>(`${this.baseUrl}/products/${productId}/variants`, variants);
  }

  getOrders(page: number, size: number): Observable<PageResponse<AdminOrderView>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AdminOrderView>>(`${this.baseUrl}/orders`, { params });
  }

  updateOrderStatus(id: number, status: string): Observable<AdminOrderView> {
    const params = new HttpParams().set('status', status);
    return this.http.put<AdminOrderView>(`${this.baseUrl}/orders/${id}/status`, null, { params });
  }

  getCategories(): Observable<ProductCategory[]> {
    return this.http.get<ProductCategory[]>(`${this.baseUrl}/categories`);
  }

  createCategory(name: string): Observable<ProductCategory> {
    return this.http.post<ProductCategory>(`${this.baseUrl}/categories`, { name });
  }

  getReviews(page: number, size: number): Observable<PageResponse<Review>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<Review>>(`${this.baseUrl}/reviews`, { params });
  }

  deleteReview(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/reviews/${id}`);
  }

  getCoupons(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(`${this.baseUrl}/coupons`);
  }

  createCoupon(payload: CouponPayload): Observable<Coupon> {
    return this.http.post<Coupon>(`${this.baseUrl}/coupons`, payload);
  }

  deleteCoupon(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/coupons/${id}`);
  }
}

export interface Coupon {
  id: number;
  code: string;
  description?: string;
  percentOff?: number | null;
  amountOff?: number | null;
  minSpend?: number | null;
  active: boolean;
  expiresAt?: string | null;
}

export interface CouponPayload {
  code: string;
  description?: string;
  percentOff?: number | null;
  amountOff?: number | null;
  minSpend?: number | null;
  active: boolean;
  expiresAt?: string | null;
}
