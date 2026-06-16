import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

interface Section {
  heading: string;
  body: string;
}

interface InfoContent {
  icon: string;
  title: string;
  intro: string;
  sections: Section[];
}

/**
 * Renders a static long-form info/legal page from a content map, selected by route `data.page`.
 * One component backs Shipping & Returns, Privacy, and Terms — same layout, different copy.
 */
@Component({
  selector: 'app-info-page',
  templateUrl: './info-page.html',
})
export class InfoPage implements OnInit {

  content!: InfoContent;
  private route = inject(ActivatedRoute);

  private readonly pages: Record<string, InfoContent> = {
    shipping: {
      icon: 'fa-truck-fast',
      title: 'Shipping & Returns',
      intro: 'Fast, tracked delivery and a no-stress 30-day return window.',
      sections: [
        { heading: 'Shipping options', body: 'Standard shipping (3–5 business days) is free on orders over $50, otherwise a flat rate is shown at checkout. Expedited delivery is available where offered.' },
        { heading: 'Order tracking', body: 'Every order gets a confirmation email with a tracking number. You can also view orders under My account → My orders.' },
        { heading: 'Returns', body: 'Return any item within 30 days of delivery in its original condition for a full refund to your original payment method. Start a return by contacting support with your order number.' },
        { heading: 'Refunds', body: 'Once we receive your return, refunds are issued within 5–7 business days. Original shipping is non-refundable unless the item arrived damaged or incorrect.' },
      ],
    },
    privacy: {
      icon: 'fa-shield-halved',
      title: 'Privacy Policy',
      intro: 'A plain-language summary of what we collect and why.',
      sections: [
        { heading: 'What we collect', body: 'Account and order details (name, email, shipping/billing address) and email preferences. Payment card data is collected directly by Stripe and never stored on our servers.' },
        { heading: 'How we use it', body: 'To process orders, provide support, and — only if you opt in — send marketing email. You can change email preferences or unsubscribe at any time.' },
        { heading: 'Sharing', body: 'We share data only with the processors needed to run the store (e.g. Stripe for payments, our email provider for delivery). We do not sell personal data.' },
        { heading: 'Your choices', body: 'Manage email preferences in Account settings, unsubscribe from any marketing email, or contact us to request access to or deletion of your data.' },
        { heading: 'Note', body: 'This is a demo store; the policy is illustrative. Replace it with a lawyer-reviewed policy before processing real customer data.' },
      ],
    },
    terms: {
      icon: 'fa-file-contract',
      title: 'Terms of Service',
      intro: 'The basics of using Luv2Shop.',
      sections: [
        { heading: 'Using the store', body: 'By placing an order you confirm the information you provide is accurate and that you are authorized to use the payment method.' },
        { heading: 'Pricing & availability', body: 'Prices and stock can change without notice. We may cancel and refund an order if an item is mispriced or unavailable.' },
        { heading: 'Orders & payment', body: 'Orders are confirmed once payment is authorized. Sale prices apply while the promotion lasts and cannot be combined unless stated.' },
        { heading: 'Limitation of liability', body: 'The store is provided “as is”. To the extent permitted by law, we are not liable for indirect or incidental damages arising from its use.' },
        { heading: 'Note', body: 'This is a demo store; these terms are illustrative. Replace them with reviewed terms before going live.' },
      ],
    },
  };

  ngOnInit(): void {
    const key = (this.route.snapshot.data['page'] as string) ?? 'shipping';
    this.content = this.pages[key] ?? this.pages['shipping'];
  }
}
