package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.Subscription;
import com.brightloop.sms.model.SubscriptionItem;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription lifecycle management (Active, Paused, Closed)")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserDao userDao;

    public SubscriptionController(SubscriptionService subscriptionService, UserDao userDao) {
        this.subscriptionService = subscriptionService;
        this.userDao = userDao;
    }

    @GetMapping
    @Operation(summary = "List subscriptions")
    public ResponseEntity<List<Subscription>> getSubscriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID customerId,
            Authentication authentication) {
        User user = getUser(authentication);
        UUID filterCustomerId = customerId;
        if (user != null && "customer".equalsIgnoreCase(user.getRole())) {
            filterCustomerId = user.getId();
        }
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(status, filterCustomerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single subscription by id")
    public ResponseEntity<Subscription> getSubscriptionById(@PathVariable UUID id, Authentication authentication) {
        Subscription sub = subscriptionService.getSubscriptionById(id);
        User user = getUser(authentication);
        if (user != null && "customer".equalsIgnoreCase(user.getRole())) {
            if (!sub.getCustomerId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(sub);
    }

    public static class CreateSubRequest {
        private Subscription subscription;
        private List<SubscriptionItem> items;
        public Subscription getSubscription() { return subscription; }
        public void setSubscription(Subscription subscription) { this.subscription = subscription; }
        public List<SubscriptionItem> getItems() { return items; }
        public void setItems(List<SubscriptionItem> items) { this.items = items; }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create confirmed subscription")
    public ResponseEntity<Subscription> createSubscription(
            @RequestBody CreateSubRequest request,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(subscriptionService.createSubscription(request.getSubscription(), request.getItems(), userId));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause active subscription")
    public ResponseEntity<Subscription> pauseSubscription(@PathVariable UUID id, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(subscriptionService.pauseSubscription(id, userId));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume paused subscription")
    public ResponseEntity<Subscription> resumeSubscription(@PathVariable UUID id, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(subscriptionService.resumeSubscription(id, userId));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close / cancel subscription")
    public ResponseEntity<Subscription> closeSubscription(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "Customer requested cancellation") String reason,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(subscriptionService.closeSubscription(id, reason, userId));
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userDao.findByEmail(auth.getName()).orElse(null);
    }

    private UUID getUserId(Authentication auth) {
        User u = getUser(auth);
        return u != null ? u.getId() : null;
    }
}
