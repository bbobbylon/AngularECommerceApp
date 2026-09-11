import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AdminService } from '../../../services/admin.service';
import { BillingPlan, PlatformService, PlatformTenant, PlatformTenantPayload } from '../../../services/platform.service';
import { TenantContextService } from '../../../services/tenant-context.service';
import { ToastService } from '../../../services/toast.service';

/**
 * Platform-level tenant CRUD (roadmap #21, Milestone B) — the first real UI for something
 * {@code TenantRepository}'s own javadoc had been deferring since Milestone A: creating, editing, and
 * deactivating the tenants hosted on this deployment. Gated server-side on the {@code SuperAdmin}
 * authority; this component additionally checks the caller's roles via the existing
 * {@code GET /api/admin/me} endpoint so a non-superadmin sees a clear message instead of an empty table
 * (courtesy UX, not the enforcement boundary — that's `SecurityConfig`). Plan assignment (roadmap #22)
 * is a separate row-level action from the identity edit form below, since it's a distinct
 * endpoint/concern even though both are SuperAdmin-only today.
 */
@Component({
  selector: 'app-platform-tenants',
  imports: [CommonModule, FormsModule],
  templateUrl: './platform-tenants.html',
})
export class PlatformTenants implements OnInit {

  readonly tenants = signal<PlatformTenant[]>([]);
  readonly plans = signal<BillingPlan[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly authorized = signal(false);
  readonly checkingAccess = signal(true);

  form: PlatformTenantPayload = this.empty();

  private platform = inject(PlatformService);
  private adminService = inject(AdminService);
  private tenantContext = inject(TenantContextService);
  private toast = inject(ToastService);
  private router = inject(Router);

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
    this.platform.getTenants().subscribe({
      next: list => { this.tenants.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Could not load tenants.'); },
    });
    this.platform.getBillingPlans().subscribe({
      next: list => this.plans.set(list.filter(p => p.active)),
      error: () => this.toast.error('Could not load billing plans.'),
    });
  }

  save(): void {
    if (!this.form.slug.trim() || !this.form.displayName.trim()) {
      this.toast.error('Slug and display name are required.');
      return;
    }
    this.saving.set(true);
    this.platform.saveTenant({
      ...this.form,
      slug: this.form.slug.trim().toLowerCase(),
      displayName: this.form.displayName.trim(),
    }).subscribe({
      next: () => {
        this.toast.success('Tenant saved');
        this.form = this.empty();
        this.saving.set(false);
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err?.error?.message ?? 'Could not save tenant.');
      },
    });
  }

  edit(tenant: PlatformTenant): void {
    this.form = {
      id: tenant.id,
      slug: tenant.slug,
      displayName: tenant.displayName,
      contactEmail: tenant.contactEmail,
      active: tenant.active,
    };
  }

  cancelEdit(): void {
    this.form = this.empty();
  }

  deactivate(tenant: PlatformTenant): void {
    if (!confirm(`Deactivate tenant "${tenant.displayName}"? Its storefront/admin traffic will 404 until reactivated.`)) {
      return;
    }
    this.platform.deactivateTenant(tenant.id).subscribe({
      next: () => { this.toast.success('Deactivated'); this.load(); },
      error: () => this.toast.error('Could not deactivate tenant.'),
    });
  }

  assignPlan(tenant: PlatformTenant, planId: string): void {
    const id = planId ? Number(planId) : null;
    this.platform.assignTenantPlan(tenant.id, id).subscribe({
      next: () => { this.toast.success('Billing plan updated'); this.load(); },
      error: () => this.toast.error('Could not assign billing plan.'),
    });
  }

  /** Switches the regular admin back-office to reflect this tenant's data, then navigates there. */
  viewAsAdmin(tenant: PlatformTenant): void {
    this.tenantContext.viewAs(tenant.slug);
    this.toast.success(`Viewing admin as "${tenant.displayName}"`);
    this.router.navigateByUrl('/admin');
  }

  private empty(): PlatformTenantPayload {
    return { id: null, slug: '', displayName: '', contactEmail: '', active: true };
  }
}
