import { Component, HostListener, signal, ChangeDetectionStrategy } from '@angular/core';

/**
 * Floating scroll-to-top button, mounted once in `App`'s global chrome. Purely presentational —
 * shows itself past a 400px scroll offset, no service dependency.
 */
@Component({
  selector: 'app-back-to-top',
  imports: [],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    @if (visible()) {
      <button type="button" class="back-to-top" (click)="scrollTop()" aria-label="Back to top">
        <i class="fa-solid fa-arrow-up"></i>
      </button>
    }
  `,
})
export class BackToTop {
  readonly visible = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    this.visible.set(window.scrollY > 400);
  }

  scrollTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
