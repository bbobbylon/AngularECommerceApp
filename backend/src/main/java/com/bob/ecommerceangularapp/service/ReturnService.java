package com.bob.ecommerceangularapp.service;

import com.bob.ecommerceangularapp.dao.OrderRepository;
import com.bob.ecommerceangularapp.dao.ReturnRequestRepository;
import com.bob.ecommerceangularapp.dto.CreateReturnRequest;
import com.bob.ecommerceangularapp.dto.ReturnDecisionRequest;
import com.bob.ecommerceangularapp.dto.ReturnRequestView;
import com.bob.ecommerceangularapp.entity.Order;
import com.bob.ecommerceangularapp.entity.ReturnRequest;
import com.stripe.Stripe;
import com.stripe.model.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Returns / RMA. Customers open a return against an order (validated by matching email); admins approve
 * or deny it. Approving a card-paid order with Stripe configured issues a real refund and marks the
 * return {@code REFUNDED}; otherwise it stays {@code APPROVED} for a manual/offline refund. Degrades
 * gracefully without Stripe — same pattern as the rest of the app.
 */
@Service
public class ReturnService {

    private static final Logger log = LoggerFactory.getLogger(ReturnService.class);

    static final String REQUESTED = "REQUESTED";
    static final String APPROVED = "APPROVED";
    static final String DENIED = "DENIED";
    static final String REFUNDED = "REFUNDED";
    /** States that block opening a second return on the same order. */
    private static final Set<String> OPEN_STATES = Set.of(REQUESTED, APPROVED, REFUNDED);

    private final ReturnRequestRepository returnRepository;
    private final OrderRepository orderRepository;
    private final String stripeKey;

    public ReturnService(ReturnRequestRepository returnRepository,
                         OrderRepository orderRepository,
                         @Value("${stripe.key.secret}") String stripeKey) {
        this.returnRepository = returnRepository;
        this.orderRepository = orderRepository;
        this.stripeKey = stripeKey;
    }

    @Transactional
    public ReturnRequestView createReturn(CreateReturnRequest request) {
        Order order = orderRepository.findByOrderTrackingNumber(request.orderTrackingNumber().trim())
                .orElseThrow(() -> new IllegalArgumentException("No order found for that tracking number."));

        String orderEmail = order.getCustomer() == null ? null : order.getCustomer().getEmail();
        if (orderEmail == null || !orderEmail.equalsIgnoreCase(request.email().trim())) {
            // Don't disclose whether the order exists vs. the email is wrong.
            throw new IllegalArgumentException("That email doesn't match this order.");
        }

        boolean alreadyOpen = returnRepository.findByOrderId(order.getId()).stream()
                .anyMatch(r -> OPEN_STATES.contains(r.getStatus()));
        if (alreadyOpen) {
            throw new IllegalArgumentException("A return is already open for this order.");
        }

        ReturnRequest rr = new ReturnRequest();
        rr.setOrderId(order.getId());
        rr.setOrderTrackingNumber(order.getOrderTrackingNumber());
        rr.setCustomerEmail(orderEmail);
        rr.setReason(request.reason().trim());
        rr.setStatus(REQUESTED);
        return toView(returnRepository.save(rr));
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestView> listForEmail(String email) {
        return returnRepository.findByCustomerEmailIgnoreCaseOrderByDateCreatedDesc(email).stream()
                .map(ReturnService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestView> adminList() {
        return returnRepository.findAllByOrderByDateCreatedDesc().stream()
                .map(ReturnService::toView).toList();
    }

    @Transactional
    public ReturnRequestView decide(Long id, ReturnDecisionRequest decision) {
        ReturnRequest rr = returnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return not found: " + id));
        rr.setAdminNote(decision.adminNote());

        if ("DENY".equalsIgnoreCase(decision.action())) {
            rr.setStatus(DENIED);
            return toView(returnRepository.save(rr));
        }

        // APPROVE
        Order order = orderRepository.findById(rr.getOrderId()).orElse(null);
        BigDecimal amount = decision.refundAmount() != null
                ? decision.refundAmount()
                : (order != null ? order.getTotalPrice() : BigDecimal.ZERO);
        rr.setRefundAmount(amount);
        rr.setStatus(APPROVED);

        String paymentIntentId = order == null ? null : order.getPaymentIntentId();
        if (stripeConfigured() && paymentIntentId != null && !paymentIntentId.isBlank()) {
            try {
                Stripe.apiKey = stripeKey;
                Map<String, Object> params = new HashMap<>();
                params.put("payment_intent", paymentIntentId);
                if (amount != null && amount.signum() > 0) {
                    params.put("amount", amount.movePointRight(2).longValueExact());
                }
                Refund refund = Refund.create(params);
                rr.setStripeRefundId(refund.getId());
                rr.setStatus(REFUNDED);
            } catch (Exception e) {
                // Non-fatal: leave APPROVED for a manual refund and record why.
                log.warn("Stripe refund failed for return {} (order {}): {}", id, rr.getOrderId(), e.getMessage());
                rr.setAdminNote(appendNote(rr.getAdminNote(), "Stripe refund failed: " + e.getMessage()));
            }
        }
        return toView(returnRepository.save(rr));
    }

    private boolean stripeConfigured() {
        return stripeKey != null && !stripeKey.isBlank();
    }

    private static String appendNote(String existing, String addition) {
        if (existing == null || existing.isBlank()) {
            return addition;
        }
        return existing + " | " + addition;
    }

    private static ReturnRequestView toView(ReturnRequest r) {
        return new ReturnRequestView(r.getId(), r.getOrderId(), r.getOrderTrackingNumber(),
                r.getCustomerEmail(), r.getReason(), r.getStatus(), r.getRefundAmount(),
                r.getAdminNote(), REFUNDED.equals(r.getStatus()), r.getDateCreated());
    }
}
