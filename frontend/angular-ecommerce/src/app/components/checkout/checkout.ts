import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Stripe, StripeCardElement, loadStripe } from '@stripe/stripe-js';

import { environment } from '../../../environments/environment';
import { Country } from '../../common/country';
import { Order } from '../../common/order';
import { OrderItem } from '../../common/order-item';
import { PaymentInfo } from '../../common/payment-info';
import { Purchase } from '../../common/purchase';
import { State } from '../../common/state';
import { CartService } from '../../services/cart.service';
import { CheckoutService } from '../../services/checkout.service';
import { Luv2ShopFormService } from '../../services/luv2shop-form.service';
import { Luv2ShopValidators } from '../../validators/luv2shop-validators';

@Component({
  selector: 'app-checkout',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout.html',
})
export class Checkout implements OnInit, AfterViewInit {

  checkoutFormGroup!: FormGroup;

  totalPrice = 0;
  totalQuantity = 0;
  isSubmitting = false;

  countries: Country[] = [];
  shippingAddressStates: State[] = [];
  billingAddressStates: State[] = [];

  // Stripe (Milestone 5)
  private stripe: Stripe | null = null;
  private cardElement?: StripeCardElement;
  cardError = '';
  private paymentInfo = new PaymentInfo();

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
    });

    this.formService.getCountries().subscribe(data => (this.countries = data));
  }

  async ngAfterViewInit(): Promise<void> {
    this.stripe = await loadStripe(environment.stripePublishableKey);
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
    });
  }

  onSubmit(): void {
    if (this.checkoutFormGroup.invalid) {
      this.checkoutFormGroup.markAllAsTouched();
      return;
    }
    if (this.cardError || !this.stripe || !this.cardElement) {
      return;
    }

    this.isSubmitting = true;

    this.paymentInfo.amount = Math.round(this.totalPrice * 100);
    this.paymentInfo.currency = 'USD';
    this.paymentInfo.receiptEmail = this.email?.value;

    this.checkoutService.createPaymentIntent(this.paymentInfo).subscribe({
      next: response => {
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
            alert(`Payment failed: ${result.error.message}`);
          } else {
            this.placeOrder();
          }
        });
      },
      error: err => {
        this.isSubmitting = false;
        alert(`There was an error creating the payment: ${err.message}`);
      },
    });
  }

  private placeOrder(): void {
    const order = new Order(this.totalQuantity, this.totalPrice);
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

    this.checkoutService.placeOrder(purchase).subscribe({
      next: response => this.completeOrder(response.orderTrackingNumber),
      error: err => {
        this.isSubmitting = false;
        alert(`There was an error placing the order: ${err.message}`);
      },
    });
  }

  private completeOrder(trackingNumber: string): void {
    this.cartService.clear();
    this.checkoutFormGroup.reset();
    this.router.navigateByUrl(`/order-confirmation/${trackingNumber}`);
  }
}
