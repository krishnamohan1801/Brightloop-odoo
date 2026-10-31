package com.brightloop.billing.service;

import com.brightloop.billing.dao.BillingDao;
import com.brightloop.billing.dao.PaymentDao;
import com.brightloop.billing.dto.ApiModels.BillingRunResult;
import com.brightloop.billing.dto.ApiModels.InvoiceDetail;
import com.brightloop.billing.dto.ApiModels.InvoiceSummary;
import com.brightloop.billing.exception.BusinessException;
import com.brightloop.billing.exception.NotFoundException;
import com.brightloop.billing.model.BillingModels.DiscountInfo;
import com.brightloop.billing.model.BillingModels.SubscriptionInfo;
import com.brightloop.billing.model.BillingModels.SubscriptionItemInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BillingService {
    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final BillingDao billingDao;
    private final PaymentDao paymentDao;
    private final TransactionTemplate transactionTemplate;

    public BillingService(BillingDao billingDao, PaymentDao paymentDao,
                          PlatformTransactionManager transactionManager) {
        this.billingDao = billingDao;
        this.paymentDao = paymentDao;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public BillingRunResult runDueBilling() {
        LocalDate today = LocalDate.now();
        List<UUID> dueIds = billingDao.findDueSubscriptionIds(today);
        int generated = 0;
        List<String> messages = new ArrayList<>();

        for (UUID subscriptionId : dueIds) {
            try {
                InvoiceSummary invoice = generateDueInvoice(subscriptionId);
                if (invoice != null) {
                    generated++;
                    messages.add("Generated " + invoice.invoiceNumber());
                }
            } catch (Exception ex) {
                log.error("Billing failed for subscription {}", subscriptionId, ex);
                messages.add("Subscription " + subscriptionId + ": " + ex.getMessage());
            }
        }
        return new BillingRunResult(dueIds.size(), generated, messages);
    }

    public InvoiceSummary generateDueInvoice(UUID subscriptionId) {
        return transactionTemplate.execute(status -> generateInsideTransaction(subscriptionId, LocalDate.now()));
    }

    private InvoiceSummary generateInsideTransaction(UUID subscriptionId, LocalDate issueDate) {
        SubscriptionInfo subscription = billingDao.findSubscription(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found: " + subscriptionId));

        if (!"active".equals(subscription.status())) {
            throw new BusinessException("Only active subscriptions can be billed.");
        }

        LocalDate periodStart = subscription.nextBillingDate();
        if (periodStart.isAfter(issueDate)) {
            throw new BusinessException("This subscription is not due for billing yet.");
        }
        if (subscription.expiryDate() != null && periodStart.isAfter(subscription.expiryDate())) {
            throw new BusinessException("The subscription has expired and is not eligible for billing.");
        }

        LocalDate nextBillingDate = nextBillingDate(periodStart, subscription.billingInterval());
        LocalDate periodEnd = nextBillingDate.minusDays(1);
        if (subscription.expiryDate() != null && periodEnd.isAfter(subscription.expiryDate())) {
            periodEnd = subscription.expiryDate();
        }

        // Repair-safe idempotency: a previously generated period is reused, never inserted twice.
        if (billingDao.invoiceExists(subscriptionId, periodStart, periodEnd)) {
            billingDao.updateNextBillingDate(subscriptionId, nextBillingDate);
            UUID existingId = billingDao.findInvoiceIdForPeriod(subscriptionId, periodStart, periodEnd)
                    .orElseThrow(() -> new BusinessException("Existing invoice could not be retrieved."));
            return billingDao.findInvoice(existingId)
                    .orElseThrow(() -> new NotFoundException("Existing invoice not found."));
        }

        List<SubscriptionItemInfo> items = billingDao.findSubscriptionItems(subscriptionId);
        if (items.isEmpty()) {
            throw new BusinessException("No subscription items are available for invoice generation.");
        }

        List<BigDecimal> lineSubtotals = new ArrayList<>();
        BigDecimal subtotal = ZERO;
        for (SubscriptionItemInfo item : items) {
            BigDecimal line = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            lineSubtotals.add(line);
            subtotal = subtotal.add(line);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        DiscountInfo discount = billingDao.findDiscount(subscription.discountId()).orElse(null);
        Map<Integer, BigDecimal> allocatedDiscount = allocateDiscount(
                discount, items, lineSubtotals, issueDate);

        BigDecimal discountAmount = allocatedDiscount.values().stream()
                .reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = ZERO;
        List<BigDecimal> lineTaxes = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            BigDecimal taxableBase = lineSubtotals.get(i).subtract(allocatedDiscount.getOrDefault(i, ZERO));
            BigDecimal tax = taxableBase.multiply(items.get(i).taxRate())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            lineTaxes.add(tax);
            taxAmount = taxAmount.add(tax);
        }

        taxAmount = taxAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate dueDate = calculateDueDate(issueDate, subscription.paymentTerms());

        UUID invoiceId = UUID.randomUUID();
        String invoiceNumber = "BL-INV-" + issueDate.toString().replace("-", "") + "-" +
                invoiceId.toString().substring(0, 8).toUpperCase();

        billingDao.insertInvoice(invoiceId, invoiceNumber, subscription.id(), subscription.customerId(),
                periodStart, periodEnd, issueDate, dueDate, subtotal, taxAmount, discountAmount, totalAmount);

        for (int i = 0; i < items.size(); i++) {
            billingDao.insertInvoiceItem(invoiceId, items.get(i), lineTaxes.get(i), lineSubtotals.get(i));
        }

        if (discountAmount.compareTo(BigDecimal.ZERO) > 0 && discount != null) {
            billingDao.incrementDiscountUsage(discount.id());
        }
        billingDao.updateNextBillingDate(subscriptionId, nextBillingDate);

        log.info("Generated invoice {} for subscription {}", invoiceNumber, subscriptionId);
        return billingDao.findInvoice(invoiceId)
                .orElseThrow(() -> new NotFoundException("Generated invoice could not be retrieved."));
    }

    private Map<Integer, BigDecimal> allocateDiscount(DiscountInfo discount,
                                                       List<SubscriptionItemInfo> items,
                                                       List<BigDecimal> lineSubtotals,
                                                       LocalDate issueDate) {
        Map<Integer, BigDecimal> result = new HashMap<>();
        if (discount == null || !discount.active()
                || issueDate.isBefore(discount.startDate()) || issueDate.isAfter(discount.endDate())
                || (discount.maxUses() != null && discount.usedCount() >= discount.maxUses())) {
            return result;
        }

        List<Integer> eligibleIndexes = new ArrayList<>();
        BigDecimal eligibleBase = ZERO;
        int eligibleQuantity = 0;
        for (int i = 0; i < items.size(); i++) {
            boolean applies = "subscription".equals(discount.targetScope())
                    || ("product".equals(discount.targetScope())
                    && discount.targetProductId() != null
                    && discount.targetProductId().equals(items.get(i).productId()));
            if (applies) {
                eligibleIndexes.add(i);
                eligibleBase = eligibleBase.add(lineSubtotals.get(i));
                eligibleQuantity += items.get(i).quantity();
            }
        }

        if (eligibleIndexes.isEmpty()
                || eligibleBase.compareTo(discount.minPurchase() == null ? ZERO : discount.minPurchase()) < 0
                || eligibleQuantity < discount.minQuantity()) {
            return result;
        }

        BigDecimal amount;
        if ("percentage".equals(discount.discountType())) {
            amount = eligibleBase.multiply(discount.value()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        } else {
            amount = discount.value().min(eligibleBase).setScale(2, RoundingMode.HALF_UP);
        }
        amount = amount.min(eligibleBase).setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return result;

        BigDecimal remaining = amount;
        for (int index : eligibleIndexes) {
            BigDecimal share = amount.multiply(lineSubtotals.get(index))
                    .divide(eligibleBase, 2, RoundingMode.DOWN)
                    .min(lineSubtotals.get(index)).min(remaining);
            result.put(index, share);
            remaining = remaining.subtract(share);
        }
        // Distribute any rounding remainder without letting a line discount exceed its line value.
        for (int index : eligibleIndexes) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal alreadyAllocated = result.getOrDefault(index, ZERO);
            BigDecimal available = lineSubtotals.get(index).subtract(alreadyAllocated);
            BigDecimal extra = available.min(remaining);
            result.put(index, alreadyAllocated.add(extra));
            remaining = remaining.subtract(extra);
        }
        return result;
    }

    public InvoiceDetail getInvoiceDetail(UUID invoiceId) {
        InvoiceSummary invoice = billingDao.findInvoice(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));
        return new InvoiceDetail(invoice, billingDao.findInvoiceItems(invoiceId));
    }

    public List<InvoiceSummary> getInvoices(UUID customerId) {
        return billingDao.findInvoices(customerId);
    }

    public List<InvoiceSummary> getOverdueInvoices() {
        return billingDao.findOverdueInvoices();
    }

    public InvoiceSummary createSampleInvoice() {
        UUID invoiceId = UUID.randomUUID();
        String invoiceNumber = "INV-2026-" + String.format("%04d", (int)(Math.random() * 9000 + 1000));
        UUID customerId = billingDao.findAnyCustomerId().orElse(UUID.randomUUID());
        UUID subscriptionId = billingDao.findAnySubscriptionId().orElse(null);
        LocalDate today = LocalDate.now();
        BigDecimal subtotal = new BigDecimal("1000.00");
        BigDecimal taxAmount = new BigDecimal("180.00");
        BigDecimal total = new BigDecimal("1180.00");
        billingDao.insertInvoice(invoiceId, invoiceNumber, subscriptionId, customerId,
                today, today.plusDays(30), today, today.plusDays(15),
                subtotal, taxAmount, BigDecimal.ZERO, total);
        return billingDao.findInvoice(invoiceId).orElseThrow();
    }

    public void cancelInvoice(UUID invoiceId) {
        transactionTemplate.executeWithoutResult(status -> {
            com.brightloop.billing.model.BillingModels.InvoiceHeader invoice = billingDao.findInvoiceForUpdate(invoiceId)
                    .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));
            BigDecimal paidAmount = paymentDao.calculateNetCollectedAmount(invoiceId);
            if (paidAmount.compareTo(BigDecimal.ZERO) > 0 || "paid".equals(invoice.databaseStatus())) {
                throw new BusinessException("An invoice with successful payments cannot be cancelled.");
            }
            billingDao.cancelInvoice(invoiceId);
        });
    }

    private LocalDate nextBillingDate(LocalDate start, String interval) {
        return switch (interval) {
            case "daily" -> start.plusDays(1);
            case "weekly" -> start.plusWeeks(1);
            case "monthly" -> start.plusMonths(1);
            case "yearly" -> start.plusYears(1);
            default -> throw new BusinessException("Unsupported billing interval: " + interval);
        };
    }

    private LocalDate calculateDueDate(LocalDate issueDate, String paymentTerms) {
        if (paymentTerms == null || paymentTerms.isBlank()) return issueDate;
        Matcher matcher = Pattern.compile("(?i)(?:net\\s*|within\\s*)?(\\d+)(?:\\s*days?)?").matcher(paymentTerms.trim());
        if (matcher.find()) return issueDate.plusDays(Long.parseLong(matcher.group(1)));
        return issueDate; // e.g. "Due on receipt"; schema requires due_date >= issue_date.
    }
}
