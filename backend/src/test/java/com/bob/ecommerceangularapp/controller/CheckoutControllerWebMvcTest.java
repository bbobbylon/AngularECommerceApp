package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.Purchase;
import com.bob.ecommerceangularapp.dto.PurchaseResponse;
import com.bob.ecommerceangularapp.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test: drives {@code POST /api/checkout/purchase} through a standalone MockMvc so the
 * request mapping and JSON (de)serialization are genuinely exercised, with the service mocked.
 * Standalone setup keeps this fast and dependency-proof (no Spring context, no security filters).
 */
class CheckoutControllerWebMvcTest {

    private final CheckoutService checkoutService = mock(CheckoutService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CheckoutController(checkoutService)).build();
    }

    @Test
    void purchase_deserializesBodyAndReturnsTrackingNumberAsJson() throws Exception {
        when(checkoutService.placeOrder(any(Purchase.class)))
                .thenReturn(new PurchaseResponse("TRACK-12345"));

        String body = """
                {
                  "customer": {"firstName": "A", "lastName": "B", "email": "a@b.com"},
                  "shippingAddress": {"street": "1 St", "city": "Town", "state": "CA", "country": "US", "zipCode": "90210"},
                  "billingAddress":  {"street": "1 St", "city": "Town", "state": "CA", "country": "US", "zipCode": "90210"},
                  "order": {"totalPrice": 10.00, "totalQuantity": 1},
                  "orderItems": [{"quantity": 1, "unitPrice": 10.00, "productId": 1, "imageUrl": "x"}]
                }
                """;

        mockMvc.perform(post("/api/checkout/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderTrackingNumber").value("TRACK-12345"));
    }
}
