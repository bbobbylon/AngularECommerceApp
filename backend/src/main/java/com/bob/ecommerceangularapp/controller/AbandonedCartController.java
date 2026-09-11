package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AbandonedCartRequest;
import com.bob.ecommerceangularapp.service.AbandonedCartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Captures a cart snapshot at checkout so it can be recovered by email if not completed. */
@RestController
@RequestMapping("/api/abandoned-cart")
public class AbandonedCartController {

    private final AbandonedCartService service;

    public AbandonedCartController(AbandonedCartService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> capture(@Valid @RequestBody AbandonedCartRequest request) {
        service.capture(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
