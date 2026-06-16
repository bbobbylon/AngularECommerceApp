import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminService, Coupon, CouponPayload } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-admin-coupons',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-coupons.html',
})
export class AdminCoupons implements OnInit {

  readonly coupons = signal<Coupon[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  // create form
  form: CouponPayload & { discountType: 'percent' | 'amount'; discountValue: number | null } = this.emptyForm();

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getCoupons().subscribe({
      next: list => {
        this.coupons.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load coupons.');
      },
    });
  }

  create(): void {
    const code = this.form.code.trim();
    if (!code || !this.form.discountValue || this.form.discountValue <= 0) {
      this.toast.error('Enter a code and a discount value.');
      return;
    }
    const payload: CouponPayload = {
      code: code.toUpperCase(),
      description: this.form.description?.trim() || '',
      percentOff: this.form.discountType === 'percent' ? this.form.discountValue : null,
      amountOff: this.form.discountType === 'amount' ? this.form.discountValue : null,
      minSpend: this.form.minSpend || null,
      active: this.form.active,
      expiresAt: this.form.expiresAt || null,
    };
    this.saving.set(true);
    this.admin.createCoupon(payload).subscribe({
      next: () => {
        this.toast.success(`Coupon ${payload.code} saved`);
        this.form = this.emptyForm();
        this.saving.set(false);
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('Could not save coupon.');
      },
    });
  }

  remove(coupon: Coupon): void {
    if (!confirm(`Delete coupon ${coupon.code}?`)) {
      return;
    }
    this.admin.deleteCoupon(coupon.id).subscribe({
      next: () => {
        this.toast.success(`Deleted ${coupon.code}`);
        this.load();
      },
      error: () => this.toast.error('Could not delete coupon.'),
    });
  }

  discountLabel(c: Coupon): string {
    if (c.percentOff) {
      return `${c.percentOff}% off`;
    }
    if (c.amountOff) {
      return `$${c.amountOff} off`;
    }
    return '—';
  }

  private emptyForm() {
    return {
      code: '',
      description: '',
      percentOff: null,
      amountOff: null,
      minSpend: null,
      active: true,
      expiresAt: null,
      discountType: 'percent' as 'percent' | 'amount',
      discountValue: null as number | null,
    };
  }
}
