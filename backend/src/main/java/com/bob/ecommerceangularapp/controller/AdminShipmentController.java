package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.CreateShipmentRequest;
import com.bob.ecommerceangularapp.dto.FulfillmentOption;
import com.bob.ecommerceangularapp.dto.ShipmentView;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.FulfillmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin shipping actions (roadmap #20): fulfill an order from a warehouse and move the shipment
 * through its lifecycle. Like order-status updates and return decisions, these are reachable by the
 * RBAC {@code OrderManager} role, not just {@code Admin} — see the request-matchers in
 * {@code SecurityConfig}. Warehouse configuration stays Admin-only in
 * {@link AdminFulfillmentController}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminShipmentController {

    private final FulfillmentService fulfillmentService;
    private final AuditLogService auditLogService;

    public AdminShipmentController(FulfillmentService fulfillmentService, AuditLogService auditLogService) {
        this.fulfillmentService = fulfillmentService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/orders/{orderId}/shipments")
    public List<ShipmentView> shipments(@PathVariable Long orderId) {
        return fulfillmentService.shipmentsForOrder(orderId);
    }

    @GetMapping("/orders/{orderId}/fulfillment-options")
    public List<FulfillmentOption> fulfillmentOptions(@PathVariable Long orderId) {
        return fulfillmentService.fulfillmentOptions(orderId);
    }

    @PostMapping("/orders/{orderId}/shipments")
    public ShipmentView createShipment(Authentication authentication, @PathVariable Long orderId,
                                       @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentView created = fulfillmentService.createShipment(orderId, request);
        auditLogService.record(authentication, "SHIPMENT_CREATE", "Shipment", String.valueOf(created.id()),
                "order=" + orderId + " warehouse=" + created.warehouseCode());
        return created;
    }

    @PutMapping("/shipments/{id}/status")
    public ShipmentView updateStatus(Authentication authentication, @PathVariable Long id,
                                     @RequestParam String status,
                                     @RequestParam(required = false) String carrier,
                                     @RequestParam(required = false) String trackingNumber) {
        ShipmentView updated = fulfillmentService.updateShipmentStatus(id, status, carrier, trackingNumber);
        auditLogService.record(authentication, "SHIPMENT_STATUS_UPDATE", "Shipment", String.valueOf(id),
                "status=" + updated.status());
        return updated;
    }
}
