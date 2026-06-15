import { AbstractControl, ValidationErrors } from '@angular/forms';

export class Luv2ShopValidators {

  /** Fails validation when a field contains only whitespace. */
  static notOnlyWhitespace(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (value != null && value.trim().length === 0) {
      return { notOnlyWhitespace: true };
    }
    return null;
  }
}
