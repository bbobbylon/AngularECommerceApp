import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminService, Warehouse, WarehousePayload, WarehouseStockRow } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/**
 * Admin warehouse configuration + per-SKU stock distribution (roadmap #20). Shipping actions
 * (create/advance a shipment) live on the Orders page — this page is about where stock physically
 * sits, not about fulfilling any one order.
 */
@Component({
  selector: 'app-admin-warehouses',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-warehouses.html',
})
export class AdminWarehouses implements OnInit {

  readonly warehouses = signal<Warehouse[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  form: WarehousePayload = this.empty();

  readonly selectedWarehouse = signal<Warehouse | null>(null);
  readonly stockRows = signal<WarehouseStockRow[]>([]);
  readonly stockLoading = signal(false);
  readonly stockSaving = signal<string | null>(null);
  readonly stockSearch = signal('');

  stockDrafts: Record<string, number> = {};

  readonly filteredStockRows = computed(() => {
    const q = this.stockSearch().trim().toLowerCase();
    if (!q) {
      return this.stockRows();
    }
    return this.stockRows().filter(r =>
      r.sku.toLowerCase().includes(q) ||
      r.productName.toLowerCase().includes(q) ||
      (r.variantLabel ?? '').toLowerCase().includes(q));
  });

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getWarehouses().subscribe({
      next: list => { this.warehouses.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Could not load warehouses.'); },
    });
  }

  save(): void {
    if (!this.form.code.trim() || !this.form.name.trim()) {
      this.toast.error('Code and name are required.');
      return;
    }
    this.saving.set(true);
    this.admin.saveWarehouse({
      ...this.form,
      code: this.form.code.trim().toUpperCase(),
      name: this.form.name.trim(),
    }).subscribe({
      next: () => {
        this.toast.success('Warehouse saved');
        this.form = this.empty();
        this.saving.set(false);
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('Could not save warehouse.');
      },
    });
  }

  edit(warehouse: Warehouse): void {
    this.form = { ...warehouse };
  }

  cancelEdit(): void {
    this.form = this.empty();
  }

  delete(warehouse: Warehouse): void {
    if (!confirm(`Delete warehouse ${warehouse.name}? This only works if it has never shipped anything.`)) {
      return;
    }
    this.admin.deleteWarehouse(warehouse.id).subscribe({
      next: () => {
        this.toast.success('Deleted');
        if (this.selectedWarehouse()?.id === warehouse.id) {
          this.selectedWarehouse.set(null);
        }
        this.load();
      },
      error: err => this.toast.error(err?.error?.message ?? 'Could not delete — deactivate it instead.'),
    });
  }

  selectWarehouse(warehouse: Warehouse): void {
    this.selectedWarehouse.set(warehouse);
    this.stockSearch.set('');
    this.stockLoading.set(true);
    this.admin.getWarehouseStock(warehouse.id).subscribe({
      next: rows => {
        this.stockRows.set(rows);
        this.stockDrafts = Object.fromEntries(rows.map(r => [r.sku, r.quantity]));
        this.stockLoading.set(false);
      },
      error: () => { this.stockLoading.set(false); this.toast.error('Could not load stock for this warehouse.'); },
    });
  }

  saveStock(row: WarehouseStockRow): void {
    const warehouse = this.selectedWarehouse();
    if (!warehouse) {
      return;
    }
    const qty = Number(this.stockDrafts[row.sku]);
    if (!Number.isFinite(qty) || qty < 0) {
      this.toast.error('Enter a valid, non-negative quantity.');
      return;
    }
    if (qty === row.quantity) {
      return;
    }
    this.stockSaving.set(row.sku);
    this.admin.updateWarehouseStock(warehouse.id, [{ sku: row.sku, quantity: qty }]).subscribe({
      next: rows => {
        this.stockRows.set(rows);
        this.stockDrafts = Object.fromEntries(rows.map(r => [r.sku, r.quantity]));
        this.stockSaving.set(null);
        this.toast.success(`${row.sku} is now at ${qty} at ${warehouse.code}.`);
      },
      error: () => { this.stockSaving.set(null); this.toast.error(`Could not update ${row.sku}.`); },
    });
  }

  private empty(): WarehousePayload {
    return { id: null, code: '', name: '', city: '', state: '', country: 'United States', priority: 0, active: true };
  }
}
