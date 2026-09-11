package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.CreateReturnRequest;
import com.bob.ecommerceangularapp.dto.ReturnRequestView;
import com.bob.ecommerceangularapp.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Customer-facing returns: open a return (email must match the order) and list your returns. */
@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping
    public ResponseEntity<ReturnRequestView> create(@Valid @RequestBody CreateReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(returnService.createReturn(request));
    }

    @GetMapping
    public List<ReturnRequestView> myReturns(@RequestParam String email) {
        return returnService.listForEmail(email);
    }
}
