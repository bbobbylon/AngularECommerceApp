package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dto.OrderHistoryView;
import com.bob.ecommerceangularapp.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, tenant-scoped "recent orders" fallback for {@code order-history.html} when no one is
 * signed in (so the page still renders something in demo mode — see {@code OrderHistoryService}).
 * Replaces the old raw {@code GET /api/orders?sort=dateCreated,desc&size=50}, which had no tenant
 * predicate at all and mixed every tenant's order history together (roadmap #21 gap; see
 * {@code MyDataRestConfig} for where that collection GET was closed off).
 */
@RestController
@RequestMapping("/api/order-history")
public class OrderHistoryController {

    private final OrderRepository orderRepository;

    public OrderHistoryController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/recent")
    public PageResponse<OrderHistoryView> recent(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(orderRepository
                .findAllByTenantIdOrderByDateCreatedDesc(TenantContext.currentTenantId(), PageRequest.of(page, size))
                .map(OrderHistoryView::of));
    }
}
