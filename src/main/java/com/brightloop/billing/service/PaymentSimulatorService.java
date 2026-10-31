package com.brightloop.billing.service;

import com.brightloop.billing.dao.BillingDao;
import com.brightloop.billing.dao.PaymentDao;
import com.brightloop.billing.dto.ApiModels.PaymentRequest;
import com.brightloop.billing.dto.ApiModels.RefundRequest;
import com.brightloop.billing.dto.ApiModels.PaymentResult;
import com.brightloop.billing.dto.ApiModels.PaymentView;
import com.brightloop.billing.exception.BusinessException;
import com.brightloop.billing.exception.NotFoundException;
import com.brightloop.billing.model.BillingModels.InvoiceHeader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentSimulatorService {
    private final BillingDao billingDao;
    private final PaymentDao paymentDao;

    public PaymentSimulatorService(BillingDao billingDao, PaymentDao paymentDao) {
        this.billingDao = billingDao;
        this.paymentDao = paymentDao;
    }

    @Transactional
    public PaymentResult simulate(PaymentRequest request) {
        InvoiceHeader invoice = billingDao.findInvoiceForUpdate(request.invoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + request.invoiceId()));
        validatePayable(invoice);

        String method = normalizePaymentMethod(request.paymentMethod());
        BigDecimal paidBefore = paymentDao.calculateNetCollectedAmount(invoice.id());
        BigDecimal balanceBefore = invoice.totalAmount().subtract(paidBefore).max(BigDecimal.ZERO);
        BigDecimal amount = request.amount().setScale(2, java.math.RoundingMode.HALF_UP);

        if (amount.compareTo(balanceBefore) != 0) {
            throw new BusinessException("Partial payment is not allowed. Full invoice amount of " + balanceBefore + " is required.");
        }

        String outcome = normalizeOutcome(request.outcome());
        if (outcome == null) outcome = randomOutcome();

        String reference = "SIM-" + outcome + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String notes = "SIMULATED_" + outcome + "; No real money was transferred.";
        UUID paymentId = paymentDao.insertPayment(invoice.id(), invoice.customerId(), amount,
                method, reference, notes);

        if ("SUCCESS".equals(outcome)) {
            BigDecimal paidAfter = paidBefore.add(amount);
            if (paidAfter.compareTo(invoice.totalAmount()) >= 0) {
                billingDao.updateInvoiceStatus(invoice.id(), "paid");
            }
        }

        BigDecimal paidAfter = paymentDao.calculateNetCollectedAmount(invoice.id());
        BigDecimal balanceAfter = invoice.totalAmount().subtract(paidAfter).max(BigDecimal.ZERO);
        String paymentStatus = resolveInvoicePaymentStatus(invoice.databaseStatus(), invoice.dueDate(), paidAfter, balanceAfter);
        return new PaymentResult(paymentId, invoice.id(), amount, method, reference, outcome,
                paidAfter, balanceAfter, paymentStatus,
                "Dummy payment " + outcome.toLowerCase(Locale.ROOT) + ". This is a simulated transaction only.");
    }

    @Transactional
    public PaymentResult resolvePendingPayment(UUID paymentId, String requestedOutcome) {
        PaymentView payment = paymentDao.findPayment(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment attempt not found: " + paymentId));
        if (!"PENDING".equals(payment.outcome())) {
            throw new BusinessException("Only pending simulated payments can be resolved.");
        }

        InvoiceHeader invoice = billingDao.findInvoiceForUpdate(payment.invoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + payment.invoiceId()));
        validatePayable(invoice);
        String outcome = normalizeOutcome(requestedOutcome);
        if (outcome == null || "PENDING".equals(outcome)) {
            throw new BusinessException("Resolve outcome must be SUCCESS or FAILED.");
        }

        BigDecimal paidBefore = paymentDao.calculateNetCollectedAmount(invoice.id());
        BigDecimal balanceBefore = invoice.totalAmount().subtract(paidBefore).max(BigDecimal.ZERO);
        if ("SUCCESS".equals(outcome) && payment.amount().compareTo(balanceBefore) != 0) {
            throw new BusinessException("Payment amount (" + payment.amount() + ") does not match remaining invoice balance (" + balanceBefore + "). Partial payment is not allowed.");
        }

        String reference = "SIM-" + outcome + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String notes = "SIMULATED_" + outcome + " (resolved from pending); No real money was transferred.";
        int updated = paymentDao.resolvePending(paymentId, reference, notes);
        if (updated == 0) throw new BusinessException("Pending transaction was already resolved.");

        if ("SUCCESS".equals(outcome)) {
            BigDecimal paidAfter = paidBefore.add(payment.amount());
            if (paidAfter.compareTo(invoice.totalAmount()) >= 0) {
                billingDao.updateInvoiceStatus(invoice.id(), "paid");
            }
        }

        BigDecimal paidAfter = paymentDao.calculateNetCollectedAmount(invoice.id());
        BigDecimal balanceAfter = invoice.totalAmount().subtract(paidAfter).max(BigDecimal.ZERO);
        String paymentStatus = resolveInvoicePaymentStatus(invoice.databaseStatus(), invoice.dueDate(), paidAfter, balanceAfter);
        return new PaymentResult(payment.id(), invoice.id(), payment.amount(), payment.paymentMethod(),
                reference, outcome, paidAfter, balanceAfter, paymentStatus,
                "Pending dummy payment resolved as " + outcome + ". No real money was transferred.");
    }

    @Transactional
    public PaymentResult simulateRefund(UUID originalPaymentId, RefundRequest request) {
        PaymentView originalPayment = paymentDao.findPayment(originalPaymentId)
                .orElseThrow(() -> new NotFoundException("Original payment not found: " + originalPaymentId));
        if (!"SUCCESS".equals(originalPayment.outcome())) {
            throw new BusinessException("Only a successful simulated payment can be refunded.");
        }

        InvoiceHeader invoice = billingDao.findInvoiceForUpdate(originalPayment.invoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + originalPayment.invoiceId()));
        if ("cancelled".equals(invoice.databaseStatus())) {
            throw new BusinessException("A cancelled invoice cannot be refunded through the simulator.");
        }

        BigDecimal amount = request.amount().setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal alreadyRefunded = paymentDao.sumRefundsForOriginalPayment(originalPaymentId);
        BigDecimal refundableAmount = originalPayment.amount().subtract(alreadyRefunded).max(BigDecimal.ZERO);
        if (amount.compareTo(refundableAmount) > 0) {
            throw new BusinessException("Refund amount exceeds the remaining refundable amount of " + refundableAmount + ".");
        }

        String reference = "SIM-REFUND-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String notes = "SIMULATED_REFUND; REFUND_OF=" + originalPaymentId + "; No real money was transferred.";
        UUID refundId = paymentDao.insertPayment(invoice.id(), invoice.customerId(), amount,
                originalPayment.paymentMethod(), reference, notes);

        BigDecimal netPaid = paymentDao.calculateNetCollectedAmount(invoice.id());
        if (netPaid.compareTo(invoice.totalAmount()) >= 0) {
            billingDao.updateInvoiceStatus(invoice.id(), "paid");
        } else {
            billingDao.updateInvoiceStatus(invoice.id(), "confirmed");
        }

        BigDecimal balance = invoice.totalAmount().subtract(netPaid).max(BigDecimal.ZERO);
        String paymentStatus = resolveInvoicePaymentStatus(invoice.databaseStatus(), invoice.dueDate(), netPaid, balance);
        return new PaymentResult(refundId, invoice.id(), amount, originalPayment.paymentMethod(),
                reference, "REFUND", netPaid, balance, paymentStatus,
                "Dummy refund recorded against payment " + originalPaymentId + ". No real money was transferred.");
    }

    public List<PaymentView> getPaymentHistory(UUID invoiceId) {
        billingDao.findInvoice(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + invoiceId));
        return paymentDao.findPayments(invoiceId);
    }

    private void validatePayable(InvoiceHeader invoice) {
        if ("cancelled".equals(invoice.databaseStatus())) {
            throw new BusinessException("Cancelled invoices cannot accept payments.");
        }
        if ("draft".equals(invoice.databaseStatus())) {
            throw new BusinessException("Confirm the invoice before accepting a payment.");
        }
    }

    private String normalizePaymentMethod(String raw) {
        if (raw == null) throw new BusinessException("Payment method is required.");
        String method = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('/', '_');
        if ("debit_card".equals(method) || "credit_debit_card".equals(method)) method = "credit_card";
        if (!List.of("bank_transfer", "credit_card", "cash", "upi", "other").contains(method)) {
            throw new BusinessException("Supported methods: credit_card, upi, bank_transfer, cash, other.");
        }
        return method;
    }

    private String normalizeOutcome(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String outcome = raw.trim().toUpperCase(Locale.ROOT);
        if ("SUCCESSFUL".equals(outcome)) outcome = "SUCCESS";
        if ("FAILED".equals(outcome) || "SUCCESS".equals(outcome) || "PENDING".equals(outcome)) return outcome;
        throw new BusinessException("Outcome must be SUCCESS, FAILED or PENDING.");
    }

    private String randomOutcome() {
        int chance = ThreadLocalRandom.current().nextInt(100);
        if (chance < 80) return "SUCCESS";
        if (chance < 95) return "FAILED";
        return "PENDING";
    }

    private String resolveInvoicePaymentStatus(String databaseStatus, java.time.LocalDate dueDate,
                                               BigDecimal paid, BigDecimal balance) {
        if ("cancelled".equals(databaseStatus)) return "CANCELLED";
        if (balance.compareTo(BigDecimal.ZERO) == 0) return "PAID";
        if (dueDate.isBefore(java.time.LocalDate.now())) return "OVERDUE";
        if (paid.compareTo(BigDecimal.ZERO) > 0) return "PARTIALLY_PAID";
        return "CONFIRMED";
    }
}
