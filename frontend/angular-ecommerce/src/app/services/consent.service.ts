import { Injectable, effect, signal } from '@angular/core';

import { ConsentDecision, PrivacyService } from './privacy.service';

const VISITOR_ID_KEY = 'consentVisitorId';

export type ConsentCategory = 'functional' | 'analytics' | 'marketing';
export type ConsentSource = 'BANNER' | 'ACCOUNT';

export interface ConsentChoice {
  functional: boolean;
  analytics: boolean;
  marketing: boolean;
}

/**
 * Signal-based consent state for cookie/storage categories (roadmap #24). The banner shows once
 * per visitor per policy version — bumping `app.privacy.policy-version` on the backend re-prompts
 * everyone, since a decision made against an older policy isn't consent to a new one.
 *
 * `necessary` storage (theme, currency, language, favorites) is never gated here — only
 * `functional` (recently-viewed) and `marketing` (referral capture) actually check `isAllowed()`.
 */
@Injectable({ providedIn: 'root' })
export class ConsentService {

  private readonly _visible = signal(false);
  private readonly _panelOpen = signal(false);
  private readonly _choice = signal<ConsentDecision | null>(null);
  private readonly _prefillEmail = signal<string | null>(null);

  readonly visible = this._visible.asReadonly();
  readonly panelOpen = this._panelOpen.asReadonly();
  readonly choice = this._choice.asReadonly();
  readonly prefillEmail = this._prefillEmail.asReadonly();

  readonly visitorId = this.loadOrCreateVisitorId();

  private policyVersion = '';

  constructor(private privacyService: PrivacyService) {
    this.init();
    effect(() => {
      document.documentElement.classList.toggle('cookie-consent-open', this._visible() && !this._panelOpen());
    });
  }

  private init(): void {
    this.privacyService.config().subscribe(config => {
      this.policyVersion = config.policyVersion;
      this.privacyService.currentConsent(this.visitorId).subscribe(existing => {
        this._choice.set(existing);
        const stale = !existing || (!!this.policyVersion && existing.policyVersion !== this.policyVersion);
        this._visible.set(stale);
      });
    });
  }

  isAllowed(category: ConsentCategory): boolean {
    const choice = this._choice();
    if (!choice) {
      return false;
    }
    return choice[category];
  }

  openPreferences(source: ConsentSource, email?: string | null): void {
    this._prefillEmail.set(email ?? null);
    this._panelOpen.set(true);
    this._visible.set(true);
  }

  closePreferences(): void {
    this._panelOpen.set(false);
  }

  acceptAll(): void {
    this.save({ functional: true, analytics: true, marketing: true }, 'BANNER');
  }

  rejectAll(): void {
    this.save({ functional: false, analytics: false, marketing: false }, 'BANNER');
  }

  saveCustom(selection: ConsentChoice, source: ConsentSource = 'ACCOUNT', email?: string | null): void {
    this.save(selection, source, email);
  }

  private save(selection: ConsentChoice, source: ConsentSource, email?: string | null): void {
    this.privacyService.recordConsent({
      visitorId: this.visitorId,
      email: email ?? this._prefillEmail() ?? null,
      functional: selection.functional,
      analytics: selection.analytics,
      marketing: selection.marketing,
      source,
    }).subscribe(decision => {
      this._choice.set(decision);
      this._visible.set(false);
      this._panelOpen.set(false);
    });
  }

  private loadOrCreateVisitorId(): string {
    try {
      const existing = localStorage.getItem(VISITOR_ID_KEY);
      if (existing) {
        return existing;
      }
      const generated = crypto.randomUUID();
      localStorage.setItem(VISITOR_ID_KEY, generated);
      return generated;
    } catch {
      return crypto.randomUUID();
    }
  }
}
