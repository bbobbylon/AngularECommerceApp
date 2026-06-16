import { Component, Input } from '@angular/core';

type Star = 'full' | 'half' | 'empty';

/** Read-only star display. Use for product cards, details, and review rows. */
@Component({
  selector: 'app-star-rating',
  templateUrl: './star-rating.html',
})
export class StarRating {

  @Input() rating: number | null | undefined = 0;
  @Input() count: number | null | undefined = null;
  /** Show the "(N)" review count next to the stars. */
  @Input() showCount = false;
  /** Show the numeric average (e.g. "4.6") before the stars. */
  @Input() showValue = false;

  get value(): number {
    return this.rating ?? 0;
  }

  get stars(): Star[] {
    const r = this.value;
    const out: Star[] = [];
    for (let i = 1; i <= 5; i++) {
      if (r >= i) {
        out.push('full');
      } else if (r >= i - 0.5) {
        out.push('half');
      } else {
        out.push('empty');
      }
    }
    return out;
  }
}
