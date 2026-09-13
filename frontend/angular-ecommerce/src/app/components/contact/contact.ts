import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SeoService } from '../../services/seo.service';

/** Static contact page — copy lives in `contact.html`; this class only wires up SEO tags. */
@Component({
  selector: 'app-contact',
  imports: [RouterLink],
  templateUrl: './contact.html',
})
export class Contact implements OnInit {
  private seo = inject(SeoService);

  ngOnInit(): void {
    this.seo.update({
      title: 'Contact Us',
      description: 'Get in touch with the Luv2Shop team.',
    });
  }
}
