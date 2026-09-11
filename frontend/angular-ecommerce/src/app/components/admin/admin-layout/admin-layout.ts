import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AdminService } from '../../../services/admin.service';
import { TenantContextService } from '../../../services/tenant-context.service';

/**
 * Shell for the whole admin back-office. Fetches the caller's RBAC (roadmap #19) role(s) once, purely
 * as a courtesy label in the sidebar — the backend's request-matcher authorization is the actual
 * enforcement boundary, this is not a client-side permission gate. Also surfaces whether a superadmin
 * (roadmap #21, Milestone B) is currently "viewing as" another tenant via {@link TenantContextService},
 * with a reset control and a link into the platform tier for those with the `SuperAdmin` role.
 */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-layout.html',
})
export class AdminLayout implements OnInit {

  readonly roles = signal<string[]>([]);

  readonly tenantContext = inject(TenantContextService);
  private admin = inject(AdminService);

  ngOnInit(): void {
    this.admin.getCurrentAdmin().subscribe({
      next: res => this.roles.set(res.roles),
      error: () => this.roles.set([]),
    });
  }

  resetTenantView(): void {
    this.tenantContext.reset();
  }
}
