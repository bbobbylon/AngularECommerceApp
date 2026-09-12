import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminApiKey, AdminApiKeyCreated, AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/** Available role strings (roadmap #19 RBAC) an issued key can be granted — no new vocabulary. */
const AVAILABLE_AUTHORITIES = ['Admin', 'OrderManager', 'Viewer'];

/**
 * Admin API-key management (roadmap #23, Milestone A): issue keys for headless/programmatic access
 * to this tenant's own back office, list existing keys, revoke. The raw secret is shown exactly once,
 * right after issuing — the list view never carries it again afterward.
 */
@Component({
  selector: 'app-admin-api-keys',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-api-keys.html',
})
export class AdminApiKeys implements OnInit {

  readonly availableAuthorities = AVAILABLE_AUTHORITIES;

  readonly keys = signal<AdminApiKey[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly justCreated = signal<AdminApiKeyCreated | null>(null);

  name = '';
  selectedAuthorities: Record<string, boolean> = { Admin: false, OrderManager: false, Viewer: false };

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getApiKeys().subscribe({
      next: list => { this.keys.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Could not load API keys.'); },
    });
  }

  issue(): void {
    const authorities = this.availableAuthorities.filter(a => this.selectedAuthorities[a]);
    if (!this.name.trim()) {
      this.toast.error('Enter a name for the key.');
      return;
    }
    if (authorities.length === 0) {
      this.toast.error('Select at least one authority.');
      return;
    }
    this.saving.set(true);
    this.admin.issueApiKey({ name: this.name.trim(), authorities }).subscribe({
      next: created => {
        this.justCreated.set(created);
        this.name = '';
        this.selectedAuthorities = { Admin: false, OrderManager: false, Viewer: false };
        this.saving.set(false);
        this.load();
      },
      error: () => { this.saving.set(false); this.toast.error('Could not issue the API key.'); },
    });
  }

  dismissReveal(): void {
    this.justCreated.set(null);
  }

  revoke(key: AdminApiKey): void {
    if (!confirm(`Revoke "${key.name}"? Anything using it will stop working immediately.`)) {
      return;
    }
    this.admin.revokeApiKey(key.id).subscribe({
      next: () => { this.toast.success(`Revoked ${key.name}`); this.load(); },
      error: () => this.toast.error('Could not revoke the key.'),
    });
  }
}
