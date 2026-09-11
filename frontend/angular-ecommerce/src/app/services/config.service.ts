import { Injectable } from '@angular/core';

import { environment } from '../../environments/environment';

/**
 * Loads optional runtime config from /config.json (written by run.sh from your .env) before the
 * app bootstraps. This lets you set the Stripe publishable key without editing source or rebuilding —
 * fill in .env, run ./run.sh, done. Falls back to the build-time environment when absent.
 */
@Injectable({ providedIn: 'root' })
export class ConfigService {

  private runtime: { stripePublishableKey?: string } = {};

  async load(): Promise<void> {
    try {
      const res = await fetch('config.json', { cache: 'no-store' });
      if (res.ok) {
        this.runtime = await res.json();
      }
    } catch {
      // no runtime config present — fall back to the build-time environment values
    }
  }

  get stripePublishableKey(): string {
    const key = this.runtime.stripePublishableKey?.trim();
    return key && key.length > 0 ? key : environment.stripePublishableKey;
  }

  /** True only when a real Stripe publishable key is set (not the shipped placeholder). */
  get stripeConfigured(): boolean {
    const key = this.stripePublishableKey;
    return key.startsWith('pk_') && !key.includes('REPLACE');
  }
}
