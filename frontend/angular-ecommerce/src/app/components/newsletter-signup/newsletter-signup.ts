import { Component, Input, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { NewsletterService } from '../../services/newsletter.service';
import { ToastService } from '../../services/toast.service';

/**
 * Reusable newsletter signup. Two looks via [variant]:
 *  - 'band'   : full-width dark promo band (home page CTA)
 *  - 'inline' : compact form (footer)
 */
@Component({
  selector: 'app-newsletter-signup',
  imports: [FormsModule],
  templateUrl: './newsletter-signup.html',
})
export class NewsletterSignup {

  @Input() variant: 'band' | 'inline' = 'inline';

  email = '';
  readonly submitting = signal(false);

  private newsletter = inject(NewsletterService);
  private toast = inject(ToastService);

  submit(): void {
    const email = this.email.trim();
    if (!email || !email.includes('@')) {
      this.toast.error('Please enter a valid email address.');
      return;
    }
    this.submitting.set(true);
    this.newsletter.subscribe(email).subscribe({
      next: res => {
        this.toast.success(res?.message ?? "You're subscribed!");
        this.email = '';
        this.submitting.set(false);
      },
      error: () => {
        this.toast.error('Could not subscribe right now. Please try again.');
        this.submitting.set(false);
      },
    });
  }
}
