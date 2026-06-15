import { FormControl } from '@angular/forms';
import { Luv2ShopValidators } from './luv2shop-validators';

describe('Luv2ShopValidators.notOnlyWhitespace', () => {
  it('returns an error for whitespace-only values', () => {
    expect(Luv2ShopValidators.notOnlyWhitespace(new FormControl('   '))).toEqual({
      notOnlyWhitespace: true,
    });
  });

  it('passes for a non-empty value', () => {
    expect(Luv2ShopValidators.notOnlyWhitespace(new FormControl('Bob'))).toBeNull();
  });

  it('passes for a value with surrounding whitespace but real content', () => {
    expect(Luv2ShopValidators.notOnlyWhitespace(new FormControl('  Bob  '))).toBeNull();
  });

  it('passes for a null value (left to Validators.required)', () => {
    expect(Luv2ShopValidators.notOnlyWhitespace(new FormControl(null))).toBeNull();
  });
});
