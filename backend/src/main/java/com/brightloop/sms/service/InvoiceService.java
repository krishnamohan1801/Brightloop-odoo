package com.brightloop.sms.service;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.InvoiceDao;
import com.brightloop.sms.dao.PaymentDao;
import com.brightloop.sms.model.Invoice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceDao invoiceDao;
    private final PaymentDao paymentDao;
    private final AuditLogDao auditLogDao;

    public InvoiceService(InvoiceDao invoiceDao, PaymentDao paymentDao, AuditLogDao auditLogDao) {
        this.invoiceDao = invoiceDao;
        this.paymentDao = paymentDao;
        this.auditLogDao = auditLogDao;
    }

    public List<Invoice> getAllInvoices(String status, UUID customerId, Boolean overdueOnly) {
        return invoiceDao.findAll(status, customerId, overdueOnly);
    }

    public Invoice getInvoiceById(UUID id) {
        Invoice invoice = invoiceDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));
        invoice.setPayments(paymentDao.findByInvoiceId(id));
        return invoice;
    }

    public Invoice confirmInvoice(UUID id, UUID userId) {
        Invoice invoice = getInvoiceById(id);
        if (!"draft".equalsIgnoreCase(invoice.getStatus())) {
            throw new IllegalStateException("Only draft invoices can be confirmed.");
        }
        invoiceDao.updateStatus(id, "confirmed");
        invoice.setStatus("confirmed");
        auditLogDao.log(userId, "INVOICE_CONFIRM", "invoices", id.toString(), "Confirmed invoice: " + invoice.getInvoiceNumber());
        return invoice;
    }

    public Invoice cancelInvoice(UUID id, String reason, UUID userId) {
        Invoice invoice = getInvoiceById(id);
        if ("paid".equalsIgnoreCase(invoice.getStatus())) {
            throw new IllegalStateException("Paid invoices cannot be cancelled directly. Process a refund first.");
        }
        invoiceDao.updateStatus(id, "cancelled");
        invoice.setStatus("cancelled");
        auditLogDao.log(userId, "INVOICE_CANCEL", "invoices", id.toString(), "Cancelled invoice: " + invoice.getInvoiceNumber() + " Reason: " + reason);
        return invoice;
    }
}
