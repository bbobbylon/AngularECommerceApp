package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.StockNotificationRequest;
import com.bob.ecommerceangularapp.service.StockNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Customer-facing "notify me when back in stock" signup. */
@RestController
@RequestMapping("/api/stock-notifications")
public class StockNotificationController {

    private final StockNotificationService service;

    public StockNotificationController(StockNotificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> subscribe(@Valid @RequestBody StockNotificationRequest request) {
        service.subscribe(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
