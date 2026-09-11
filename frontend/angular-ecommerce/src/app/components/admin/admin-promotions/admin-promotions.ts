import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminService, Promotion, PromotionPayload } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/**
 * Admin promotions (roadmap #16): automatic, no-code discounts scheduled over a date window. Unlike
 * coupons, these apply themselves at checkout whenever the best-value active one qualifies.
 */
@Component({
  selector: 'app-admin-promotions',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-promotions.html',
})
export class AdminPromotions implements OnInit {

  readonly promotions = signal<Promotion[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  // create form
  form: PromotionPayload & { discountType: 'percent' | 'amount'; discountValue: number | null } = this.emptyForm();

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getPromotions().subscribe({
      next: list => {
        this.promotions.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load promotions.');
      },
    });
  }

  create(): void {
    const name = this.form.name.trim();
    if (!name || !this.form.discountValue || this.form.discountValue <= 0) {
      this.toast.error('Enter a name and a discount value.');
      return;
    }
    const payload: PromotionPayload = {
      name,
      description: this.form.description?.trim() || '',
      percentOff: this.form.discountType === 'percent' ? this.form.discountValue : null,
      amountOff: this.form.discountType === 'amount' ? this.form.discountValue : null,
      minSpend: this.form.minSpend || null,
      active: this.form.active,
      startsAt: this.form.startsAt || null,
      endsAt: this.form.endsAt || null,
    };
    this.saving.set(true);
    this.admin.savePromotion(payload).subscribe({
      next: () => {
        this.toast.success(`Promotion ${payload.name} saved`);
        this.form = this.emptyForm();
        this.saving.set(false);
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('Could not save promotion.');
      },
    });
  }

  remove(promotion: Promotion): void {
    if (!confirm(`Delete promotion ${promotion.name}?`)) {
      return;
    }
    this.admin.deletePromotion(promotion.id).subscribe({
      next: () => {
        this.toast.success(`Deleted ${promotion.name}`);
        this.load();
      },
      error: () => this.toast.error('Could not delete promotion.'),
    });
  }

  discountLabel(p: Promotion): string {
    if (p.percentOff) {
      return `${p.percentOff}% off`;
    }
    if (p.amountOff) {
      return `$${p.amountOff} off`;
    }
    return '—';
  }

  windowLabel(p: Promotion): string {
    if (!p.startsAt && !p.endsAt) {
      return 'Always on';
    }
    return `${p.startsAt ?? 'Any time'} → ${p.endsAt ?? 'Ongoing'}`;
  }

  private emptyForm() {
    return {
      name: '',
      description: '',
      percentOff: null,
      amountOff: null,
      minSpend: null,
      active: true,
      startsAt: null,
      endsAt: null,
      discountType: 'percent' as 'percent' | 'amount',
      discountValue: null as number | null,
    };
  }
}
