import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

/**
 * Light/dark theme. Applies a `data-theme` attribute on <html> (CSS variables do the rest),
 * persists the choice, and falls back to the OS preference on first visit.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {

  readonly theme = signal<Theme>('light');

  constructor() {
    const saved = localStorage.getItem('theme') as Theme | null;
    const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    this.apply(saved ?? (prefersDark ? 'dark' : 'light'));
  }

  toggle(): void {
    this.apply(this.theme() === 'dark' ? 'light' : 'dark');
  }

  private apply(theme: Theme): void {
    this.theme.set(theme);
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }
}
