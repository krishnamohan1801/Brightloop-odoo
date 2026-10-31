package com.brightloop.sms.service;

import com.brightloop.sms.dao.*;
import com.brightloop.sms.dto.RefundRequest;
import com.brightloop.sms.dto.SimulatedPaymentRequest;
import com.brightloop.sms.dto.SimulatedPaymentResponse;
import com.brightloop.sms.model.Invoice;
import com.brightloop.sms.model.Notification;
import com.brightloop.sms.model.Payment;
import com.brightloop.sms.model.Refund;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentSimulatorService {

    private final InvoiceDao invoiceDao;
    private final PaymentDao paymentDao;
    private final RefundDao refundDao;
    private final NotificationDao notificationDao;
    private final AuditLogDao auditLogDao;

    public PaymentSimulatorService(
            InvoiceDao invoiceDao,
            PaymentDao paymentDao,
            RefundDao refundDao,
            NotificationDao notificationDao,
            AuditLogDao auditLogDao) {
        this.invoiceDao = invoiceDao;
        this.paymentDao = paymentDao;
        this.refundDao = refundDao;
        this.notificationDao = notificationDao;
        this.auditLogDao = auditLogDao;
    }

    public List<Payment> getAllPayments(UUID customerId) {
        return paymentDao.findAll(customerId);
    }

    public List<Payment> getPaymentsForInvoice(UUID invoiceId) {
        return paymentDao.findByInvoiceId(invoiceId);
    }

    public List<Refund> getAllRefunds() {
        return refundDao.findAll();
    }

    @Transactional
    public SimulatedPaymentResponse processSimulatedPayment(SimulatedPaymentRequest req, UUID currentUserId) {
        Invoice invoice = invoiceDao.findById(req.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + req.getInvoiceId()));

        if ("paid".equalsIgnoreCase(invoice.getStatus())) {
            throw new IllegalStateException("Invoice is already fully paid.");
        }
        if ("cancelled".equalsIgnoreCase(invoice.getStatus())) {
            throw new IllegalStateException("Cannot process payments on a cancelled invoice.");
        }

        BigDecimal balanceDue = invoice.getBalanceDue();
        if (req.getAmount().compareTo(balanceDue) > 0) {
            throw new IllegalArgumentException("Payment amount ($" + req.getAmount() + ") cannot exceed outstanding balance ($" + balanceDue + ").");
        }

        String methodPrefix = switch (req.getPaymentMethod().toLowerCase()) {
            case "credit_card" -> "SIM-CC";
            case "upi" -> "SIM-UPI";
            case "bank_transfer" -> "SIM-ACH";
            case "cash" -> "SIM-CASH";
            default -> "SIM-TXN";
        };
        String refNumber = methodPrefix + "-" + System.currentTimeMillis();

        SimulatedPaymentResponse resp = new SimulatedPaymentResponse();
        resp.setInvoiceId(invoice.getId());
        resp.setAmount(req.getAmount());
        resp.setPaymentMethod(req.getPaymentMethod());
        resp.setReferenceNumber(refNumber);
        resp.setIsSimulated(true);

        String outcome = req.getSimulatedOutcome() != null ? req.getSimulatedOutcome().toLowerCase() : "successful";
        resp.setOutcome(outcome);

        if ("failed".equalsIgnoreCase(outcome)) {
            resp.setInvoiceStatus(invoice.getStatus());
            resp.setRemainingBalance(balanceDue);
            resp.setMessage("Simulated Payment Rejected: Insufficient funds or simulator decline.");

            notificationDao.create(new Notification(
                    invoice.getCustomerId(),
                    "Payment Failed (Simulated)",
                    "A payment of $" + req.getAmount() + " for invoice " + invoice.getInvoiceNumber() + " failed: Simulated decline.",
                    "alert"
            ));

            auditLogDao.log(
                    currentUserId,
                    "PAYMENT_SIM_FAILED",
                    "payments",
                    refNumber,
                    "Simulated payment failed for invoice " + invoice.getInvoiceNumber() + " ($" + req.getAmount() + ")"
            );
            return resp;
        }

        if ("pending".equalsIgnoreCase(outcome)) {
            resp.setInvoiceStatus(invoice.getStatus());
            resp.setRemainingBalance(balanceDue);
            resp.setMessage("Simulated Payment Queued: Awaiting bank settlement.");

            notificationDao.create(new Notification(
                    invoice.getCustomerId(),
                    "Payment Processing (Simulated)",
                    "A payment of $" + req.getAmount() + " for invoice " + invoice.getInvoiceNumber() + " is currently pending settlement.",
                    "info"
            ));

            auditLogDao.log(
                    currentUserId,
                    "PAYMENT_SIM_PENDING",
                    "payments",
                    refNumber,
                    "Simulated payment pending for invoice " + invoice.getInvoiceNumber()
            );
            return resp;
        }

        // Successful Payment Simulation
        Payment payment = new Payment();
        payment.setInvoiceId(invoice.getId());
        payment.setCustomerId(invoice.getCustomerId());
        payment.setAmount(req.getAmount());
        payment.setPaymentMethod(req.getPaymentMethod());
        payment.setPaymentDate(LocalDate.now());
        payment.setReferenceNumber(refNumber);
        payment.setNotes("Simulated payment transaction via " + req.getPaymentMethod() + (req.getNotes() != null ? " - " + req.getNotes() : ""));

        Payment recorded = paymentDao.create(payment);
        resp.setPaymentId(recorded.getId());

        BigDecimal newBalance = balanceDue.subtract(req.getAmount());
        resp.setRemainingBalance(newBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newBalance);

        String newInvoiceStatus = invoice.getStatus();
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            newInvoiceStatus = "paid";
            invoiceDao.updateStatus(invoice.getId(), "paid");
        }
        resp.setInvoiceStatus(newInvoiceStatus);
        resp.setMessage("Payment of $" + req.getAmount() + " processed successfully via " + req.getPaymentMethod() + " [Demo Simulator].");

        notificationDao.create(new Notification(
                invoice.getCustomerId(),
                "Payment Received (Simulated)",
                "We received your payment of $" + req.getAmount() + " for invoice " + invoice.getInvoiceNumber() + " (" + refNumber + ").",
                "success"
        ));

        auditLogDao.log(
                currentUserId,
                "PAYMENT_SIM_SUCCESS",
                "payments",
                recorded.getId().toString(),
                "Simulated payment recorded: $" + req.getAmount() + " against " + invoice.getInvoiceNumber() + " (" + refNumber + ")"
        );

        return resp;
    }

    @Transactional
    public Refund processRefund(RefundRequest req, UUID currentUserId) {
        Payment payment = paymentDao.findById(req.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + req.getPaymentId()));

        BigDecimal alreadyRefunded = payment.getRefundedAmount() != null ? payment.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal maxRefundable = payment.getAmount().subtract(alreadyRefunded);

        if (req.getAmount().compareTo(maxRefundable) > 0) {
            throw new IllegalArgumentException("Refund amount ($" + req.getAmount() + ") cannot exceed refundable balance ($" + maxRefundable + ").");
        }

        Refund refund = new Refund();
        refund.setPaymentId(payment.getId());
        refund.setAmount(req.getAmount());
        refund.setReason(req.getReason() != null ? req.getReason() : "Customer requested simulated refund");

        Refund created = refundDao.create(refund);

        // If invoice was marked 'paid', restore status back to 'confirmed' since balance is now unpaid
        Invoice invoice = invoiceDao.findById(payment.getInvoiceId())
                .orElseThrow(() -> new IllegalStateException("Invoice not found for payment: " + payment.getInvoiceId()));

        if ("paid".equalsIgnoreCase(invoice.getStatus())) {
            invoiceDao.updateStatus(invoice.getId(), "confirmed");
        }

        notificationDao.create(new Notification(
                payment.getCustomerId(),
                "Refund Processed (Simulated)",
                "A refund of $" + req.getAmount() + " has been issued for payment " + payment.getReferenceNumber() + ".",
                "warning"
        ));

        auditLogDao.log(
                currentUserId,
                "REFUND_SIM_PROCESSED",
                "refunds",
                created.getId().toString(),
                "Processed refund of $" + req.getAmount() + " for payment " + payment.getReferenceNumber()
        );

        return created;
    }
}
