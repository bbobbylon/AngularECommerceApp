package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.config.TenantContext;
import com.bob.ecommerceangularapp.dto.AnalyticsSummary;
import com.bob.ecommerceangularapp.dto.RevenuePoint;
import com.bob.ecommerceangularapp.dto.StatusCount;
import com.bob.ecommerceangularapp.dto.TopProduct;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.bob.ecommerceangularapp.entity.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sales analytics for the admin dashboard (roadmap #18) — revenue trend, best sellers, order-status
 * mix, and top-line KPIs. Purely a read-side aggregation over the existing Order/OrderItem/Product
 * data (no new entity/migration): every number here is derived, never stored.
 */
@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public AnalyticsService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /** One point per day for the last {@code days} days (oldest first), zero-filled on quiet days. */
    public List<RevenuePoint> revenueOverTime(int days) {
        int window = clamp(days, 1, 365);
        Map<LocalDate, List<Order>> byDay = ordersSince(window).stream()
                .collect(Collectors.groupingBy(o -> toLocalDate(o.getDateCreated())));

        LocalDate today = LocalDate.now();
        List<RevenuePoint> points = new ArrayList<>();
        for (int i = window - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<Order> dayOrders = byDay.getOrDefault(day, List.of());
            BigDecimal revenue = dayOrders.stream()
                    .map(Order::getTotalPrice).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            points.add(new RevenuePoint(day.toString(), revenue, dayOrders.size()));
        }
        return points;
    }

    /** Best sellers by revenue over the last {@code days} days, aggregated from order line items. */
    public List<TopProduct> topProducts(int days, int limit) {
        int window = clamp(days, 1, 365);
        int cappedLimit = clamp(limit, 1, 50);

        Map<Long, Accumulator> byProduct = new HashMap<>();
        for (Order order : ordersSince(window)) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProductId() == null) {
                    continue;
                }
                Accumulator acc = byProduct.computeIfAbsent(item.getProductId(), id -> new Accumulator());
                acc.units += item.getQuantity();
                if (item.getUnitPrice() != null) {
                    acc.revenue = acc.revenue.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }

        Map<Long, String> names = productRepository.findAllById(byProduct.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        return byProduct.entrySet().stream()
                .map(e -> new TopProduct(e.getKey(), names.getOrDefault(e.getKey(), "Unknown product"),
                        e.getValue().units, e.getValue().revenue))
                .sorted(Comparator.comparing(TopProduct::revenue).reversed())
                .limit(cappedLimit)
                .toList();
    }

    /** All-time order count by status (missing/blank status reported as "UNKNOWN"). */
    public List<StatusCount> orderStatusBreakdown() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        o -> Optional.ofNullable(o.getStatus()).filter(s -> !s.isBlank()).orElse("UNKNOWN"),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new StatusCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatusCount::count).reversed())
                .toList();
    }

    /** Average order value (all time) plus this-month-vs-last-month revenue growth. */
    public AnalyticsSummary summary() {
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue(TenantContext.currentTenantId());
        BigDecimal averageOrderValue = totalOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        LocalDate today = LocalDate.now();
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);

        BigDecimal revenueThisMonth = BigDecimal.ZERO;
        BigDecimal revenueLastMonth = BigDecimal.ZERO;
        for (Order order : orderRepository.findByDateCreatedGreaterThanEqual(toDate(lastMonthStart))) {
            BigDecimal price = order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice();
            LocalDate day = toLocalDate(order.getDateCreated());
            if (!day.isBefore(thisMonthStart)) {
                revenueThisMonth = revenueThisMonth.add(price);
            } else {
                revenueLastMonth = revenueLastMonth.add(price);
            }
        }

        Double growthPercent = revenueLastMonth.signum() == 0
                ? null
                : revenueThisMonth.subtract(revenueLastMonth)
                        .divide(revenueLastMonth, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        return new AnalyticsSummary(averageOrderValue, revenueThisMonth, revenueLastMonth, growthPercent);
    }

    private List<Order> ordersSince(int days) {
        return orderRepository.findByDateCreatedGreaterThanEqual(toDate(LocalDate.now().minusDays(days - 1L)));
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static final class Accumulator {
        private long units;
        private BigDecimal revenue = BigDecimal.ZERO;
    }
}
