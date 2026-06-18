package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.ReturnDecisionRequest;
import com.bob.ecommerceangularapp.dto.ReturnRequestView;
import com.bob.ecommerceangularapp.service.ReturnService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin returns queue: review and approve/deny (approval issues a Stripe refund when possible). */
@RestController
@RequestMapping("/api/admin/returns")
public class AdminReturnController {

    private final ReturnService returnService;

    public AdminReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @GetMapping
    public List<ReturnRequestView> list() {
        return returnService.adminList();
    }

    @PutMapping("/{id}/decision")
    public ReturnRequestView decide(@PathVariable Long id, @Valid @RequestBody ReturnDecisionRequest decision) {
        return returnService.decide(id, decision);
    }
}
