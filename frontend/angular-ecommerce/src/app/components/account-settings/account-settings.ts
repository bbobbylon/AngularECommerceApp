import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { OktaAuthStateService } from '@okta/okta-angular';

import { isOktaConfigured } from '../../auth/dev-auth.guard';
import { oktaConfig } from '../../auth/okta-config';
import { AccountPreferences, AccountService } from '../../services/account.service';
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
  imports: [FormsModule, RouterLink],
  templateUrl: './account-settings.html',
})
export class AccountSettings implements OnInit {

  email = '';

  // editable fields, bound once preferences are loaded
  firstName = '';
  lastName = '';
  newsletterSubscribed = true;

  readonly prefs = signal<AccountPreferences | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly notFound = signal(false);

  // Sign-in security (MFA/OTP/passkeys) is managed by the identity provider (Okta).
  readonly oktaConfigured = isOktaConfigured();
  readonly oktaSettingsUrl = this.oktaConfigured
    ? oktaConfig.issuer.replace(/\/oauth2\/.*$/, '') + '/enduser/settings'
    : '';

  private accountService = inject(AccountService);
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

  private applyPrefs(prefs: AccountPreferences): void {
    this.prefs.set(prefs);
    this.firstName = prefs.firstName ?? '';
    this.lastName = prefs.lastName ?? '';
    this.newsletterSubscribed = prefs.newsletterSubscribed;
  }
}
