import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

import { AdminService, AuditLogEntry } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/** Read-only view of the global admin audit log (roadmap #19) — who did what, to which entity, and when. */
@Component({
  selector: 'app-admin-audit-log',
  imports: [CommonModule, FormsModule, NgbPaginationModule],
  templateUrl: './admin-audit-log.html',
})
export class AdminAuditLog implements OnInit {

  readonly entries = signal<AuditLogEntry[]>([]);
  readonly loading = signal(true);

  readonly entityTypes = ['Product', 'Order', 'ReturnRequest', 'Coupon', 'Promotion', 'GiftCard',
    'TaxRate', 'ShippingMethod', 'SiteBanner', 'FaqEntry', 'InventoryItem', 'ProductCategory', 'Review'];
  entityTypeFilter = '';

  pageNumber = 1;
  pageSize = 20;
  totalElements = 0;

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getAuditLog(this.pageNumber - 1, this.pageSize, this.entityTypeFilter || undefined).subscribe({
      next: res => {
        this.entries.set(res.content);
        this.totalElements = res.totalElements;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load the audit log.');
      },
    });
  }

  onFilterChange(): void {
    this.pageNumber = 1;
    this.load();
  }
}
