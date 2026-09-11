import { Component, HostListener, isDevMode, signal } from '@angular/core';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  readonly userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

/**
 * Surfaces the browser's native "Add to Home Screen" capability (roadmap #12 — PWA) once Chrome/Edge
 * fires `beforeinstallprompt`. Suppressed if the app is already running standalone, or if the visitor
 * dismissed it recently — re-offered after a cooldown rather than never again.
 */
@Component({
  selector: 'app-install-prompt',
  imports: [],
  template: `
    @if (visible()) {
      <div class="install-prompt" role="dialog" aria-label="Install Luv2Shop">
        <i class="fa-solid fa-download install-prompt-icon"></i>
        <div class="install-prompt-body">
          <strong>Install Luv2Shop</strong>
          <span>Add it to your home screen for a faster, app-like experience.</span>
        </div>
        <button type="button" class="btn btn-sm btn-primary" (click)="install()">Install</button>
        <button type="button" class="install-prompt-dismiss" aria-label="Dismiss" (click)="dismiss()">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>
    }
  `,
})
export class InstallPrompt {

  private readonly storageKey = 'installPromptDismissedAt';
  private readonly cooldownMs = 14 * 24 * 60 * 60 * 1000;

  private deferredEvent: BeforeInstallPromptEvent | null = null;
  readonly visible = signal(false);

  @HostListener('window:beforeinstallprompt', ['$event'])
  onBeforeInstallPrompt(event: Event): void {
    event.preventDefault();
    if (isDevMode() || this.isStandalone() || !this.cooldownElapsed()) {
      return;
    }
    this.deferredEvent = event as BeforeInstallPromptEvent;
    this.visible.set(true);
  }

  @HostListener('window:appinstalled')
  onInstalled(): void {
    this.visible.set(false);
    this.deferredEvent = null;
  }

  async install(): Promise<void> {
    if (!this.deferredEvent) {
      return;
    }
    this.visible.set(false);
    await this.deferredEvent.prompt();
    this.deferredEvent = null;
  }

  dismiss(): void {
    this.visible.set(false);
    try {
      localStorage.setItem(this.storageKey, String(Date.now()));
    } catch {
      // storage unavailable (e.g. private mode) — just hide it for this session
    }
  }

  private isStandalone(): boolean {
    return window.matchMedia?.('(display-mode: standalone)').matches
      || (window.navigator as { standalone?: boolean }).standalone === true;
  }

  private cooldownElapsed(): boolean {
    try {
      const raw = localStorage.getItem(this.storageKey);
      return !raw || Date.now() - Number(raw) > this.cooldownMs;
    } catch {
      return true;
    }
  }
}
