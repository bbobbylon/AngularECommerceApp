import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SeoService } from '../../services/seo.service';
import { NewsletterSignup } from '../newsletter-signup/newsletter-signup';

/** Static "About Us" marketing page — copy lives in `about.html`; this class only wires up SEO tags. */
@Component({
  selector: 'app-about',
  imports: [RouterLink, NewsletterSignup],
  templateUrl: './about.html',
})
export class About implements OnInit {
  private seo = inject(SeoService);

  ngOnInit(): void {
    this.seo.update({
      title: 'About Us',
      description: 'The story behind Luv2Shop — a warm little online store.',
    });
  }
}
