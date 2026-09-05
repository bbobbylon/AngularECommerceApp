import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AdminFaqEntry, AdminFaqEntryPayload, AdminService, SiteBanner } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

/** Admin CMS (roadmap #17): the single site-wide banner, and the FAQ list. */
@Component({
  selector: 'app-admin-content',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-content.html',
})
export class AdminContent implements OnInit {

  readonly faq = signal<AdminFaqEntry[]>([]);
  readonly loading = signal(true);
  readonly savingBanner = signal(false);
  readonly savingFaq = signal(false);

  bannerForm: SiteBanner = this.emptyBanner();
  faqForm: AdminFaqEntryPayload = this.emptyFaq();

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getBanner().subscribe({
      next: banner => this.bannerForm = { ...banner, linkUrl: banner.linkUrl ?? '', linkText: banner.linkText ?? '' },
      error: () => this.toast.error('Could not load the banner.'),
    });
    this.admin.getFaqAdmin().subscribe({
      next: entries => {
        this.faq.set(entries);
        this.loading.set(false);
        // Keep the "add entry" default sort order in step with the freshly-loaded list — computing
        // it eagerly (e.g. in a field initializer) would race ahead of this load and collide with
        // an existing entry's sortOrder.
        if (!this.faqForm.id) {
          this.faqForm.sortOrder = entries.length + 1;
        }
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load the FAQ.');
      },
    });
  }

  // ----- banner -----

  saveBanner(): void {
    if (!this.bannerForm.message.trim()) {
      this.toast.error('Banner message is required.');
      return;
    }
    this.savingBanner.set(true);
    this.admin.saveBanner({
      message: this.bannerForm.message.trim(),
      linkUrl: this.bannerForm.linkUrl?.trim() || null,
      linkText: this.bannerForm.linkText?.trim() || null,
      active: this.bannerForm.active,
    }).subscribe({
      next: banner => {
        this.bannerForm = { ...banner, linkUrl: banner.linkUrl ?? '', linkText: banner.linkText ?? '' };
        this.savingBanner.set(false);
        this.toast.success('Banner saved');
      },
      error: () => {
        this.savingBanner.set(false);
        this.toast.error('Could not save the banner.');
      },
    });
  }

  // ----- FAQ -----

  saveFaq(): void {
    if (!this.faqForm.question.trim() || !this.faqForm.answer.trim()) {
      this.toast.error('Question and answer are required.');
      return;
    }
    this.savingFaq.set(true);
    this.admin.saveFaq({
      ...this.faqForm,
      question: this.faqForm.question.trim(),
      answer: this.faqForm.answer.trim(),
    }).subscribe({
      next: () => {
        this.toast.success('FAQ entry saved');
        this.faqForm = this.emptyFaq();
        this.savingFaq.set(false);
        this.load();
      },
      error: () => {
        this.savingFaq.set(false);
        this.toast.error('Could not save the FAQ entry.');
      },
    });
  }

  editFaq(entry: AdminFaqEntry): void {
    this.faqForm = { ...entry };
  }

  cancelFaqEdit(): void {
    this.faqForm = this.emptyFaq();
  }

  deleteFaq(entry: AdminFaqEntry): void {
    if (!confirm(`Delete the FAQ entry "${entry.question}"?`)) {
      return;
    }
    this.admin.deleteFaq(entry.id).subscribe({
      next: () => { this.toast.success('Deleted'); this.load(); },
      error: () => this.toast.error('Could not delete.'),
    });
  }

  private emptyBanner(): SiteBanner {
    return { id: null, message: '', linkUrl: '', linkText: '', active: true };
  }

  private emptyFaq(): AdminFaqEntryPayload {
    return { id: null, question: '', answer: '', sortOrder: this.faq().length + 1, active: true };
  }
}
