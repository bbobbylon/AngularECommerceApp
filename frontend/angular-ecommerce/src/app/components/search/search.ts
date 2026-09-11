import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';

import { MoneyPipe } from '../../common/money.pipe';
import { Product, isOnSale } from '../../common/product';
import { ProductService } from '../../services/product.service';

const MIN_QUERY_LENGTH = 2;
const MAX_SUGGESTIONS = 6;
const DEBOUNCE_MS = 250;

/**
 * Header search box with as-you-type suggestions (roadmap #14). Reuses the faceted
 * /api/catalog/search endpoint with a small page size rather than a dedicated autocomplete
 * endpoint. Implements the ARIA 1.2 combobox pattern: real focus stays on the input, arrow
 * keys move a virtual selection (aria-activedescendant) over listbox options.
 */
@Component({
  selector: 'app-search',
  imports: [CommonModule, MoneyPipe, RouterLink],
  templateUrl: './search.html',
})
export class Search {
  private router = inject(Router);
  private productService = inject(ProductService);
  private host = inject(ElementRef<HTMLElement>);

  protected readonly isOnSale = isOnSale;

  readonly query = signal('');
  readonly open = signal(false);
  readonly activeIndex = signal(-1);
  readonly loading = signal(false);

  private readonly debouncedQuery = toSignal(
    toObservable(this.query).pipe(debounceTime(DEBOUNCE_MS), distinctUntilChanged()),
    { initialValue: '' },
  );

  readonly suggestions = toSignal(
    toObservable(this.debouncedQuery).pipe(
      switchMap(raw => {
        const keyword = raw.trim();
        if (keyword.length < MIN_QUERY_LENGTH) {
          this.loading.set(false);
          return of<Product[]>([]);
        }
        return this.productService.searchCatalog({ keyword, size: MAX_SUGGESTIONS, page: 0 }).pipe(
          map(page => page.content),
          catchError(() => of<Product[]>([])),
          tap(() => this.loading.set(false)),
        );
      }),
    ),
    { initialValue: [] as Product[] },
  );

  readonly showDropdown = computed(() => this.open() && this.query().trim().length >= MIN_QUERY_LENGTH);

  readonly activeDescendantId = computed(() => {
    const idx = this.activeIndex();
    if (idx < 0) {
      return null;
    }
    const items = this.suggestions();
    if (idx < items.length) {
      return 'search-option-' + items[idx].id;
    }
    return idx === items.length && items.length > 0 ? 'search-option-all' : null;
  });

  readonly resultsAnnouncement = computed(() => {
    if (!this.showDropdown()) {
      return '';
    }
    if (this.loading()) {
      return 'Searching…';
    }
    const n = this.suggestions().length;
    return n === 0
      ? `No products found for "${this.query().trim()}"`
      : `${n} suggestion${n === 1 ? '' : 's'} available`;
  });

  onInput(value: string): void {
    this.query.set(value);
    this.open.set(true);
    this.activeIndex.set(-1);
    this.loading.set(value.trim().length >= MIN_QUERY_LENGTH);
  }

  onFocus(): void {
    if (this.query().trim().length >= MIN_QUERY_LENGTH) {
      this.open.set(true);
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  close(): void {
    this.open.set(false);
    this.activeIndex.set(-1);
  }

  onKeydown(event: KeyboardEvent): void {
    const items = this.suggestions();
    const maxIndex = items.length > 0 ? items.length : -1;

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        if (!this.open()) {
          this.open.set(true);
          return;
        }
        if (maxIndex >= 0) {
          this.activeIndex.set(Math.min(this.activeIndex() + 1, maxIndex));
        }
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.activeIndex.set(Math.max(this.activeIndex() - 1, -1));
        break;
      case 'Escape':
        this.close();
        break;
      case 'Enter': {
        event.preventDefault();
        const idx = this.activeIndex();
        if (this.open() && idx >= 0 && idx < items.length) {
          this.close();
          this.router.navigate(['/products', items[idx].id]);
        } else {
          this.doSearch(this.query());
        }
        break;
      }
    }
  }

  doSearch(value: string): void {
    const keyword = value.trim();
    if (keyword.length > 0) {
      this.close();
      this.router.navigate(['/search', keyword]);
    }
  }
}
