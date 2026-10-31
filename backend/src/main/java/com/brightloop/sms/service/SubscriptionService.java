package com.brightloop.sms.service;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.NotificationDao;
import com.brightloop.sms.dao.PlanDao;
import com.brightloop.sms.dao.SubscriptionDao;
import com.brightloop.sms.model.Notification;
import com.brightloop.sms.model.Plan;
import com.brightloop.sms.model.Subscription;
import com.brightloop.sms.model.SubscriptionItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionDao subscriptionDao;
    private final PlanDao planDao;
    private final ProductService productService;
    private final AuditLogDao auditLogDao;
    private final NotificationDao notificationDao;

    public SubscriptionService(
            SubscriptionDao subscriptionDao,
            PlanDao planDao,
            ProductService productService,
            AuditLogDao auditLogDao,
            NotificationDao notificationDao) {
        this.subscriptionDao = subscriptionDao;
        this.planDao = planDao;
        this.productService = productService;
        this.auditLogDao = auditLogDao;
        this.notificationDao = notificationDao;
    }

    public List<Subscription> getAllSubscriptions(String status, UUID customerId) {
        return subscriptionDao.findAll(status, customerId);
    }

    public Subscription getSubscriptionById(UUID id) {
        return subscriptionDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found with id: " + id));
    }

    @Transactional
    public Subscription createSubscription(Subscription sub, List<SubscriptionItem> items, UUID userId) {
        if (sub.getSubscriptionNumber() == null || sub.getSubscriptionNumber().isBlank()) {
            sub.setSubscriptionNumber("SUB-" + System.currentTimeMillis());
        }
        if (sub.getStatus() == null) {
            sub.setStatus("active");
        }
        if (sub.getStartDate() == null) {
            sub.setStartDate(LocalDate.now());
        }
        if (sub.getNextBillingDate() == null) {
            sub.setNextBillingDate(sub.getStartDate());
        }
        sub.setCreatedBy(userId);

        if (items != null) {
            for (SubscriptionItem item : items) {
                if (item.getUnitPrice() == null || item.getUnitPrice().signum() <= 0) {
                    item.setUnitPrice(productService.calculateEffectivePrice(item.getProductId(), item.getVariantId()));
                }
                int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
                item.setQuantity(qty);
                item.setSubtotal(item.getUnitPrice().multiply(new BigDecimal(qty)));
            }
        }

        Subscription created = subscriptionDao.create(sub, items);
        auditLogDao.log(userId, "SUBSCRIPTION_CREATE", "subscriptions", created.getId().toString(), "Created subscription: " + created.getSubscriptionNumber());

        notificationDao.create(new Notification(
                created.getCustomerId(),
                "Subscription Created",
                "Your subscription " + created.getSubscriptionNumber() + " is now " + created.getStatus() + ".",
                "info"
        ));

        return created;
    }

    public Subscription pauseSubscription(UUID subscriptionId, UUID userId) {
        Subscription sub = getSubscriptionById(subscriptionId);
        Plan plan = planDao.findById(sub.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (!plan.getCanPause()) {
            throw new IllegalStateException("The assigned billing plan '" + plan.getName() + "' does not permit pausing.");
        }

        if (!"active".equalsIgnoreCase(sub.getStatus())) {
            throw new IllegalStateException("Only active subscriptions can be paused.");
        }

        sub.setStatus("paused");
        subscriptionDao.updateStatus(subscriptionId, "paused");

        auditLogDao.log(userId, "SUBSCRIPTION_PAUSE", "subscriptions", subscriptionId.toString(), "Paused subscription: " + sub.getSubscriptionNumber());
        notificationDao.create(new Notification(
                sub.getCustomerId(),
                "Subscription Paused",
                "Your subscription " + sub.getSubscriptionNumber() + " has been paused.",
                "warning"
        ));

        return sub;
    }

    public Subscription resumeSubscription(UUID subscriptionId, UUID userId) {
        Subscription sub = getSubscriptionById(subscriptionId);

        if (!"paused".equalsIgnoreCase(sub.getStatus())) {
            throw new IllegalStateException("Only paused subscriptions can be resumed.");
        }

        sub.setStatus("active");
        if (sub.getNextBillingDate() == null || sub.getNextBillingDate().isBefore(LocalDate.now())) {
            sub.setNextBillingDate(LocalDate.now());
        }
        subscriptionDao.update(sub);

        auditLogDao.log(userId, "SUBSCRIPTION_RESUME", "subscriptions", subscriptionId.toString(), "Resumed subscription: " + sub.getSubscriptionNumber());
        notificationDao.create(new Notification(
                sub.getCustomerId(),
                "Subscription Resumed",
                "Your subscription " + sub.getSubscriptionNumber() + " is active again.",
                "success"
        ));

        return sub;
    }

    public Subscription closeSubscription(UUID subscriptionId, String reason, UUID userId) {
        Subscription sub = getSubscriptionById(subscriptionId);

        sub.setStatus("closed");
        sub.setNotes((sub.getNotes() != null ? sub.getNotes() + " | " : "") + "Closed: " + reason);
        subscriptionDao.update(sub);

        auditLogDao.log(userId, "SUBSCRIPTION_CLOSE", "subscriptions", subscriptionId.toString(), "Closed subscription: " + sub.getSubscriptionNumber() + " (" + reason + ")");
        notificationDao.create(new Notification(
                sub.getCustomerId(),
                "Subscription Closed",
                "Your subscription " + sub.getSubscriptionNumber() + " has been closed.",
                "info"
        ));

        return sub;
    }
}
