import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { AdminService, AnalyticsSummary, RevenuePoint, StatusCount, TopProduct } from '../../../services/admin.service';

/** Sales analytics (roadmap #18): revenue trend, best sellers, order-status mix, top-line KPIs. */
@Component({
  selector: 'app-admin-analytics',
  imports: [CurrencyPipe, DatePipe, DecimalPipe],
  templateUrl: './admin-analytics.html',
})
export class AdminAnalytics implements OnInit {

  readonly summary = signal<AnalyticsSummary | null>(null);
  readonly revenue = signal<RevenuePoint[]>([]);
  readonly topProducts = signal<TopProduct[]>([]);
  readonly statusBreakdown = signal<StatusCount[]>([]);
  readonly loading = signal(true);

  readonly maxRevenue = computed(() => Math.max(1, ...this.revenue().map(p => p.revenue)));
  readonly maxTopProductRevenue = computed(() => Math.max(1, ...this.topProducts().map(p => p.revenue)));
  readonly maxStatusCount = computed(() => Math.max(1, ...this.statusBreakdown().map(s => s.count)));
  readonly totalOrders30d = computed(() => this.revenue().reduce((sum, p) => sum + p.orderCount, 0));

  private admin = inject(AdminService);

  ngOnInit(): void {
    this.admin.getAnalyticsSummary().subscribe(s => this.summary.set(s));
    this.admin.getRevenueOverTime(30).subscribe(points => {
      this.revenue.set(points);
      this.loading.set(false);
    });
    this.admin.getTopProducts(30, 5).subscribe(products => this.topProducts.set(products));
    this.admin.getOrderStatusBreakdown().subscribe(statuses => this.statusBreakdown.set(statuses));
  }

  barHeight(revenue: number): number {
    return Math.max(2, Math.round((revenue / this.maxRevenue()) * 100));
  }

  barWidth(value: number, max: number): number {
    return Math.max(2, Math.round((value / max) * 100));
  }
}
