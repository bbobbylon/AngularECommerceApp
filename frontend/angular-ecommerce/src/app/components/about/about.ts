import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NewsletterSignup } from '../newsletter-signup/newsletter-signup';

@Component({
  selector: 'app-about',
  imports: [RouterLink, NewsletterSignup],
  templateUrl: './about.html',
})
export class About {}
