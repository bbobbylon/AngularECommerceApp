import { Injectable, computed, signal } from '@angular/core';

/**
 * Wishlist / favorites. Favorited product ids live in localStorage and are exposed as a signal,
 * so the navbar badge, product cards, and the favorites page all stay in sync reactively.
 */
@Injectable({ providedIn: 'root' })
export class FavoritesService {

  private readonly storageKey = 'favorites';
  private readonly _ids = signal<number[]>(this.load());

  readonly ids = this._ids.asReadonly();
  readonly count = computed(() => this._ids().length);

  isFavorite(id: number): boolean {
    return this._ids().includes(id);
  }

  toggle(id: number): void {
    const ids = this._ids();
    this.commit(ids.includes(id) ? ids.filter(x => x !== id) : [...ids, id]);
  }

  remove(id: number): void {
    this.commit(this._ids().filter(x => x !== id));
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
