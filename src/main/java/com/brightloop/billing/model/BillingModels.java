package com.brightloop.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class BillingModels {
    private BillingModels() { }

    public record SubscriptionInfo(
            UUID id,
            UUID customerId,
            UUID discountId,
            String status,
            String paymentTerms,
            LocalDate nextBillingDate,
            LocalDate expiryDate,
            String billingInterval) { }

    public record SubscriptionItemInfo(
            UUID productId,
            UUID variantId,
            String productName,
            String variantName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate) { }

    public record DiscountInfo(
            UUID id,
            String code,
            String discountType,
            BigDecimal value,
            BigDecimal minPurchase,
            int minQuantity,
            LocalDate startDate,
            LocalDate endDate,
            Integer maxUses,
            int usedCount,
            String targetScope,
            UUID targetProductId,
            boolean active) { }

    public record InvoiceHeader(
            UUID id,
            UUID customerId,
            BigDecimal totalAmount,
            String databaseStatus,
            LocalDate dueDate) { }
}
