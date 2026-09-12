package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.AdminWebhookSubscriptionRequest;
import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.dto.WebhookDeliveryAttemptView;
import com.bob.ecommerceangularapp.dto.WebhookSubscriptionCreatedView;
import com.bob.ecommerceangularapp.dto.WebhookSubscriptionView;
import com.bob.ecommerceangularapp.entity.WebhookSubscription;
import com.bob.ecommerceangularapp.service.AuditLogService;
import com.bob.ecommerceangularapp.service.WebhookSubscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin webhook-subscription management: register/update/list/deactivate (roadmap #23, Milestone B).
 * {@code save} is an upsert (null {@code id} = create); the response type differs by which happened —
 * a create returns {@link WebhookSubscriptionCreatedView} (the one time the full signing secret is
 * shown), an update returns the normal masked {@link WebhookSubscriptionView}.
 */
@RestController
@RequestMapping("/api/admin/webhooks")
public class AdminWebhookController {

    private final WebhookSubscriptionService webhookSubscriptionService;
    private final AuditLogService auditLogService;

    public AdminWebhookController(WebhookSubscriptionService webhookSubscriptionService, AuditLogService auditLogService) {
        this.webhookSubscriptionService = webhookSubscriptionService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<WebhookSubscriptionView> list() {
        return webhookSubscriptionService.listAll().stream().map(WebhookSubscriptionView::from).toList();
    }

    @PostMapping
    public ResponseEntity<?> save(Authentication authentication, @Valid @RequestBody AdminWebhookSubscriptionRequest request) {
        boolean isCreate = request.id() == null;
        WebhookSubscription saved = webhookSubscriptionService.save(request);
        auditLogService.record(authentication, isCreate ? "WEBHOOK_CREATE" : "WEBHOOK_UPDATE",
                "WebhookSubscription", String.valueOf(saved.getId()), saved.getUrl());
        if (isCreate) {
            return ResponseEntity.status(HttpStatus.CREATED).body(WebhookSubscriptionCreatedView.from(saved));
        }
        return ResponseEntity.ok(WebhookSubscriptionView.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(Authentication authentication, @PathVariable Long id) {
        webhookSubscriptionService.deactivate(id);
        auditLogService.record(authentication, "WEBHOOK_DEACTIVATE", "WebhookSubscription", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/deliveries")
    public PageResponse<WebhookDeliveryAttemptView> deliveries(@PathVariable Long id,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(webhookSubscriptionService.listDeliveries(id, PageRequest.of(page, size)));
    }
}
