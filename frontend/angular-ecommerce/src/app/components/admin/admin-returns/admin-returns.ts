import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ReturnRequestView, ReturnService } from '../../../services/return.service';
import { ToastService } from '../../../services/toast.service';

/** Admin returns queue: review requests, then approve (issues a Stripe refund when possible) or deny. */
@Component({
  selector: 'app-admin-returns',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-returns.html',
})
export class AdminReturns implements OnInit {

  readonly returns = signal<ReturnRequestView[]>([]);
  readonly loading = signal(true);

  // per-row decision inputs (keyed by return id)
  noteFor: Record<number, string> = {};
  amountFor: Record<number, number | null> = {};
  busy: number | null = null;

  private svc = inject(ReturnService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.svc.adminList().subscribe({
      next: list => { this.returns.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Could not load returns.'); },
    });
  }

  pending(r: ReturnRequestView): boolean {
    return r.status === 'REQUESTED';
  }

  decide(r: ReturnRequestView, action: 'APPROVE' | 'DENY'): void {
    this.busy = r.id;
    this.svc.decide(r.id, {
      action,
      refundAmount: this.amountFor[r.id] ?? null,
      adminNote: this.noteFor[r.id] || undefined,
    }).subscribe({
      next: () => {
        this.toast.success(action === 'APPROVE' ? 'Return approved' : 'Return denied');
        this.busy = null;
        this.load();
      },
      error: () => {
        this.busy = null;
        this.toast.error('Could not update the return.');
      },
    });
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'REFUNDED': return 'bg-success-subtle text-success-emphasis';
      case 'APPROVED': return 'bg-info-subtle text-info-emphasis';
      case 'DENIED': return 'bg-danger-subtle text-danger-emphasis';
      default: return 'bg-warning-subtle text-warning-emphasis';
    }
  }
}
