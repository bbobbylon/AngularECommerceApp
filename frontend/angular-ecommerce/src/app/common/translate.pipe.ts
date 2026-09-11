import { Pipe, PipeTransform, inject } from '@angular/core';

import { I18nService } from '../services/i18n.service';

/** Translates an i18n key to the active language. Impure so it re-renders when the language changes. */
@Pipe({ name: 't', standalone: true, pure: false })
export class TranslatePipe implements PipeTransform {

  private i18n = inject(I18nService);

  transform(key: string): string {
    return this.i18n.t(key);
  }
}
