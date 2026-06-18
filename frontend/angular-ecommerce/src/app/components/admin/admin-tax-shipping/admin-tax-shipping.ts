import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminService, AdminShippingMethod, AdminTaxRate } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/** Admin config for sales-tax rates (by region) and shipping methods. */
@Component({
  selector: 'app-admin-tax-shipping',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-tax-shipping.html',
})
export class AdminTaxShipping implements OnInit {

  readonly taxRates = signal<AdminTaxRate[]>([]);
  readonly shippingMethods = signal<AdminShippingMethod[]>([]);
  readonly loading = signal(true);
  readonly savingTax = signal(false);
  readonly savingShip = signal(false);

  taxForm: AdminTaxRate = this.emptyTax();
  shipForm: AdminShippingMethod = this.emptyShip();

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getTaxRates().subscribe({
      next: rates => this.taxRates.set(rates),
      error: () => this.toast.error('Could not load tax rates.'),
    });
    this.admin.getShippingMethods().subscribe({
      next: methods => {
        this.shippingMethods.set(methods);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load shipping methods.');
      },
    });
  }

  // ----- tax rates -----

  saveTax(): void {
    if (!this.taxForm.country.trim() || this.taxForm.ratePercent == null) {
      this.toast.error('Country and rate are required.');
      return;
    }
    this.savingTax.set(true);
    this.admin.saveTaxRate({
      ...this.taxForm,
      country: this.taxForm.country.trim(),
      state: this.taxForm.state?.trim() || null,
    }).subscribe({
      next: () => {
        this.toast.success('Tax rate saved');
        this.taxForm = this.emptyTax();
        this.savingTax.set(false);
        this.load();
      },
      error: () => {
        this.savingTax.set(false);
        this.toast.error('Could not save tax rate.');
      },
    });
  }

  editTax(rate: AdminTaxRate): void {
    this.taxForm = { ...rate, state: rate.state ?? '' };
  }

  deleteTax(rate: AdminTaxRate): void {
    if (!rate.id || !confirm(`Delete the ${rate.country}${rate.state ? ' / ' + rate.state : ''} tax rate?`)) {
      return;
    }
    this.admin.deleteTaxRate(rate.id).subscribe({
      next: () => { this.toast.success('Deleted'); this.load(); },
      error: () => this.toast.error('Could not delete.'),
    });
  }

  // ----- shipping methods -----

  saveShip(): void {
    if (!this.shipForm.code.trim() || !this.shipForm.name.trim() || this.shipForm.baseRate == null) {
      this.toast.error('Code, name and base rate are required.');
      return;
    }
    this.savingShip.set(true);
    this.admin.saveShippingMethod({
      ...this.shipForm,
      code: this.shipForm.code.trim().toUpperCase(),
      name: this.shipForm.name.trim(),
      freeOverThreshold: this.shipForm.freeOverThreshold || null,
    }).subscribe({
      next: () => {
        this.toast.success('Shipping method saved');
        this.shipForm = this.emptyShip();
        this.savingShip.set(false);
        this.load();
      },
      error: () => {
        this.savingShip.set(false);
        this.toast.error('Could not save shipping method.');
      },
    });
  }

  editShip(method: AdminShippingMethod): void {
    this.shipForm = { ...method };
  }

  deleteShip(method: AdminShippingMethod): void {
    if (!method.id || !confirm(`Delete shipping method ${method.name}?`)) {
      return;
    }
    this.admin.deleteShippingMethod(method.id).subscribe({
      next: () => { this.toast.success('Deleted'); this.load(); },
      error: () => this.toast.error('Could not delete.'),
    });
  }

  private emptyTax(): AdminTaxRate {
    return { id: null, country: 'United States', state: '', ratePercent: 0, active: true };
  }

  private emptyShip(): AdminShippingMethod {
    return { id: null, code: '', name: '', baseRate: 0, freeOverThreshold: null, estimatedDays: '', sortOrder: 0, active: true };
  }
}
