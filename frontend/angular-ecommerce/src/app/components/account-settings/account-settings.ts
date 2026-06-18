import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { OktaAuthStateService } from '@okta/okta-angular';
import { Stripe, StripeCardElement, loadStripe } from '@stripe/stripe-js';

import { isOktaConfigured } from '../../auth/dev-auth.guard';
import { oktaConfig } from '../../auth/okta-config';
import { AccountPreferences, AccountService, SavedAddress, SavedPaymentMethod } from '../../services/account.service';
import { ConfigService } from '../../services/config.service';
import { LoyaltyService, LoyaltySummary } from '../../services/loyalty.service';
import { ReferralService, ReferralSummary } from '../../services/referral.service';
import { ToastService } from '../../services/toast.service';

/**
 * Account settings portal — view + update email preferences.
 *
 * Identity: when Okta is configured and the user is signed in, their email is pulled from the
 * id-token and preferences load automatically. In local/dev (no Okta) the user enters the email
 * they checked out with. The route is protected by the same devOrAuthGuard used for order history.
 */
@Component({
  selector: 'app-account-settings',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './account-settings.html',
})
export class AccountSettings implements OnInit {

  email = '';

  // editable fields, bound once preferences are loaded
  firstName = '';
  lastName = '';
  newsletterSubscribed = true;

  readonly prefs = signal<AccountPreferences | null>(null);
  readonly loyalty = signal<LoyaltySummary | null>(null);
  readonly referral = signal<ReferralSummary | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly notFound = signal(false);

  // address book
  readonly addresses = signal<SavedAddress[]>([]);
  addressForm: SavedAddress = this.emptyAddress();
  readonly editingAddress = signal(false);
  readonly savingAddress = signal(false);

  // saved cards
  readonly cards = signal<SavedPaymentMethod[]>([]);
  readonly cardSetupOpen = signal(false);
  readonly savingCard = signal(false);
  cardSetupError = '';
  private stripe: Stripe | null = null;
  private cardSetupElement?: StripeCardElement;
  private setupClientSecret = '';
  private config = inject(ConfigService);

  // Sign-in security (MFA/OTP/passkeys) is managed by the identity provider (Okta).
  readonly oktaConfigured = isOktaConfigured();
  readonly oktaSettingsUrl = this.oktaConfigured
    ? oktaConfig.issuer.replace(/\/oauth2\/.*$/, '') + '/enduser/settings'
    : '';

  private accountService = inject(AccountService);
  private loyaltyService = inject(LoyaltyService);
  private referralService = inject(ReferralService);
  private authStateService = inject(OktaAuthStateService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.authStateService.authState$.subscribe(state => {
      const claimEmail = state?.isAuthenticated ? (state.idToken?.claims?.email as string) : '';
      if (claimEmail && !this.prefs()) {
        this.email = claimEmail;
        this.load();
      }
    });
  }

  load(): void {
    const email = this.email.trim();
    if (!email || !email.includes('@')) {
      this.toast.error('Enter a valid email address.');
      return;
    }
    this.loading.set(true);
    this.notFound.set(false);
    this.accountService.getPreferences(email).subscribe({
      next: prefs => {
        this.applyPrefs(prefs);
        this.loading.set(false);
        this.loyaltyService.summary(email).subscribe({
          next: summary => this.loyalty.set(summary),
          error: () => this.loyalty.set(null),
        });
        this.referralService.summary(email).subscribe({
          next: summary => this.referral.set(summary),
          error: () => this.referral.set(null),
        });
        this.loadAddressesAndCards(email);
      },
      error: () => {
        this.prefs.set(null);
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  save(): void {
    const prefs = this.prefs();
    if (!prefs) {
      return;
    }
    this.saving.set(true);
    this.accountService
      .updatePreferences({
        email: prefs.email,
        firstName: this.firstName,
        lastName: this.lastName,
        newsletterSubscribed: this.newsletterSubscribed,
      })
      .subscribe({
        next: updated => {
          this.applyPrefs(updated);
          this.toast.success('Your preferences were saved.');
          this.saving.set(false);
        },
        error: () => {
          this.toast.error('Could not save your preferences. Please try again.');
          this.saving.set(false);
        },
      });
  }

  private currentEmail(): string {
    return (this.prefs()?.email ?? this.email).trim();
  }

  private loadAddressesAndCards(email: string): void {
    this.accountService.getAddresses(email).subscribe({
      next: list => this.addresses.set(list),
      error: () => this.addresses.set([]),
    });
    this.accountService.getPaymentMethods(email).subscribe({
      next: list => this.cards.set(list),
      error: () => this.cards.set([]),
    });
  }

  // ----- address book -----

  saveAddress(): void {
    const email = this.currentEmail();
    const a = this.addressForm;
    if (!a.street?.trim() || !a.city?.trim() || !a.state?.trim() || !a.country?.trim() || !a.zipCode?.trim()) {
      this.toast.error('Please fill in the full address.');
      return;
    }
    this.savingAddress.set(true);
    this.accountService.saveAddress(email, a).subscribe({
      next: () => {
        this.toast.success('Address saved');
        this.addressForm = this.emptyAddress();
        this.editingAddress.set(false);
        this.savingAddress.set(false);
        this.accountService.getAddresses(email).subscribe(list => this.addresses.set(list));
      },
      error: () => { this.savingAddress.set(false); this.toast.error('Could not save the address.'); },
    });
  }

  editAddress(a: SavedAddress): void {
    this.addressForm = { ...a };
    this.editingAddress.set(true);
  }

  cancelAddress(): void {
    this.addressForm = this.emptyAddress();
    this.editingAddress.set(false);
  }

  deleteAddress(a: SavedAddress): void {
    if (!a.id || !confirm('Delete this address?')) {
      return;
    }
    const email = this.currentEmail();
    this.accountService.deleteAddress(email, a.id).subscribe({
      next: () => { this.toast.success('Address removed'); this.accountService.getAddresses(email).subscribe(l => this.addresses.set(l)); },
      error: () => this.toast.error('Could not remove the address.'),
    });
  }

  // ----- saved cards -----

  removeCard(c: SavedPaymentMethod): void {
    if (!confirm('Remove this saved card?')) {
      return;
    }
    const email = this.currentEmail();
    this.accountService.deletePaymentMethod(email, c.id).subscribe({
      next: () => { this.toast.success('Card removed'); this.accountService.getPaymentMethods(email).subscribe(l => this.cards.set(l)); },
      error: () => this.toast.error('Could not remove the card.'),
    });
  }

  async startAddCard(): Promise<void> {
    const email = this.currentEmail();
    this.accountService.createSetupIntent(email).subscribe(async res => {
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
        this.cardSetupElement.mount('#card-setup-element');
        this.cardSetupElement.on('change', e => (this.cardSetupError = e.error ? e.error.message : ''));
      });
    });
  }

  saveCard(): void {
    if (!this.stripe || !this.cardSetupElement) {
      return;
    }
    this.savingCard.set(true);
    const email = this.currentEmail();
    this.stripe.confirmCardSetup(this.setupClientSecret, { payment_method: { card: this.cardSetupElement } })
      .then(result => {
        if (result.error || !result.setupIntent?.payment_method) {
          this.savingCard.set(false);
          this.cardSetupError = result.error?.message ?? 'Could not save the card.';
          return;
        }
        const pmId = String(result.setupIntent.payment_method);
        this.accountService.recordPaymentMethod(email, pmId).subscribe({
          next: () => {
            this.toast.success('Card saved');
            this.savingCard.set(false);
            this.cardSetupOpen.set(false);
            this.accountService.getPaymentMethods(email).subscribe(l => this.cards.set(l));
          },
          error: () => { this.savingCard.set(false); this.toast.error('Could not save the card.'); },
        });
      });
  }

  cancelAddCard(): void {
    this.cardSetupOpen.set(false);
    this.cardSetupError = '';
  }

  private emptyAddress(): SavedAddress {
    return { id: null, label: '', recipientName: '', street: '', city: '', state: '', country: '', zipCode: '', defaultAddress: false };
  }

  referralLink(code: string): string {
    return `${window.location.origin}/products?ref=${code}`;
  }

  copyReferral(code: string): void {
    navigator.clipboard?.writeText(this.referralLink(code)).then(
      () => this.toast.success('Referral link copied!'),
      () => this.toast.error('Could not copy — select and copy the link manually.'),
    );
  }

  private applyPrefs(prefs: AccountPreferences): void {
    this.prefs.set(prefs);
    this.firstName = prefs.firstName ?? '';
    this.lastName = prefs.lastName ?? '';
    this.newsletterSubscribed = prefs.newsletterSubscribed;
  }
}
