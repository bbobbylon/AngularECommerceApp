import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminService,
  CsvImportResult,
  InventoryAdjustment,
  InventoryItem,
} from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/**
 * Admin inventory management (roadmap #15): a merged product+variant stock view with inline
 * quantity edits, CSV export/import for bulk restocks, and an audit trail of every change.
 */
@Component({
  selector: 'app-admin-inventory',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-inventory.html',
})
export class AdminInventory implements OnInit {

  readonly items = signal<InventoryItem[]>([]);
  readonly loading = signal(true);
  readonly saving = signal<string | null>(null);
  readonly search = signal('');

  readonly history = signal<InventoryAdjustment[]>([]);
  readonly historyLoading = signal(true);

  readonly importing = signal(false);
  readonly importResult = signal<CsvImportResult | null>(null);

  drafts: Record<string, number> = {};

  readonly filteredItems = computed(() => {
    const q = this.search().trim().toLowerCase();
    if (!q) {
      return this.items();
    }
    return this.items().filter(i =>
      i.sku.toLowerCase().includes(q) ||
      i.productName.toLowerCase().includes(q) ||
      (i.variantLabel ?? '').toLowerCase().includes(q));
  });

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
    this.loadHistory();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getInventory().subscribe({
      next: items => {
        this.items.set(items);
        this.drafts = Object.fromEntries(items.map(i => [i.sku, i.unitsInStock]));
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); this.toast.error('Could not load inventory.'); },
    });
  }

  loadHistory(): void {
    this.historyLoading.set(true);
    this.admin.getInventoryAdjustments(0, 20).subscribe({
      next: page => { this.history.set(page.content); this.historyLoading.set(false); },
      error: () => this.historyLoading.set(false),
    });
  }

  save(item: InventoryItem): void {
    const qty = Number(this.drafts[item.sku]);
    if (!Number.isFinite(qty) || qty < 0) {
      this.toast.error('Enter a valid, non-negative quantity.');
      return;
    }
    if (qty === item.unitsInStock) {
      return;
    }
    this.saving.set(item.sku);
    this.admin.adjustInventory(item.sku, qty).subscribe({
      next: updated => {
        this.items.update(list => list.map(i => i.sku === item.sku ? updated : i));
        this.drafts[item.sku] = updated.unitsInStock;
        this.saving.set(null);
        this.toast.success(`${item.sku} is now at ${updated.unitsInStock} in stock.`);
        this.loadHistory();
      },
      error: () => { this.saving.set(null); this.toast.error(`Could not update ${item.sku}.`); },
    });
  }

  exportCsv(): void {
    this.admin.exportInventoryCsv().subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'inventory.csv';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Could not export inventory.'),
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.importing.set(true);
    this.importResult.set(null);
    this.admin.importInventoryCsv(file).subscribe({
      next: result => {
        this.importing.set(false);
        this.importResult.set(result);
        input.value = '';
        if (result.updated > 0) {
          this.toast.success(`Updated ${result.updated} SKU${result.updated === 1 ? '' : 's'} from the CSV.`);
          this.load();
          this.loadHistory();
        }
        if (result.errors.length > 0) {
          this.toast.error(`${result.errors.length} row${result.errors.length === 1 ? '' : 's'} had errors — see details below.`);
        }
      },
      error: () => {
        this.importing.set(false);
        this.toast.error('Could not import the CSV file.');
        input.value = '';
      },
    });
  }
}
