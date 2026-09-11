import { Pipe, PipeTransform, inject } from '@angular/core';

import { CurrencyService } from '../services/currency.service';

/**
 * Formats a USD amount in the customer's chosen display currency. Impure so it re-renders when the
 * selected currency changes (the value to format rarely changes, but the currency can at any time).
 * Use on storefront browse/cart prices; the checkout/settlement screens keep the built-in USD pipe.
 */
@Pipe({ name: 'money', standalone: true, pure: false })
export class MoneyPipe implements PipeTransform {

  private currency = inject(CurrencyService);

  transform(usd: number | null | undefined): string {
    return this.currency.format(usd);
  }
}
