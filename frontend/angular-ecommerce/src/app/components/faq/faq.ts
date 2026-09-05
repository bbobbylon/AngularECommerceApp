import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ContentService, FaqEntry } from '../../services/content.service';
import { SeoService } from '../../services/seo.service';

@Component({
  selector: 'app-faq',
  imports: [RouterLink],
  templateUrl: './faq.html',
})
export class Faq implements OnInit {
  private seo = inject(SeoService);
  private content = inject(ContentService);

  readonly faqs = signal<FaqEntry[]>([]);

  ngOnInit(): void {
    this.seo.update({
      title: 'Frequently Asked Questions',
      description: 'Shipping, returns, payments, and account answers for Luv2Shop.',
    });
    this.content.getFaq().subscribe(entries => this.faqs.set(entries));
  }
}
