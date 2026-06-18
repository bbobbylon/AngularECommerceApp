package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ReturnRequestRepository;
import com.bob.ecommerceangularapp.dto.CreateReturnRequest;
import com.bob.ecommerceangularapp.dto.ReturnDecisionRequest;
import com.bob.ecommerceangularapp.dto.ReturnRequestView;
import com.bob.ecommerceangularapp.entity.Customer;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.ReturnRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests (no Spring/DB, Stripe not configured) for the returns lifecycle. */
class ReturnServiceTest {

    private final ReturnRequestRepository returnRepository = mock(ReturnRequestRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ReturnService service = new ReturnService(returnRepository, orderRepository, "");

    private Order order() {
        Customer c = new Customer();
        c.setEmail("buyer@example.com");
        Order o = new Order();
        o.setId(7L);
        o.setOrderTrackingNumber("TRK-1");
        o.setTotalPrice(new BigDecimal("42.00"));
        o.setCustomer(c);
        return o;
    }

    @Test
    void createReturn_opensRequestWhenEmailMatches() {
        when(orderRepository.findByOrderTrackingNumber("TRK-1")).thenReturn(Optional.of(order()));
        when(returnRepository.findByOrderId(7L)).thenReturn(List.of());
        when(returnRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReturnRequestView view = service.createReturn(
                new CreateReturnRequest("TRK-1", "BUYER@example.com", "Too small"));

        assertThat(view.status()).isEqualTo("REQUESTED");
        assertThat(view.orderId()).isEqualTo(7L);
        assertThat(view.customerEmail()).isEqualTo("buyer@example.com");
    }

    @Test
    void createReturn_rejectsEmailMismatch() {
        when(orderRepository.findByOrderTrackingNumber("TRK-1")).thenReturn(Optional.of(order()));

        assertThatThrownBy(() -> service.createReturn(
                new CreateReturnRequest("TRK-1", "someone@else.com", "x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createReturn_rejectsDuplicateOpenReturn() {
        when(orderRepository.findByOrderTrackingNumber("TRK-1")).thenReturn(Optional.of(order()));
        ReturnRequest existing = new ReturnRequest();
        existing.setStatus("REQUESTED");
        when(returnRepository.findByOrderId(7L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createReturn(
                new CreateReturnRequest("TRK-1", "buyer@example.com", "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already open");
    }

    @Test
    void decide_approveWithoutStripeMarksApprovedWithRefundAmount() {
        ReturnRequest rr = new ReturnRequest();
        rr.setId(3L);
        rr.setOrderId(7L);
        rr.setStatus("REQUESTED");
        when(returnRepository.findById(3L)).thenReturn(Optional.of(rr));
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order()));
        when(returnRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReturnRequestView view = service.decide(3L, new ReturnDecisionRequest("APPROVE", null, "ok"));

        assertThat(view.status()).isEqualTo("APPROVED"); // no Stripe → manual refund
        assertThat(view.refundAmount()).isEqualByComparingTo("42.00"); // defaulted to order total
        assertThat(view.refunded()).isFalse();
    }

    @Test
    void decide_denySetsDenied() {
        ReturnRequest rr = new ReturnRequest();
        rr.setId(4L);
        rr.setStatus("REQUESTED");
        when(returnRepository.findById(4L)).thenReturn(Optional.of(rr));
        when(returnRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReturnRequestView view = service.decide(4L, new ReturnDecisionRequest("DENY", null, "Out of policy"));

        assertThat(view.status()).isEqualTo("DENIED");
    }
}
