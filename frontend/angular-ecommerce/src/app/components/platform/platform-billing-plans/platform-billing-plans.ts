import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminService } from '../../../services/admin.service';
import { BillingPlan, BillingPlanPayload, PlatformService } from '../../../services/platform.service';
import { ToastService } from '../../../services/toast.service';

/**
 * Platform-level billing-plan catalog CRUD (roadmap #22) — mirrors {@code PlatformTenants}'s layout
 * and access-check pattern exactly. Deactivating a plan here never breaks a tenant already on it
 * (soft-retire, matches {@code TaxRate}/{@code ShippingMethod}/{@code Tenant.active}); assigning a
 * plan to a tenant happens on the Tenants page, not here.
 */
@Component({
  selector: 'app-platform-billing-plans',
  imports: [CommonModule, FormsModule],
  templateUrl: './platform-billing-plans.html',
})
export class PlatformBillingPlans implements OnInit {

  readonly plans = signal<BillingPlan[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly authorized = signal(false);
  readonly checkingAccess = signal(true);

  form: BillingPlanPayload = this.empty();

  private platform = inject(PlatformService);
  private adminService = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.adminService.getCurrentAdmin().subscribe({
      next: res => {
        this.checkingAccess.set(false);
        this.authorized.set(res.roles.includes('SuperAdmin'));
        if (this.authorized()) {
          this.load();
        }
      },
      error: () => {
        this.checkingAccess.set(false);
        this.authorized.set(false);
      },
    });
  }

  load(): void {
    this.loading.set(true);
    this.platform.getBillingPlans().subscribe({
      next: list => { this.plans.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Could not load billing plans.'); },
    });
  }

  save(): void {
    if (!this.form.name.trim() || this.form.monthlyPrice == null || this.form.monthlyPrice < 0) {
      this.toast.error('Name and a non-negative monthly price are required.');
      return;
    }
    this.saving.set(true);
    this.platform.saveBillingPlan({ ...this.form, name: this.form.name.trim() }).subscribe({
      next: () => {
        this.toast.success('Billing plan saved');
        this.form = this.empty();
        this.saving.set(false);
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Could not save billing plan.');
      },
    });
  }

  edit(plan: BillingPlan): void {
    this.form = {
      id: plan.id,
      name: plan.name,
      monthlyPrice: plan.monthlyPrice,
      currency: plan.currency,
      features: plan.features,
      sortOrder: plan.sortOrder,
      active: plan.active,
    };
  }

  cancelEdit(): void {
    this.form = this.empty();
  }

  deactivate(plan: BillingPlan): void {
    if (!confirm(`Deactivate plan "${plan.name}"? Tenants already on it keep it; it just won't be assignable going forward.`)) {
      return;
    }
    this.platform.deactivateBillingPlan(plan.id).subscribe({
      next: () => { this.toast.success('Deactivated'); this.load(); },
      error: () => this.toast.error('Could not deactivate billing plan.'),
    });
  }

  private empty(): BillingPlanPayload {
    return { id: null, name: '', monthlyPrice: 0, currency: 'USD', features: '', sortOrder: 0, active: true };
  }
}
