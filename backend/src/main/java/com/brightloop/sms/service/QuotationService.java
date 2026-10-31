package com.brightloop.sms.service;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.NotificationDao;
import com.brightloop.sms.dao.QuotationTemplateDao;
import com.brightloop.sms.dao.SubscriptionDao;
import com.brightloop.sms.model.Notification;
import com.brightloop.sms.model.QuotationTemplate;
import com.brightloop.sms.model.Subscription;
import com.brightloop.sms.model.SubscriptionItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class QuotationService {

    private final QuotationTemplateDao templateDao;
    private final SubscriptionDao subscriptionDao;
    private final ProductService productService;
    private final AuditLogDao auditLogDao;
    private final NotificationDao notificationDao;

    public QuotationService(
            QuotationTemplateDao templateDao,
            SubscriptionDao subscriptionDao,
            ProductService productService,
            AuditLogDao auditLogDao,
            NotificationDao notificationDao) {
        this.templateDao = templateDao;
        this.subscriptionDao = subscriptionDao;
        this.productService = productService;
        this.auditLogDao = auditLogDao;
        this.notificationDao = notificationDao;
    }

    public List<QuotationTemplate> getAllTemplates() {
        return templateDao.findAll();
    }

    public QuotationTemplate createTemplate(QuotationTemplate template, UUID userId) {
        QuotationTemplate created = templateDao.create(template);
        auditLogDao.log(userId, "TEMPLATE_CREATE", "quotation_templates", created.getId().toString(), "Created template: " + created.getName());
        return created;
    }

    public List<Subscription> getAllQuotations(UUID customerId) {
        return subscriptionDao.findAll("quotation", customerId);
    }

    @Transactional
    public Subscription createQuotation(Subscription quote, List<SubscriptionItem> items, UUID userId) {
        if (quote.getSubscriptionNumber() == null || quote.getSubscriptionNumber().isBlank()) {
            quote.setSubscriptionNumber("QT-" + System.currentTimeMillis());
        }
        quote.setStatus("quotation");
        quote.setCreatedBy(userId);
        if (quote.getStartDate() == null) {
            quote.setStartDate(LocalDate.now());
        }
        if (quote.getNextBillingDate() == null) {
            quote.setNextBillingDate(LocalDate.now());
        }
        if (quote.getExpiryDate() == null) {
            quote.setExpiryDate(LocalDate.now().plusDays(30)); // 30-day validity
        }

        // Calculate unit prices and subtotals
        if (items != null) {
            for (SubscriptionItem item : items) {
                if (item.getUnitPrice() == null || item.getUnitPrice().signum() <= 0) {
                    item.setUnitPrice(productService.calculateEffectivePrice(item.getProductId(), item.getVariantId()));
                }
                int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
                item.setQuantity(qty);
                item.setSubtotal(item.getUnitPrice().multiply(new java.math.BigDecimal(qty)));
            }
        }

        Subscription created = subscriptionDao.create(quote, items);
        auditLogDao.log(userId, "QUOTATION_CREATE", "subscriptions", created.getId().toString(), "Prepared quotation offer: " + created.getSubscriptionNumber());

        notificationDao.create(new Notification(
                created.getCustomerId(),
                "New Quotation Offer Received",
                "You have a new quotation " + created.getSubscriptionNumber() + " awaiting your review and approval.",
                "info"
        ));

        return created;
    }

    @Transactional
    public Subscription acceptQuotation(UUID quotationId, UUID userId) {
        Subscription quote = subscriptionDao.findById(quotationId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found: " + quotationId));

        if (!"quotation".equalsIgnoreCase(quote.getStatus()) && !"draft".equalsIgnoreCase(quote.getStatus())) {
            throw new IllegalStateException("Only quotations in draft or quotation status can be accepted.");
        }

        if (quote.getExpiryDate() != null && quote.getExpiryDate().isBefore(LocalDate.now())) {
            quote.setStatus("closed");
            subscriptionDao.updateStatus(quotationId, "closed");
            throw new IllegalStateException("Quotation has expired on " + quote.getExpiryDate());
        }

        // Move to Active
        quote.setStatus("active");
        quote.setStartDate(LocalDate.now());
        quote.setNextBillingDate(LocalDate.now());
        subscriptionDao.update(quote);

        auditLogDao.log(userId, "QUOTATION_ACCEPT", "subscriptions", quotationId.toString(), "Quotation accepted and activated: " + quote.getSubscriptionNumber());

        notificationDao.create(new Notification(
                quote.getCustomerId(),
                "Subscription Activated!",
                "Your quotation " + quote.getSubscriptionNumber() + " has been accepted and activated. Welcome aboard!",
                "success"
        ));

        return subscriptionDao.findById(quotationId).orElse(quote);
    }

    public Subscription rejectQuotation(UUID quotationId, UUID userId) {
        Subscription quote = subscriptionDao.findById(quotationId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found: " + quotationId));

        quote.setStatus("closed");
        quote.setNotes((quote.getNotes() != null ? quote.getNotes() + " | " : "") + "Rejected on " + LocalDate.now());
        subscriptionDao.update(quote);

        auditLogDao.log(userId, "QUOTATION_REJECT", "subscriptions", quotationId.toString(), "Quotation rejected: " + quote.getSubscriptionNumber());
        return quote;
    }
}
