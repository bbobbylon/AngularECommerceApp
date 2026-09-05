package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AnalyticsSummary;
import com.bob.ecommerceangularapp.dto.RevenuePoint;
import com.bob.ecommerceangularapp.dto.StatusCount;
import com.bob.ecommerceangularapp.dto.TopProduct;
import com.bob.ecommerceangularapp.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin sales analytics (roadmap #18): revenue trend, best sellers, order-status mix, top-line KPIs. */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/revenue")
    public List<RevenuePoint> revenue(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.revenueOverTime(days);
    }

    @GetMapping("/top-products")
    public List<TopProduct> topProducts(@RequestParam(defaultValue = "30") int days,
                                        @RequestParam(defaultValue = "5") int limit) {
        return analyticsService.topProducts(days, limit);
    }

    @GetMapping("/order-status")
    public List<StatusCount> orderStatus() {
        return analyticsService.orderStatusBreakdown();
    }

    @GetMapping("/summary")
    public AnalyticsSummary summary() {
        return analyticsService.summary();
    }
}
