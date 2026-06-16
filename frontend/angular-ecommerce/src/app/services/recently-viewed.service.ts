import { Injectable, signal } from '@angular/core';

/**
 * Tracks the products a visitor has recently opened (most-recent-first), persisted in localStorage
 * and exposed as a signal. Powers the "Recently viewed" strips on the home and product pages.
 */
@Injectable({ providedIn: 'root' })
export class RecentlyViewedService {

  private readonly storageKey = 'recentlyViewed';
  private readonly max = 12;
  private readonly _ids = signal<number[]>(this.load());

  readonly ids = this._ids.asReadonly();

  /** Records a product view, moving it to the front and capping the list length. */
  record(id: number): void {
    if (!Number.isFinite(id)) {
      return;
    }
    const next = [id, ...this._ids().filter(x => x !== id)].slice(0, this.max);
    this.commit(next);
  }

  private commit(ids: number[]): void {
    this._ids.set(ids);
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(ids));
    } catch {
      // storage unavailable (e.g. private mode) — keep the in-memory state
    }
  }

  private load(): number[] {
    try {
      const raw = localStorage.getItem(this.storageKey);
      return raw ? (JSON.parse(raw) as number[]) : [];
    } catch {
      return [];
    }
  }
}
