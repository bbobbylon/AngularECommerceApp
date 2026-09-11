import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { Stripe, StripeCardElement, loadStripe } from '@stripe/stripe-js';

import { AdminService, BillingAccount, BillingInvoice } from '../../../services/admin.service';
import { ConfigService } from '../../../services/config.service';
import { ToastService } from '../../../services/toast.service';

/**
 * Tenant-facing billing view (roadmap #22) — read-only plan/status/renewal + invoice history, plus
 * self-service card management. The plan itself is SuperAdmin-assigned (see the Platform Tenants
 * page); this page only manages the card on file, reusing {@code account-settings.ts}'s exact Stripe
 * Elements SetupIntent flow, re-pointed at {@code /api/admin/billing/*}.
 */
@Component({
  selector: 'app-admin-billing',
  imports: [CommonModule, NgbPaginationModule],
  templateUrl: './admin-billing.html',
})
export class AdminBilling implements OnInit {

  readonly account = signal<BillingAccount | null>(null);
  readonly invoices = signal<BillingInvoice[]>([]);
  readonly loading = signal(true);

  pageNumber = 1;
  pageSize = 10;
  totalElements = 0;

  readonly cardSetupOpen = signal(false);
  readonly savingCard = signal(false);
  cardSetupError = '';
  private stripe: Stripe | null = null;
  private cardSetupElement?: StripeCardElement;
  private setupClientSecret = '';

  private adminService = inject(AdminService);
  private config = inject(ConfigService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.adminService.getBillingAccount().subscribe({
      next: account => { this.account.set(account); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Could not load billing information.'); },
    });
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.adminService.getBillingInvoices(this.pageNumber - 1, this.pageSize).subscribe({
      next: res => {
        this.invoices.set(res.content);
        this.totalElements = res.totalElements;
      },
      error: () => this.toast.error('Could not load invoice history.'),
    });
  }

  async startAddCard(): Promise<void> {
    this.adminService.createBillingSetupIntent().subscribe(async res => {
      if (!res.enabled || !res.clientSecret) {
        this.toast.error('Saving cards needs Stripe configured — see docs/STRIPE.md.');
        return;
      }
      this.setupClientSecret = res.clientSecret;
      this.cardSetupError = '';
      this.cardSetupOpen.set(true);
      this.stripe = await loadStripe(this.config.stripePublishableKey);
      // Mount after the element div renders.
      setTimeout(() => {
        if (!this.stripe) {
          return;
        }
        this.cardSetupElement = this.stripe.elements().create('card', { hidePostalCode: true });
        this.cardSetupElement.mount('#billing-card-setup-element');
        this.cardSetupElement.on('change', e => (this.cardSetupError = e.error ? e.error.message : ''));
      });
    });
  }

  saveCard(): void {
    if (!this.stripe || !this.cardSetupElement) {
      return;
    }
    this.savingCard.set(true);
    this.stripe.confirmCardSetup(this.setupClientSecret, { payment_method: { card: this.cardSetupElement } })
      .then(result => {
        if (result.error || !result.setupIntent?.payment_method) {
          this.savingCard.set(false);
          this.cardSetupError = result.error?.message ?? 'Could not save the card.';
          return;
        }
        const pmId = String(result.setupIntent.payment_method);
        this.adminService.recordBillingPaymentMethod(pmId).subscribe({
          next: () => {
            this.toast.success('Card saved');
            this.savingCard.set(false);
            this.cardSetupOpen.set(false);
            this.load();
          },
          error: () => { this.savingCard.set(false); this.toast.error('Could not save the card.'); },
        });
      });
  }

  cancelAddCard(): void {
    this.cardSetupOpen.set(false);
    this.cardSetupError = '';
  }
}
