package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ProductRepository;
import com.bob.ecommerceangularapp.dto.AnalyticsSummary;
import com.bob.ecommerceangularapp.dto.RevenuePoint;
import com.bob.ecommerceangularapp.dto.StatusCount;
import com.bob.ecommerceangularapp.dto.TopProduct;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.OrderItem;
import com.bob.ecommerceangularapp.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit test (mocked repositories) for the sales analytics aggregations (roadmap #18). */
class AnalyticsServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final AnalyticsService service = new AnalyticsService(orderRepository, productRepository);

    @Test
    void revenueOverTime_zeroFillsQuietDaysAndSumsBusyOnes() {
        Order today1 = orderOn(LocalDate.now(), "50.00");
        Order today2 = orderOn(LocalDate.now(), "25.00");
        when(orderRepository.findByTenantIdAndDateCreatedGreaterThanEqual(any(), any())).thenReturn(List.of(today1, today2));

        List<RevenuePoint> points = service.revenueOverTime(7);

        assertThat(points).hasSize(7);
        RevenuePoint last = points.get(points.size() - 1);
        assertThat(last.date()).isEqualTo(LocalDate.now().toString());
        assertThat(last.revenue()).isEqualByComparingTo("75.00");
        assertThat(last.orderCount()).isEqualTo(2);
        assertThat(points.get(0).revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(points.get(0).orderCount()).isZero();
    }

    @Test
    void topProducts_aggregatesUnitsAndRevenueAcrossOrdersAndSortsDescending() {
        Order order1 = orderWithItems(orderItem(1L, 2, "10.00"), orderItem(2L, 1, "40.00"));
        Order order2 = orderWithItems(orderItem(1L, 3, "10.00"));
        when(orderRepository.findByTenantIdAndDateCreatedGreaterThanEqual(any(), any())).thenReturn(List.of(order1, order2));
        when(productRepository.findAllById(any())).thenReturn(List.of(
                Product.builder().id(1L).name("Mug").build(),
                Product.builder().id(2L).name("Book").build()));

        List<TopProduct> top = service.topProducts(30, 5);

        assertThat(top).hasSize(2);
        // Book: 1 * 40 = 40 revenue; Mug: 5 * 10 = 50 revenue -> Mug should lead.
        assertThat(top.get(0).name()).isEqualTo("Mug");
        assertThat(top.get(0).unitsSold()).isEqualTo(5);
        assertThat(top.get(0).revenue()).isEqualByComparingTo("50.00");
        assertThat(top.get(1).name()).isEqualTo("Book");
        assertThat(top.get(1).revenue()).isEqualByComparingTo("40.00");
    }

    @Test
    void topProducts_fallsBackToUnknownForADeletedProduct() {
        Order order = orderWithItems(orderItem(99L, 1, "5.00"));
        when(orderRepository.findByTenantIdAndDateCreatedGreaterThanEqual(any(), any())).thenReturn(List.of(order));
        when(productRepository.findAllById(any())).thenReturn(List.of());

        List<TopProduct> top = service.topProducts(30, 5);

        assertThat(top).hasSize(1);
        assertThat(top.get(0).name()).isEqualTo("Unknown product");
    }

    @Test
    void topProducts_respectsTheLimit() {
        Order order = orderWithItems(orderItem(1L, 1, "10.00"), orderItem(2L, 1, "20.00"), orderItem(3L, 1, "30.00"));
        when(orderRepository.findByTenantIdAndDateCreatedGreaterThanEqual(any(), any())).thenReturn(List.of(order));
        when(productRepository.findAllById(any())).thenReturn(List.of(
                Product.builder().id(1L).name("A").build(),
                Product.builder().id(2L).name("B").build(),
                Product.builder().id(3L).name("C").build()));

        assertThat(service.topProducts(30, 2)).hasSize(2);
    }

    @Test
    void orderStatusBreakdown_groupsAndCountsByStatusTreatingBlankAsUnknown() {
        Order shipped1 = new Order();
        shipped1.setStatus("SHIPPED");
        Order shipped2 = new Order();
        shipped2.setStatus("SHIPPED");
        Order noStatus = new Order();
        noStatus.setStatus(null);
        when(orderRepository.findByTenantId(any())).thenReturn(List.of(shipped1, shipped2, noStatus));

        List<StatusCount> breakdown = service.orderStatusBreakdown();

        assertThat(breakdown).hasSize(2);
        assertThat(breakdown.get(0).status()).isEqualTo("SHIPPED");
        assertThat(breakdown.get(0).count()).isEqualTo(2);
        assertThat(breakdown.get(1).status()).isEqualTo("UNKNOWN");
        assertThat(breakdown.get(1).count()).isEqualTo(1);
    }

    @Test
    void summary_computesAverageOrderValueAndMonthOverMonthGrowth() {
        when(orderRepository.countByTenantId(any())).thenReturn(4L);
        when(orderRepository.sumTotalRevenue(any())).thenReturn(new BigDecimal("400.00"));

        LocalDate thisMonthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate lastMonthDay = thisMonthStart.minusDays(1);
        Order thisMonthOrder = orderOn(thisMonthStart, "150.00");
        Order lastMonthOrder = orderOn(lastMonthDay, "100.00");
        when(orderRepository.findByTenantIdAndDateCreatedGreaterThanEqual(any(), any()))
                .thenReturn(List.of(thisMonthOrder, lastMonthOrder));

        AnalyticsSummary summary = service.summary();

        assertThat(summary.averageOrderValue()).isEqualByComparingTo("100.00");
        assertThat(summary.revenueThisMonth()).isEqualByComparingTo("150.00");
        assertThat(summary.revenueLastMonth()).isEqualByComparingTo("100.00");
        assertThat(summary.growthPercent()).isEqualTo(50.0);
    }

    @Test
    void summary_growthIsNullWhenThereWasNoRevenueLastMonthToCompareAgainst() {
        when(orderRepository.countByTenantId(any())).thenReturn(1L);
        when(orderRepository.sumTotalRevenue(any())).thenReturn(new BigDecimal("20.00"));
        when(orderRepository.findByTenantIdAndDateCreatedGreaterThanEqual(any(), any()))
                .thenReturn(List.of(orderOn(LocalDate.now(), "20.00")));

        AnalyticsSummary summary = service.summary();

        assertThat(summary.revenueLastMonth()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.growthPercent()).isNull();
    }

    @Test
    void summary_averageOrderValueIsZeroWhenThereAreNoOrders() {
        when(orderRepository.countByTenantId(any())).thenReturn(0L);
        when(orderRepository.sumTotalRevenue(any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.findByTenantIdAndDateCreatedGreaterThanEqual(any(), any())).thenReturn(List.of());

        assertThat(service.summary().averageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static Order orderOn(LocalDate date, String totalPrice) {
        Order order = new Order();
        order.setTotalPrice(new BigDecimal(totalPrice));
        order.setDateCreated(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        order.setOrderItems(new HashSet<>());
        return order;
    }

    private static Order orderWithItems(OrderItem... items) {
        Order order = orderOn(LocalDate.now(), "0.00");
        Set<OrderItem> set = new HashSet<>(List.of(items));
        order.setOrderItems(set);
        return order;
    }

    private static OrderItem orderItem(Long productId, int quantity, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }
}
