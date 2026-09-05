package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.ShipmentView;
import com.bob.ecommerceangularapp.service.FulfillmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Customer-facing shipment tracking (roadmap #20), keyed the same way as returns: the order's
 * tracking number plus a matching email — the service refuses a mismatch without disclosing whether
 * the order exists. Backs the shipment details shown under the order-history timeline.
 */
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final FulfillmentService fulfillmentService;

    public ShipmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping
    public List<ShipmentView> track(@RequestParam String orderTrackingNumber, @RequestParam String email) {
        return fulfillmentService.trackShipments(orderTrackingNumber, email);
    }
}
