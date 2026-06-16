import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

import { AdminOrderView, AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-admin-orders',
  imports: [CommonModule, NgbPaginationModule],
  templateUrl: './admin-orders.html',
})
export class AdminOrders implements OnInit {

  readonly orders = signal<AdminOrderView[]>([]);
  readonly loading = signal(true);

  pageNumber = 1;
  pageSize = 15;
  totalElements = 0;

  readonly statuses = ['Received', 'Processing', 'Shipped', 'Delivered', 'Cancelled'];

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getOrders(this.pageNumber - 1, this.pageSize).subscribe({
      next: res => {
        this.orders.set(res.content);
        this.totalElements = res.totalElements;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load orders.');
      },
    });
  }

  updateStatus(order: AdminOrderView, status: string): void {
    this.admin.updateOrderStatus(order.id, status).subscribe({
      next: updated => {
        this.orders.update(list => list.map(o => (o.id === updated.id ? updated : o)));
        this.toast.success(`Order #${order.id} → ${status}`);
      },
      error: () => this.toast.error('Could not update order status.'),
    });
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'Delivered': return 'bg-success-subtle text-success-emphasis';
      case 'Shipped': return 'bg-info-subtle text-info-emphasis';
      case 'Cancelled': return 'bg-danger-subtle text-danger-emphasis';
      case 'Processing': return 'bg-warning-subtle text-warning-emphasis';
      default: return 'bg-secondary-subtle text-secondary';
    }
  }
}
