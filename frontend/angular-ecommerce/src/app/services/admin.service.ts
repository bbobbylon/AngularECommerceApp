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

  // ----- promotions -----

  getPromotions(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.baseUrl}/promotions`);
  }

  savePromotion(payload: PromotionPayload): Observable<Promotion> {
    return this.http.post<Promotion>(`${this.baseUrl}/promotions`, payload);
  }

  deletePromotion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/promotions/${id}`);
  }

  // ----- analytics -----

  getRevenueOverTime(days = 30): Observable<RevenuePoint[]> {
    const params = new HttpParams().set('days', days);
    return this.http.get<RevenuePoint[]>(`${this.baseUrl}/analytics/revenue`, { params });
  }

  getTopProducts(days = 30, limit = 5): Observable<TopProduct[]> {
    const params = new HttpParams().set('days', days).set('limit', limit);
    return this.http.get<TopProduct[]>(`${this.baseUrl}/analytics/top-products`, { params });
  }

  getOrderStatusBreakdown(): Observable<StatusCount[]> {
    return this.http.get<StatusCount[]>(`${this.baseUrl}/analytics/order-status`);
  }

  getAnalyticsSummary(): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>(`${this.baseUrl}/analytics/summary`);
  }

  // ----- content (CMS) -----

  getBanner(): Observable<SiteBanner> {
    return this.http.get<SiteBanner>(`${this.baseUrl}/content/banner`);
  }

  saveBanner(payload: SiteBannerPayload): Observable<SiteBanner> {
    return this.http.put<SiteBanner>(`${this.baseUrl}/content/banner`, payload);
  }

  getFaqAdmin(): Observable<AdminFaqEntry[]> {
    return this.http.get<AdminFaqEntry[]>(`${this.baseUrl}/content/faq`);
  }

  saveFaq(payload: AdminFaqEntryPayload): Observable<AdminFaqEntry> {
    return this.http.post<AdminFaqEntry>(`${this.baseUrl}/content/faq`, payload);
  }

  deleteFaq(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/content/faq/${id}`);
  }

  // ----- tax & shipping -----

  getTaxRates(): Observable<AdminTaxRate[]> {
    return this.http.get<AdminTaxRate[]>(`${this.baseUrl}/tax-rates`);
  }

  saveTaxRate(payload: AdminTaxRate): Observable<AdminTaxRate> {
    return this.http.post<AdminTaxRate>(`${this.baseUrl}/tax-rates`, payload);
  }

  deleteTaxRate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tax-rates/${id}`);
  }

  getShippingMethods(): Observable<AdminShippingMethod[]> {
    return this.http.get<AdminShippingMethod[]>(`${this.baseUrl}/shipping-methods`);
  }

  saveShippingMethod(payload: AdminShippingMethod): Observable<AdminShippingMethod> {
    return this.http.post<AdminShippingMethod>(`${this.baseUrl}/shipping-methods`, payload);
  }

  deleteShippingMethod(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/shipping-methods/${id}`);
  }

  // ----- gift cards -----

  getGiftCards(): Observable<AdminGiftCard[]> {
    return this.http.get<AdminGiftCard[]>(`${this.baseUrl}/gift-cards`);
  }

  issueGiftCard(payload: AdminGiftCardPayload): Observable<AdminGiftCard> {
    return this.http.post<AdminGiftCard>(`${this.baseUrl}/gift-cards`, payload);
  }

  deactivateGiftCard(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/gift-cards/${id}`);
  }

  // ----- inventory -----

  getInventory(): Observable<InventoryItem[]> {
    return this.http.get<InventoryItem[]>(`${this.baseUrl}/inventory`);
  }

  getInventoryAdjustments(page: number, size: number): Observable<PageResponse<InventoryAdjustment>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<InventoryAdjustment>>(`${this.baseUrl}/inventory/adjustments`, { params });
  }

  adjustInventory(sku: string, quantity: number, note?: string): Observable<InventoryItem> {
    return this.http.put<InventoryItem>(`${this.baseUrl}/inventory/${encodeURIComponent(sku)}`, { quantity, note });
  }

  exportInventoryCsv(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/inventory/export`, { responseType: 'blob' });
  }

  importInventoryCsv(file: File): Observable<CsvImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CsvImportResult>(`${this.baseUrl}/inventory/import`, formData);
  }

  // ----- RBAC + audit log (roadmap #19) -----

  getCurrentAdmin(): Observable<CurrentAdmin> {
    return this.http.get<CurrentAdmin>(`${this.baseUrl}/me`);
  }

  getAuditLog(page: number, size: number, entityType?: string): Observable<PageResponse<AuditLogEntry>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (entityType) {
      params = params.set('entityType', entityType);
    }
    return this.http.get<PageResponse<AuditLogEntry>>(`${this.baseUrl}/audit-log`, { params });
  }
}

export interface AdminGiftCard {
  id: number;
  code: string;
  initialBalance: number;
  balance: number;
  recipientEmail?: string | null;
  active: boolean;
  dateCreated: string;
}

export interface AdminGiftCardPayload {
  code?: string;
  initialBalance: number;
  recipientEmail?: string | null;
  active: boolean;
}

export interface AdminTaxRate {
  id?: number | null;
  country: string;
  state?: string | null;
  ratePercent: number;
  active: boolean;
}

export interface AdminShippingMethod {
  id?: number | null;
  code: string;
  name: string;
  baseRate: number;
  freeOverThreshold?: number | null;
  estimatedDays?: string;
  sortOrder: number;
  active: boolean;
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

export interface Promotion {
  id: number;
  name: string;
  description?: string;
  percentOff?: number | null;
  amountOff?: number | null;
  minSpend?: number | null;
  active: boolean;
  startsAt?: string | null;
  endsAt?: string | null;
}

export interface PromotionPayload {
  id?: number | null;
  name: string;
  description?: string;
  percentOff?: number | null;
  amountOff?: number | null;
  minSpend?: number | null;
  active: boolean;
  startsAt?: string | null;
  endsAt?: string | null;
}

export interface RevenuePoint {
  date: string;
  revenue: number;
  orderCount: number;
}

export interface TopProduct {
  productId: number;
  name: string;
  unitsSold: number;
  revenue: number;
}

export interface StatusCount {
  status: string;
  count: number;
}

export interface AnalyticsSummary {
  averageOrderValue: number;
  revenueThisMonth: number;
  revenueLastMonth: number;
  growthPercent: number | null;
}

export interface SiteBanner {
  id?: number | null;
  message: string;
  linkUrl?: string | null;
  linkText?: string | null;
  active: boolean;
}

export interface SiteBannerPayload {
  message: string;
  linkUrl?: string | null;
  linkText?: string | null;
  active: boolean;
}

export interface AdminFaqEntry {
  id: number;
  question: string;
  answer: string;
  sortOrder: number;
  active: boolean;
}

export interface AdminFaqEntryPayload {
  id?: number | null;
  question: string;
  answer: string;
  sortOrder: number;
  active: boolean;
}

export interface InventoryItem {
  sku: string;
  productId: number;
  productName: string;
  variantLabel?: string | null;
  unitsInStock: number;
  lowStock: boolean;
  active: boolean;
}

export interface InventoryAdjustment {
  id: number;
  sku: string;
  productName: string;
  previousQuantity: number;
  newQuantity: number;
  delta: number;
  source: string;
  note?: string | null;
  dateCreated: string;
}

export interface CsvImportResult {
  updated: number;
  errors: string[];
}

export interface CurrentAdmin {
  roles: string[];
}

export interface AuditLogEntry {
  id: number;
  actor: string;
  action: string;
  entityType: string;
  entityId: string | null;
  details: string | null;
  createdAt: string;
}
