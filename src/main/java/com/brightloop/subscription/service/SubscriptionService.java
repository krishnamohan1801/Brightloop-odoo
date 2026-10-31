package com.brightloop.subscription.service;

import com.brightloop.subscription.dao.SubscriptionDAO;
import com.brightloop.subscription.dao.SubscriptionItemDAO;
import com.brightloop.subscription.model.Subscription;
import com.brightloop.subscription.model.SubscriptionItem;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionDAO subscriptionDAO;
    private final SubscriptionItemDAO subscriptionItemDAO;

    private static final Set<String> VALID_STATUSES = Set.of(
            "draft",
            "quotation",
            "confirmed",
            "active",
            "paused",
            "closed"
    );

    private static final String DEFAULT_STATUS = "draft";

    public SubscriptionService(SubscriptionDAO subscriptionDAO, SubscriptionItemDAO subscriptionItemDAO) {
        this.subscriptionDAO = subscriptionDAO;
        this.subscriptionItemDAO = subscriptionItemDAO;
    }

    @Transactional
    public Subscription createSubscription(Subscription subscription, List<SubscriptionItem> items) {
        if (subscription == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription payload is required");
        }

        if (subscription.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer ID is required");
        }

        if (subscription.getPlanId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan ID is required");
        }

        if (subscription.getSubscriptionNumber() == null || subscription.getSubscriptionNumber().isBlank()) {
            subscription.setSubscriptionNumber("SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        }

        String normalizedStatus = normalizeStatus(subscription.getStatus());
        subscription.setStatus(normalizedStatus);
        validateStatus(subscription.getStatus());

        UUID subscriptionId = subscriptionDAO.save(subscription);

        if (items != null) {
            for (SubscriptionItem item : items) {
                if (item == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subscription items cannot be null");
                }

                if (item.getProductId() == null
                        || item.getQuantity() == null
                        || item.getQuantity() <= 0
                        || item.getUnitPrice() == null
                        || item.getUnitPrice().signum() < 0
                        || item.getSubtotal() == null
                        || item.getSubtotal().signum() < 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Each item requires a product, positive quantity, and valid price and subtotal"
                    );
                }

                item.setSubscriptionId(subscriptionId);
                subscriptionItemDAO.save(item);
            }
        }

        return getSubscriptionById(subscriptionId);
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionDAO.findAll();
    }

    public Subscription getSubscriptionById(UUID id) {
        return subscriptionDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
    }

    public List<Subscription> getSubscriptionsByCustomer(UUID customerId) {
        return subscriptionDAO.findByCustomerId(customerId);
    }

    @Transactional
    public Subscription updateStatus(UUID id, String newStatus) {
        getSubscriptionById(id);

        String normalizedStatus = normalizeStatus(newStatus);
        validateStatus(normalizedStatus);

        subscriptionDAO.updateStatus(id, normalizedStatus);

        return getSubscriptionById(id);
    }

    @Transactional
    public Subscription pauseSubscription(UUID id) {
        Subscription subscription = getSubscriptionById(id);

        if (!"active".equalsIgnoreCase(subscription.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active subscriptions can be paused");
        }

        subscriptionDAO.updateStatus(id, "paused");

        return getSubscriptionById(id);
    }

    @Transactional
    public Subscription resumeSubscription(UUID id) {
        Subscription subscription = getSubscriptionById(id);

        if (!"paused".equalsIgnoreCase(subscription.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only paused subscriptions can be resumed");
        }

        subscriptionDAO.updateStatus(id, "active");

        return getSubscriptionById(id);
    }

    @Transactional
    public Subscription closeSubscription(UUID id) {
        Subscription subscription = getSubscriptionById(id);

        if ("closed".equalsIgnoreCase(subscription.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subscription is already closed");
        }

        subscriptionDAO.updateStatus(id, "closed");

        return getSubscriptionById(id);
    }

    public List<SubscriptionItem> getSubscriptionItems(UUID id) {
        getSubscriptionById(id);
        return subscriptionItemDAO.findBySubscriptionId(id);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return DEFAULT_STATUS;
        }

        String normalized = status.trim();
        if (normalized.isBlank()) {
            return DEFAULT_STATUS;
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private void validateStatus(String status) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid subscription status");
        }
    }
}
