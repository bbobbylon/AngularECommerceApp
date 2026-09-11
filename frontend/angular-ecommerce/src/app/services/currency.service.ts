import { Injectable, signal } from '@angular/core';

export interface CurrencyInfo {
  code: string;
  symbol: string;
  /** Units of this currency per 1 USD (demo static rates). */
  rate: number;
  /** Fraction digits to display (e.g. JPY shows 0). */
  decimals: number;
}

/**
 * Display-currency conversion for the storefront. Catalog/cart prices (stored in USD) are converted
 * for display in the customer's chosen currency; the **checkout settles in USD** (kept simple — the
 * payment screens stay in USD and say so). The choice is a signal (so the impure `money` pipe reacts)
 * and is persisted to localStorage. Rates are static demo values — swap in a live FX feed for prod.
 */
@Injectable({ providedIn: 'root' })
export class CurrencyService {

  static readonly CURRENCIES: CurrencyInfo[] = [
    { code: 'USD', symbol: '$', rate: 1, decimals: 2 },
    { code: 'EUR', symbol: '€', rate: 0.92, decimals: 2 },
    { code: 'GBP', symbol: '£', rate: 0.79, decimals: 2 },
    { code: 'CAD', symbol: 'C$', rate: 1.36, decimals: 2 },
    { code: 'AUD', symbol: 'A$', rate: 1.52, decimals: 2 },
    { code: 'JPY', symbol: '¥', rate: 157, decimals: 0 },
  ];

  private readonly storageKey = 'displayCurrency';
  readonly current = signal<CurrencyInfo>(this.load());

  readonly currencies = CurrencyService.CURRENCIES;

  setCurrency(code: string): void {
    const found = CurrencyService.CURRENCIES.find(c => c.code === code);
    if (found) {
      this.current.set(found);
      try {
        localStorage.setItem(this.storageKey, found.code);
      } catch {
        /* ignore */
      }
    }
  }

  /** Converts a USD amount and formats it with the active currency's symbol + precision. */
  format(usd: number | null | undefined): string {
    const c = this.current();
    const value = (usd ?? 0) * c.rate;
    return c.symbol + value.toLocaleString(undefined, {
      minimumFractionDigits: c.decimals,
      maximumFractionDigits: c.decimals,
    });
  }

  private load(): CurrencyInfo {
    try {
      const code = localStorage.getItem(this.storageKey);
      return CurrencyService.CURRENCIES.find(c => c.code === code) ?? CurrencyService.CURRENCIES[0];
    } catch {
      return CurrencyService.CURRENCIES[0];
    }
  }
}
