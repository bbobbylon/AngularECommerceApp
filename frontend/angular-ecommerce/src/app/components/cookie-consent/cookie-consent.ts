import { Component, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ConsentService } from '../../services/consent.service';

/**
 * Global cookie/storage consent banner + preferences panel (roadmap #24). Mounted once in `App`
 * so it's reachable from every route; `ConsentService` owns all visibility/state so the account
 * settings "Cookie preferences" link can reopen the same panel via `openPreferences('ACCOUNT')`.
 */
@Component({
  selector: 'app-cookie-consent',
  imports: [FormsModule, RouterLink],
  template: `
    @if (consentService.visible() && !consentService.panelOpen()) {
      <div class="cookie-consent" role="dialog" aria-live="polite" aria-label="Cookie consent">
        <div class="cookie-consent-body">
          <strong>We value your privacy</strong>
          <span>
            We use essential cookies to run the site, and — with your permission — optional ones to
            remember your preferences and personalize your experience. See our
            <a routerLink="/privacy">Privacy Policy</a> for details.
          </span>
        </div>
        <div class="cookie-consent-actions">
          <button type="button" class="btn btn-sm btn-outline-secondary" (click)="customize()">Customize</button>
          <button type="button" class="btn btn-sm btn-outline-secondary" (click)="rejectAll()">Reject all</button>
          <button type="button" class="btn btn-sm btn-primary" (click)="acceptAll()">Accept all</button>
        </div>
      </div>
    }

    @if (consentService.panelOpen()) {
      <div class="cookie-consent-overlay" (click)="close()">
        <div class="cookie-consent-panel" role="dialog" aria-modal="true" aria-label="Cookie preferences" (click)="$event.stopPropagation()">
          <div class="cookie-consent-panel-header">
            <strong>Cookie preferences</strong>
            <button type="button" class="cookie-consent-dismiss" aria-label="Close" (click)="close()">
              <i class="fa-solid fa-xmark"></i>
            </button>
          </div>
          <p class="cookie-consent-panel-intro">
            Choose which optional categories you're comfortable with. You can change this anytime from
            "Cookie preferences" in the footer or your account settings.
          </p>

          <div class="cookie-consent-category">
            <div class="cookie-consent-category-text">
              <strong>Necessary</strong>
              <span>Always on — required for cart, checkout, sign-in, and site preferences to work.</span>
            </div>
            <input type="checkbox" checked disabled aria-label="Necessary (always on)">
          </div>

          <div class="cookie-consent-category">
            <div class="cookie-consent-category-text">
              <strong>Functional</strong>
              <span>Remembers items you've recently viewed so we can show them back to you.</span>
            </div>
            <input type="checkbox" [(ngModel)]="functional" aria-label="Functional">
          </div>

          <div class="cookie-consent-category">
            <div class="cookie-consent-category-text">
              <strong>Analytics</strong>
              <span>Helps us understand how the store is used so we can improve it.</span>
            </div>
            <input type="checkbox" [(ngModel)]="analytics" aria-label="Analytics">
          </div>

          <div class="cookie-consent-category">
            <div class="cookie-consent-category-text">
              <strong>Marketing</strong>
              <span>Lets a referral link credit whoever shared it with you.</span>
            </div>
            <input type="checkbox" [(ngModel)]="marketing" aria-label="Marketing">
          </div>

          <div class="cookie-consent-panel-actions">
            <button type="button" class="btn btn-sm btn-outline-secondary" (click)="rejectAll()">Reject all</button>
            <button type="button" class="btn btn-sm btn-primary" (click)="saveCustom()">Save preferences</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class CookieConsent {

  functional = false;
  analytics = false;
  marketing = false;

  constructor(readonly consentService: ConsentService) {
    effect(() => {
      if (this.consentService.panelOpen()) {
        const choice = this.consentService.choice();
        this.functional = choice?.functional ?? false;
        this.analytics = choice?.analytics ?? false;
        this.marketing = choice?.marketing ?? false;
      }
    });
  }

  customize(): void {
    this.consentService.openPreferences('BANNER');
  }

  acceptAll(): void {
    this.consentService.acceptAll();
  }

  rejectAll(): void {
    this.consentService.rejectAll();
  }

  close(): void {
    this.consentService.closePreferences();
  }

  saveCustom(): void {
    this.consentService.saveCustom({
      functional: this.functional,
      analytics: this.analytics,
      marketing: this.marketing,
    });
  }
}
