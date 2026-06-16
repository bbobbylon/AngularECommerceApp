import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface QA {
  q: string;
  a: string;
}

@Component({
  selector: 'app-faq',
  imports: [RouterLink],
  templateUrl: './faq.html',
})
export class Faq {
  readonly faqs: QA[] = [
    { q: 'How long does shipping take?', a: 'Standard shipping is 3–5 business days. Orders over $50 ship free; expedited options are shown at checkout.' },
    { q: 'What is your return policy?', a: 'Returns are accepted within 30 days of delivery, in original condition. See Shipping & Returns for the full details.' },
    { q: 'Which payment methods do you accept?', a: 'All major credit and debit cards via Stripe. Your card details are entered directly into Stripe and never touch our servers.' },
    { q: 'Do you ship internationally?', a: 'We ship to the United States, Canada, Brazil, Germany, India and Australia, with more regions on the way.' },
    { q: 'How do I track my order?', a: 'You\'ll get an order confirmation email with a tracking number, and you can view past orders under My account → My orders.' },
    { q: 'How do I manage marketing emails?', a: 'Update your preferences anytime in Account settings, or use the unsubscribe link at the bottom of any marketing email.' },
  ];
}
