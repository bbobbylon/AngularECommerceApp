import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

import { isOnSale } from '../../../common/product';
import { AdminProduct, AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-admin-products',
  imports: [CommonModule, RouterLink, NgbPaginationModule],
  templateUrl: './admin-products.html',
})
export class AdminProducts implements OnInit {

  readonly products = signal<AdminProduct[]>([]);
  readonly loading = signal(true);

  pageNumber = 1;
  pageSize = 10;
  totalElements = 0;

  readonly isOnSale = isOnSale;

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getProducts(this.pageNumber - 1, this.pageSize).subscribe({
      next: res => {
        this.products.set(res.content);
        this.totalElements = res.totalElements;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load products.');
      },
    });
  }

  remove(product: AdminProduct): void {
    if (!confirm(`Delete "${product.name}"? This can't be undone.`)) {
      return;
    }
    this.admin.deleteProduct(product.id).subscribe({
      next: () => {
        this.toast.success(`Deleted ${product.name}`);
        // step back a page if we just removed the last row on it
        if (this.products().length === 1 && this.pageNumber > 1) {
          this.pageNumber--;
        }
        this.load();
      },
      error: () => this.toast.error('Could not delete product.'),
    });
  }
}
