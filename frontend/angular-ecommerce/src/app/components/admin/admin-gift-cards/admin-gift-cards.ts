import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminGiftCard, AdminGiftCardPayload, AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/** Admin gift-card management: issue cards (auto-generated or custom code), list balances, deactivate. */
@Component({
  selector: 'app-admin-gift-cards',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-gift-cards.html',
})
export class AdminGiftCards implements OnInit {

  readonly cards = signal<AdminGiftCard[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  form: AdminGiftCardPayload = this.empty();

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getGiftCards().subscribe({
      next: list => { this.cards.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Could not load gift cards.'); },
    });
  }

  issue(): void {
    if (!this.form.initialBalance || this.form.initialBalance <= 0) {
      this.toast.error('Enter an initial balance greater than zero.');
      return;
    }
    this.saving.set(true);
    this.admin.issueGiftCard({
      code: this.form.code?.trim() || undefined,
      initialBalance: Number(this.form.initialBalance),
      recipientEmail: this.form.recipientEmail?.trim() || null,
      active: this.form.active,
    }).subscribe({
      next: card => {
        this.toast.success(`Issued ${card.code}`);
        this.form = this.empty();
        this.saving.set(false);
        this.load();
      },
      error: () => { this.saving.set(false); this.toast.error('Could not issue the gift card.'); },
    });
  }

  deactivate(card: AdminGiftCard): void {
    if (!confirm(`Deactivate ${card.code}?`)) {
      return;
    }
    this.admin.deactivateGiftCard(card.id).subscribe({
      next: () => { this.toast.success(`Deactivated ${card.code}`); this.load(); },
      error: () => this.toast.error('Could not deactivate the gift card.'),
    });
  }

  private empty(): AdminGiftCardPayload {
    return { code: '', initialBalance: 25, recipientEmail: '', active: true };
  }
}
