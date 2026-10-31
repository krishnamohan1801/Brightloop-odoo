package com.brightloop.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() { }

    public record InvoiceSummary(
            UUID id,
            String invoiceNumber,
            UUID subscriptionId,
            UUID customerId,
            LocalDate billingPeriodStart,
            LocalDate billingPeriodEnd,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String databaseStatus,
            BigDecimal paidAmount,
            BigDecimal balanceDue,
            String paymentStatus) { }

    public record InvoiceItemView(
            UUID id,
            UUID productId,
            UUID variantId,
            String description,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal subtotal) { }

    public record InvoiceDetail(InvoiceSummary invoice, List<InvoiceItemView> items) { }

    public record BillingRunResult(int dueSubscriptions, int invoicesGenerated, List<String> messages) { }

    public record PaymentRequest(
            @NotNull UUID invoiceId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String paymentMethod,
            String outcome) { }

    public record ResolvePaymentRequest(@NotBlank String outcome) { }

    public record RefundRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount) { }

    public record PaymentView(
            UUID id,
            UUID invoiceId,
            UUID customerId,
            BigDecimal amount,
            String paymentMethod,
            LocalDate paymentDate,
            String referenceNumber,
            String outcome,
            String notes) { }

    public record PaymentResult(
            UUID paymentId,
            UUID invoiceId,
            BigDecimal amount,
            String paymentMethod,
            String referenceNumber,
            String outcome,
            BigDecimal paidAmount,
            BigDecimal balanceDue,
            String invoicePaymentStatus,
            String message) { }

    public record ApiError(String message, LocalDate date) { }
}
