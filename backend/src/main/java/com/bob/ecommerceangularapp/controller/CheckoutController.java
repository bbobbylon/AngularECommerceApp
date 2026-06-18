package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.PaymentInfo;
import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.bob.ecommerceangularapp.dto.QuoteRequest;
import com.bob.ecommerceangularapp.dto.QuoteResponse;
import com.bob.ecommerceangularapp.dto.ShippingMethodView;
import com.bob.ecommerceangularapp.service.CheckoutService;
import com.bob.ecommerceangularapp.service.TaxShippingService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final TaxShippingService taxShippingService;

    public CheckoutController(CheckoutService checkoutService, TaxShippingService taxShippingService) {
        this.checkoutService = checkoutService;
        this.taxShippingService = taxShippingService;
    }

    @PostMapping("/purchase")
    public PurchaseResponse placeOrder(@RequestBody Purchase purchase) {
        return checkoutService.placeOrder(purchase);
    }

    /** Active shipping options for the checkout selector. */
    @GetMapping("/shipping-methods")
    public List<ShippingMethodView> shippingMethods() {
        return taxShippingService.listShippingMethods();
    }

    /** Live totals breakdown (discount + shipping + tax + total) for the cart/region/method. */
    @PostMapping("/quote")
    public QuoteResponse quote(@RequestBody QuoteRequest request) {
        return taxShippingService.quote(request);
    }

    @PostMapping("/payment-intent")
    public ResponseEntity<String> createPaymentIntent(@RequestBody PaymentInfo paymentInfo) throws StripeException {
        PaymentIntent paymentIntent = checkoutService.createPaymentIntent(paymentInfo);
        return new ResponseEntity<>(paymentIntent.toJson(), HttpStatus.OK);
    }
}
