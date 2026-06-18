import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Stripe, StripeCardElement, loadStripe } from '@stripe/stripe-js';

import { ConfigService } from '../../services/config.service';
import { CouponService } from '../../services/coupon.service';
import { Country } from '../../common/country';
import { Order } from '../../common/order';
import { OrderItem } from '../../common/order-item';
import { PaymentInfo } from '../../common/payment-info';
import { Purchase } from '../../common/purchase';
import { State } from '../../common/state';
import { CartService } from '../../services/cart.service';
import { CheckoutService, ShippingMethodView } from '../../services/checkout.service';
import { LoyaltyService } from '../../services/loyalty.service';
import { ReferralService } from '../../services/referral.service';
import { Luv2ShopFormService } from '../../services/luv2shop-form.service';
import { Luv2ShopValidators } from '../../validators/luv2shop-validators';

@Component({
  selector: 'app-checkout',
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './checkout.html',
})
export class Checkout implements OnInit, AfterViewInit {

  checkoutFormGroup!: FormGroup;

  totalPrice = 0;
  totalQuantity = 0;
  isSubmitting = false;
  errorMessage = '';

  // coupon / discount
  private couponService = inject(CouponService);
  couponCode = '';
  appliedCode = '';
  discount = 0;
  couponError = '';
  applyingCoupon = false;

  // shipping + tax (server-computed quote — single source of truth for the grand total)
  shippingMethods: ShippingMethodView[] = [];
  selectedShippingCode = '';
  shippingAmount = 0;
  taxAmount = 0;
  private quoteTotal: number | null = null;

  // gift card (store credit applied against the grand total)
  giftCardCode = '';
  appliedGiftCode = '';
  giftCardBalance = 0;
  giftCardError = '';
  applyingGift = false;

  // loyalty / rewards points (store credit; 1 point = $0.01)
  private loyalty = inject(LoyaltyService);
  private referral = inject(ReferralService);
  useLoyalty = false;
  loyaltyBalance = 0;
  loyaltyTier = '';
  loyaltyError = '';
  loadingLoyalty = false;

  get grandTotal(): number {
    return this.quoteTotal != null ? this.quoteTotal : Math.max(0, this.totalPrice - this.discount);
  }

  /** Gift-card store credit actually applied: capped at the order total. */
  get giftApplied(): number {
    return this.appliedGiftCode ? Math.min(this.giftCardBalance, this.grandTotal) : 0;
  }

  /** Points redeemed against the remaining total (1 point = $0.01), capped by balance + remainder. */
  get pointsApplied(): number {
    if (!this.useLoyalty) {
      return 0;
    }
    const remainderCents = Math.round(Math.max(0, this.grandTotal - this.giftApplied) * 100);
    return Math.min(this.loyaltyBalance, remainderCents);
  }

  get loyaltyApplied(): number {
    return this.pointsApplied / 100;
  }

  /** What the customer pays by card after all store credit. */
  get amountDue(): number {
    return Math.max(0, this.grandTotal - this.giftApplied - this.loyaltyApplied);
  }

  countries: Country[] = [];
  shippingAddressStates: State[] = [];
  billingAddressStates: State[] = [];

  // Stripe (Milestone 5)
  private config = inject(ConfigService);
  private stripe: Stripe | null = null;
  private cardElement?: StripeCardElement;
  cardError = '';
  private paymentInfo = new PaymentInfo();
  /** Captured after a successful card payment so the order can later be refunded on a return. */
  private paymentIntentId = '';

  /** True only when a real Stripe publishable key is configured (runtime config.json or environment). */
  get stripeConfigured(): boolean {
    return this.config.stripeConfigured;
  }

  constructor(
    private formBuilder: FormBuilder,
    private cartService: CartService,
    private checkoutService: CheckoutService,
    private formService: Luv2ShopFormService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.reviewCartDetails();

    this.checkoutFormGroup = this.formBuilder.group({
      customer: this.formBuilder.group({
        firstName: ['', [Validators.required, Validators.minLength(2), Luv2ShopValidators.notOnlyWhitespace]],
        lastName: ['', [Validators.required, Validators.minLength(2), Luv2ShopValidators.notOnlyWhitespace]],
        email: ['', [Validators.required, Validators.pattern('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$')]],
      }),
      shippingAddress: this.buildAddressGroup(),
      billingAddress: this.buildAddressGroup(),
      subscribeToNewsletter: [true],
    });

    this.formService.getCountries().subscribe(data => (this.countries = data));

    this.checkoutService.getShippingMethods().subscribe(methods => {
      this.shippingMethods = methods;
      if (methods.length && !this.selectedShippingCode) {
        this.selectedShippingCode = methods[0].code;
      }
      this.recomputeQuote();
    });
  }

  /** Re-fetch the authoritative totals (discount + shipping + tax + total) from the server. */
  recomputeQuote(): void {
    if (this.totalPrice <= 0) {
      this.quoteTotal = null;
      this.shippingAmount = 0;
      this.taxAmount = 0;
      return;
    }
    const country = (this.shippingCountry?.value as Country)?.name;
    const state = (this.shippingState?.value as State)?.name;
    this.checkoutService.quote({
      subtotal: this.totalPrice,
      country,
      state,
      couponCode: this.appliedCode || undefined,
      shippingMethodCode: this.selectedShippingCode || undefined,
    }).subscribe({
      next: q => {
        this.discount = q.discount;
        this.shippingAmount = q.shippingAmount;
        this.taxAmount = q.taxAmount;
        this.quoteTotal = q.total;
      },
      error: () => { /* keep the last-known totals on a transient failure */ },
    });
  }

  selectShipping(code: string): void {
    this.selectedShippingCode = code;
    this.recomputeQuote();
  }

  /** Snapshot the cart (by email) so it can be recovered if checkout isn't completed. */
  captureAbandonedCart(): void {
    const email = (this.email?.value ?? '').trim();
    if (!email || !email.includes('@') || this.totalQuantity === 0) {
      return;
    }
    const summary = this.cartService.cartItems
      .map(i => `${i.quantity}× ${i.name}`).join(', ').slice(0, 900);
    this.checkoutService.captureAbandonedCart({
      email, itemCount: this.totalQuantity, total: this.totalPrice, summary,
    }).subscribe({ next: () => { /* best-effort */ }, error: () => { /* best-effort */ } });
  }

  async ngAfterViewInit(): Promise<void> {
    if (!this.stripeConfigured) {
      return; // demo mode: no card element to mount
    }
    this.stripe = await loadStripe(this.config.stripePublishableKey);
    if (!this.stripe) {
      return;
    }
    const elements = this.stripe.elements();
    this.cardElement = elements.create('card', { hidePostalCode: true });
    this.cardElement.mount('#card-element');
    this.cardElement.on('change', event => {
      this.cardError = event.error ? event.error.message : '';
    });
  }

  private buildAddressGroup(): FormGroup {
    return this.formBuilder.group({
      street: ['', [Validators.required, Validators.minLength(2), Luv2ShopValidators.notOnlyWhitespace]],
      city: ['', [Validators.required, Validators.minLength(2), Luv2ShopValidators.notOnlyWhitespace]],
      state: ['', Validators.required],
      country: ['', Validators.required],
      zipCode: ['', [Validators.required, Validators.minLength(2), Luv2ShopValidators.notOnlyWhitespace]],
    });
  }

  private reviewCartDetails(): void {
    this.cartService.totalQuantity.subscribe(data => (this.totalQuantity = data));
    this.cartService.totalPrice.subscribe(data => (this.totalPrice = data));
    this.cartService.computeCartTotals();
  }

  // ----- convenience getters for template validation -----
  get firstName() { return this.checkoutFormGroup.get('customer.firstName'); }
  get lastName() { return this.checkoutFormGroup.get('customer.lastName'); }
  get email() { return this.checkoutFormGroup.get('customer.email'); }

  get shippingStreet() { return this.checkoutFormGroup.get('shippingAddress.street'); }
  get shippingCity() { return this.checkoutFormGroup.get('shippingAddress.city'); }
  get shippingState() { return this.checkoutFormGroup.get('shippingAddress.state'); }
  get shippingCountry() { return this.checkoutFormGroup.get('shippingAddress.country'); }
  get shippingZipCode() { return this.checkoutFormGroup.get('shippingAddress.zipCode'); }

  get billingStreet() { return this.checkoutFormGroup.get('billingAddress.street'); }
  get billingCity() { return this.checkoutFormGroup.get('billingAddress.city'); }
  get billingState() { return this.checkoutFormGroup.get('billingAddress.state'); }
  get billingCountry() { return this.checkoutFormGroup.get('billingAddress.country'); }
  get billingZipCode() { return this.checkoutFormGroup.get('billingAddress.zipCode'); }

  applyCoupon(): void {
    const code = this.couponCode.trim();
    if (!code) {
      return;
    }
    this.applyingCoupon = true;
    this.couponError = '';
    this.couponService.validate(code, this.totalPrice).subscribe({
      next: res => {
        if (res.valid) {
          this.appliedCode = res.code;
          this.couponError = '';
        } else {
          this.discount = 0;
          this.appliedCode = '';
          this.couponError = res.message;
        }
        this.applyingCoupon = false;
        this.recomputeQuote();
      },
      error: () => {
        this.couponError = 'Could not check that code. Please try again.';
        this.applyingCoupon = false;
      },
    });
  }

  removeCoupon(): void {
    this.discount = 0;
    this.appliedCode = '';
    this.couponCode = '';
    this.couponError = '';
    this.recomputeQuote();
  }

  applyGiftCard(): void {
    const code = this.giftCardCode.trim();
    if (!code) {
      return;
    }
    this.applyingGift = true;
    this.giftCardError = '';
    this.checkoutService.checkGiftCard(code).subscribe({
      next: res => {
        if (res.valid) {
          this.appliedGiftCode = res.code;
          this.giftCardBalance = res.balance;
          this.giftCardError = '';
        } else {
          this.appliedGiftCode = '';
          this.giftCardBalance = 0;
          this.giftCardError = res.message;
        }
        this.applyingGift = false;
      },
      error: () => {
        this.giftCardError = 'Could not check that gift card. Please try again.';
        this.applyingGift = false;
      },
    });
  }

  removeGiftCard(): void {
    this.appliedGiftCode = '';
    this.giftCardBalance = 0;
    this.giftCardCode = '';
    this.giftCardError = '';
  }

  /** Looks up the customer's points (by the email entered above) and applies them as store credit. */
  applyLoyalty(): void {
    const email = (this.email?.value ?? '').trim();
    if (!email) {
      this.loyaltyError = 'Enter your email above first so we can find your rewards.';
      return;
    }
    this.loadingLoyalty = true;
    this.loyaltyError = '';
    this.loyalty.summary(email).subscribe({
      next: s => {
        this.loyaltyBalance = s.balance;
        this.loyaltyTier = s.tier;
        if (s.balance > 0) {
          this.useLoyalty = true;
        } else {
          this.loyaltyError = 'No points available on this account yet.';
        }
        this.loadingLoyalty = false;
      },
      error: () => {
        this.loyaltyError = 'Could not load your rewards. Please try again.';
        this.loadingLoyalty = false;
      },
    });
  }

  removeLoyalty(): void {
    this.useLoyalty = false;
    this.loyaltyError = '';
  }

  copyShippingToBilling(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.checkoutFormGroup.controls['billingAddress']
        .setValue(this.checkoutFormGroup.controls['shippingAddress'].value);
      this.billingAddressStates = this.shippingAddressStates;
    } else {
      this.checkoutFormGroup.controls['billingAddress'].reset();
      this.billingAddressStates = [];
    }
  }

  onCountryChange(addressType: 'shippingAddress' | 'billingAddress'): void {
    const formGroup = this.checkoutFormGroup.get(addressType);
    const countryCode = formGroup?.value.country.code;

    this.formService.getStates(countryCode).subscribe(data => {
      if (addressType === 'shippingAddress') {
        this.shippingAddressStates = data;
      } else {
        this.billingAddressStates = data;
      }
      formGroup?.get('state')?.setValue(data[0] ?? '');
      if (addressType === 'shippingAddress') {
        this.recomputeQuote(); // destination changed → tax may change
      }
    });
  }

  /** Bound to the shipping state selector so tax recalculates when the destination changes. */
  onShippingStateChange(): void {
    this.recomputeQuote();
  }

  onSubmit(): void {
    if (this.checkoutFormGroup.invalid) {
      this.checkoutFormGroup.markAllAsTouched();
      return;
    }
    this.errorMessage = '';

    // Skip the card step when Stripe isn't configured (demo) OR store credit covers the whole order.
    if (!this.stripeConfigured || this.amountDue <= 0) {
      this.isSubmitting = true;
      this.placeOrder();
      return;
    }

    if (this.cardError || !this.stripe || !this.cardElement) {
      return;
    }

    this.isSubmitting = true;

    this.paymentInfo.amount = Math.round(this.amountDue * 100);
    this.paymentInfo.currency = 'USD';
    this.paymentInfo.receiptEmail = this.email?.value;

    this.checkoutService.createPaymentIntent(this.paymentInfo).subscribe({
      next: response => {
        this.paymentIntentId = response.id;
        this.stripe!.confirmCardPayment(
          response.client_secret,
          {
            payment_method: {
              card: this.cardElement!,
              billing_details: {
                email: this.email?.value,
                name: `${this.firstName?.value} ${this.lastName?.value}`,
                address: {
                  line1: this.shippingStreet?.value,
                  city: this.shippingCity?.value,
                  state: (this.shippingState?.value as State)?.name,
                  postal_code: this.shippingZipCode?.value,
                  country: (this.shippingCountry?.value as Country)?.code,
                },
              },
            },
          },
          { handleActions: false },
        ).then(result => {
          if (result.error) {
            this.isSubmitting = false;
            this.errorMessage = result.error.message ?? 'Payment failed. Please check your card details.';
          } else {
            this.placeOrder();
          }
        });
      },
      error: err => {
        this.isSubmitting = false;
        this.errorMessage = `There was an error creating the payment: ${err.message}`;
      },
    });
  }

  private placeOrder(): void {
    const order = new Order(this.totalQuantity, this.grandTotal);
    order.shippingAmount = this.shippingAmount;
    order.taxAmount = this.taxAmount;
    order.shippingMethod = this.selectedShippingCode;
    const orderItems: OrderItem[] = this.cartService.cartItems.map(item => new OrderItem(item));

    const purchase = new Purchase();
    purchase.customer = this.checkoutFormGroup.controls['customer'].value;
    purchase.shippingAddress = this.checkoutFormGroup.controls['shippingAddress'].value;
    purchase.billingAddress = this.checkoutFormGroup.controls['billingAddress'].value;

    purchase.shippingAddress.state = (this.shippingState?.value as State).name;
    purchase.shippingAddress.country = (this.shippingCountry?.value as Country).name;
    purchase.billingAddress.state = (this.billingState?.value as State).name;
    purchase.billingAddress.country = (this.billingCountry?.value as Country).name;

    purchase.order = order;
    purchase.orderItems = orderItems;
    purchase.subscribeToNewsletter = !!this.checkoutFormGroup.get('subscribeToNewsletter')?.value;
    // Always send the subtotal + chosen method so the server recomputes shipping + tax (+ coupon).
    purchase.subtotal = this.totalPrice;
    purchase.shippingMethodCode = this.selectedShippingCode || undefined;
    purchase.paymentIntentId = this.paymentIntentId || undefined;
    purchase.giftCardCode = this.appliedGiftCode || undefined;
    purchase.pointsToRedeem = this.pointsApplied || undefined;
    purchase.referralCode = this.referral.getStoredCode() || undefined;
    if (this.appliedCode && this.discount > 0) {
      purchase.couponCode = this.appliedCode;
    }

    this.checkoutService.placeOrder(purchase).subscribe({
      next: response => this.completeOrder(response.orderTrackingNumber),
      error: err => {
        this.isSubmitting = false;
        this.errorMessage = `There was an error placing the order: ${err.message}`;
      },
    });
  }

  private completeOrder(trackingNumber: string): void {
    const summary = {
      totalQuantity: this.totalQuantity,
      totalPrice: this.grandTotal,
      shippingAmount: this.shippingAmount,
      taxAmount: this.taxAmount,
      discount: this.discount,
      items: this.cartService.cartItems.map(item => ({
        name: item.name,
        imageUrl: item.imageUrl,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
      })),
    };

    this.cartService.clear();
    this.referral.clear(); // a referral applies to the first order only
    this.checkoutFormGroup.reset();
    this.router.navigate(['/order-confirmation', trackingNumber], { state: { summary } });
  }
}
