import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminService,
  AdminWebhookSubscription,
  AdminWebhookSubscriptionCreated,
} from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/** Event types a subscription can be registered for (roadmap #23, Milestone C publish points). */
const AVAILABLE_EVENT_TYPES = [
  'order.created',
  'shipment.shipped',
  'shipment.delivered',
  'return.approved',
  'return.denied',
];

/**
 * Admin webhook-subscription management (roadmap #23, Milestone B): register a target URL for one or
 * more event types, edit it in place, deactivate. The signing secret is shown exactly once, right
 * after creating a subscription — every later read (including this page's own list) only ever
 * carries the masked form.
 */
@Component({
  selector: 'app-admin-webhooks',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-webhooks.html',
})
export class AdminWebhooks implements OnInit {

  readonly availableEventTypes = AVAILABLE_EVENT_TYPES;

  readonly subscriptions = signal<AdminWebhookSubscription[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly justCreated = signal<AdminWebhookSubscriptionCreated | null>(null);
  readonly editingId = signal<number | null>(null);

  url = '';
  active = true;
  selectedEventTypes: Record<string, boolean> = {};

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.resetForm();
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getWebhooks().subscribe({
      next: list => { this.subscriptions.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Could not load webhook subscriptions.'); },
    });
  }

  startEdit(sub: AdminWebhookSubscription): void {
    this.editingId.set(sub.id);
    this.url = sub.url;
    this.active = sub.active;
    this.selectedEventTypes = {};
    for (const type of sub.eventTypes.split(',')) {
      this.selectedEventTypes[type] = true;
    }
  }

  cancelEdit(): void {
    this.resetForm();
  }

  save(): void {
    const eventTypes = this.availableEventTypes.filter(t => this.selectedEventTypes[t]);
    if (!this.url.trim()) {
      this.toast.error('Enter a target URL.');
      return;
    }
    if (eventTypes.length === 0) {
      this.toast.error('Select at least one event type.');
      return;
    }
    this.saving.set(true);
    const id = this.editingId();
    this.admin.saveWebhook({ id, url: this.url.trim(), eventTypes, active: this.active }).subscribe({
      next: result => {
        if (id == null) {
          this.justCreated.set(result as AdminWebhookSubscriptionCreated);
        }
        this.saving.set(false);
        this.resetForm();
        this.load();
      },
      error: () => { this.saving.set(false); this.toast.error('Could not save the webhook subscription.'); },
    });
  }

  dismissReveal(): void {
    this.justCreated.set(null);
  }

  deactivate(sub: AdminWebhookSubscription): void {
    if (!confirm(`Deactivate the webhook for "${sub.url}"? It will stop receiving events.`)) {
      return;
    }
    this.admin.deactivateWebhook(sub.id).subscribe({
      next: () => { this.toast.success('Webhook deactivated.'); this.load(); },
      error: () => this.toast.error('Could not deactivate the webhook.'),
    });
  }

  private resetForm(): void {
    this.editingId.set(null);
    this.url = '';
    this.active = true;
    this.selectedEventTypes = {};
  }
}
