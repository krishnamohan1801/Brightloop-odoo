package com.brightloop.sms.service;

import com.brightloop.sms.dao.*;
import com.brightloop.sms.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final SubscriptionDao subscriptionDao;
    private final PlanDao planDao;
    private final InvoiceDao invoiceDao;
    private final DiscountDao discountDao;
    private final TaxDao taxDao;
    private final NotificationDao notificationDao;
    private final AuditLogDao auditLogDao;
    private final EmailService emailService;

    public BillingService(
            SubscriptionDao subscriptionDao,
            PlanDao planDao,
            InvoiceDao invoiceDao,
            DiscountDao discountDao,
            TaxDao taxDao,
            NotificationDao notificationDao,
            AuditLogDao auditLogDao,
            EmailService emailService) {
        this.subscriptionDao = subscriptionDao;
        this.planDao = planDao;
        this.invoiceDao = invoiceDao;
        this.discountDao = discountDao;
        this.taxDao = taxDao;
        this.notificationDao = notificationDao;
        this.auditLogDao = auditLogDao;
        this.emailService = emailService;
    }

    /**
     * Executes the automated recurring billing job.
     */
    @Transactional
    public Map<String, Object> runBillingEngine() {
        LocalDate today = LocalDate.now();
        log.info("Starting recurring billing run for date: {}", today);

        List<Subscription> dueSubscriptions = subscriptionDao.findDueForBilling(today);
        int generatedCount = 0;
        int skippedCount = 0;
        List<String> generatedInvoices = new ArrayList<>();

        for (Subscription sub : dueSubscriptions) {
            try {
                Optional<Invoice> created = processSubscriptionBilling(sub, today);
                if (created.isPresent()) {
                    generatedCount++;
                    generatedInvoices.add(created.get().getInvoiceNumber());
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to bill subscription {}: {}", sub.getSubscriptionNumber(), e.getMessage(), e);
            }
        }

        log.info("Billing engine run completed. Generated: {}, Skipped: {}", generatedCount, skippedCount);

        return Map.of(
            "runDate", today.toString(),
            "dueSubscriptionsChecked", dueSubscriptions.size(),
            "invoicesGenerated", generatedCount,
            "invoicesSkipped", skippedCount,
            "invoiceNumbers", generatedInvoices
        );
    }

    private Optional<Invoice> processSubscriptionBilling(Subscription sub, LocalDate today) {
        Plan plan = planDao.findById(sub.getPlanId())
                .orElseThrow(() -> new IllegalStateException("Plan not found: " + sub.getPlanId()));

        LocalDate periodStart = sub.getNextBillingDate() != null ? sub.getNextBillingDate() : today;
        LocalDate periodEnd = calculatePeriodEnd(periodStart, plan.getBillingInterval());
        LocalDate nextBillingDate = calculateNextBillingDate(periodStart, plan.getBillingInterval());

        // Zero Double-Billing Protection
        if (invoiceDao.existsByBillingCycle(sub.getId(), periodStart, periodEnd)) {
            log.warn("Invoice already exists for subscription {} in cycle [{} - {}]. Advancing next billing date.",
                    sub.getSubscriptionNumber(), periodStart, periodEnd);
            subscriptionDao.updateNextBillingDate(sub.getId(), nextBillingDate);
            return Optional.empty();
        }

        List<SubscriptionItem> subItems = sub.getItems();
        if (subItems == null || subItems.isEmpty()) {
            subItems = subscriptionDao.findItemsBySubscriptionId(sub.getId());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        BigDecimal totalTaxAmount = BigDecimal.ZERO;

        for (SubscriptionItem item : subItems) {
            BigDecimal lineSubtotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            subtotal = subtotal.add(lineSubtotal);

            BigDecimal lineTaxRate = BigDecimal.ZERO;
            if (item.getTaxId() != null) {
                Optional<Tax> taxOpt = taxDao.findById(item.getTaxId());
                if (taxOpt.isPresent()) {
                    lineTaxRate = taxOpt.get().getRate();
                }
            }

            BigDecimal lineTaxAmount = lineSubtotal.multiply(lineTaxRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            totalTaxAmount = totalTaxAmount.add(lineTaxAmount);

            InvoiceItem invItem = new InvoiceItem();
            invItem.setProductId(item.getProductId());
            invItem.setVariantId(item.getVariantId());
            String desc = item.getProductName() != null ? item.getProductName() : "Subscription Item";
            if (item.getVariantName() != null) {
                desc += " (" + item.getVariantName() + ")";
            }
            invItem.setDescription(desc);
            invItem.setQuantity(item.getQuantity());
            invItem.setUnitPrice(item.getUnitPrice());
            invItem.setTaxRate(lineTaxRate);
            invItem.setTaxAmount(lineTaxAmount);
            invItem.setSubtotal(lineSubtotal);
            invoiceItems.add(invItem);
        }

        // Apply discount if applicable
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (sub.getDiscountId() != null) {
            Optional<Discount> discountOpt = discountDao.findById(sub.getDiscountId());
            if (discountOpt.isPresent()) {
                Discount disc = discountOpt.get();
                if (disc.getIsActive() && !today.isBefore(disc.getStartDate()) && !today.isAfter(disc.getEndDate())) {
                    if ("percentage".equalsIgnoreCase(disc.getDiscountType())) {
                        discountAmount = subtotal.multiply(disc.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    } else {
                        discountAmount = disc.getValue();
                    }
                    if (discountAmount.compareTo(subtotal) > 0) {
                        discountAmount = subtotal;
                    }
                    discountDao.incrementUsedCount(disc.getId());
                }
            }
        }

        BigDecimal netBeforeTax = subtotal.subtract(discountAmount);
        if (netBeforeTax.compareTo(BigDecimal.ZERO) < 0) {
            netBeforeTax = BigDecimal.ZERO;
        }
        BigDecimal totalAmount = netBeforeTax.add(totalTaxAmount);

        Invoice invoice = new Invoice();
        String invoiceNumber = "INV-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int)(Math.random() * 90000 + 10000));
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setSubscriptionId(sub.getId());
        invoice.setCustomerId(sub.getCustomerId());
        invoice.setBillingPeriodStart(periodStart);
        invoice.setBillingPeriodEnd(periodEnd);
        invoice.setIssueDate(today);
        invoice.setDueDate(today.plusDays(15)); // Net 15
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(totalTaxAmount);
        invoice.setDiscountAmount(discountAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus("confirmed");

        Invoice createdInvoice = invoiceDao.create(invoice, invoiceItems);

        // Advance subscription billing date
        subscriptionDao.updateNextBillingDate(sub.getId(), nextBillingDate);

        // Check auto_close lifecycle
        if (Boolean.TRUE.equals(plan.getAutoClose()) && sub.getExpiryDate() != null) {
            if (nextBillingDate.isAfter(sub.getExpiryDate())) {
                sub.setStatus("closed");
                sub.setNotes((sub.getNotes() != null ? sub.getNotes() + " | " : "") + "Auto-closed on completion of billing period");
                subscriptionDao.update(sub);
            }
        }

        // Notify customer
        notificationDao.create(new Notification(
                sub.getCustomerId(),
                "New Invoice Generated",
                "Invoice " + invoiceNumber + " for $" + totalAmount + " has been generated for period " + periodStart + " to " + periodEnd + ".",
                "info"
        ));

        // Audit log
        auditLogDao.log(
                sub.getCustomerId(),
                "INVOICE_GENERATED",
                "invoices",
                createdInvoice.getId().toString(),
                "Automated recurring invoice generated: " + invoiceNumber + " ($" + totalAmount + ")"
        );

        if (sub.getCustomerEmail() != null) {
            emailService.sendInvoiceNotification(sub.getCustomerEmail(), invoiceNumber, totalAmount, invoice.getDueDate().toString());
        }

        return Optional.of(createdInvoice);
    }

    private LocalDate calculatePeriodEnd(LocalDate start, String interval) {
        if (interval == null) return start.plusMonths(1).minusDays(1);
        return switch (interval.toLowerCase()) {
            case "daily" -> start;
            case "weekly" -> start.plusWeeks(1).minusDays(1);
            case "yearly" -> start.plusYears(1).minusDays(1);
            default -> start.plusMonths(1).minusDays(1);
        };
    }

    private LocalDate calculateNextBillingDate(LocalDate current, String interval) {
        if (interval == null) return current.plusMonths(1);
        return switch (interval.toLowerCase()) {
            case "daily" -> current.plusDays(1);
            case "weekly" -> current.plusWeeks(1);
            case "yearly" -> current.plusYears(1);
            default -> current.plusMonths(1);
        };
    }
}
