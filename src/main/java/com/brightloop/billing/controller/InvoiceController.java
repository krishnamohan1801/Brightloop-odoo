package com.brightloop.billing.controller;

import com.brightloop.billing.dto.ApiModels.BillingRunResult;
import com.brightloop.billing.dto.ApiModels.InvoiceDetail;
import com.brightloop.billing.dto.ApiModels.InvoiceSummary;
import com.brightloop.billing.service.BillingService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final BillingService billingService;

    public InvoiceController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceSummary>> getInvoices(
            @RequestParam(required = false) UUID customerId) {
        return ResponseEntity.ok(billingService.getInvoices(customerId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<InvoiceSummary>> getOverdueInvoices() {
        return ResponseEntity.ok(billingService.getOverdueInvoices());
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceDetail> getInvoice(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(billingService.getInvoiceDetail(invoiceId));
    }

    @PostMapping("/subscriptions/{subscriptionId}/generate")
    public ResponseEntity<InvoiceSummary> generateForSubscription(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(billingService.generateDueInvoice(subscriptionId));
    }

    @PostMapping("/billing/run")
    public ResponseEntity<BillingRunResult> runBillingNow() {
        return ResponseEntity.ok(billingService.runDueBilling());
    }

    @PostMapping("/create-sample")
    public ResponseEntity<InvoiceSummary> createSampleInvoice() {
        return ResponseEntity.ok(billingService.createSampleInvoice());
    }

    @PostMapping("/{invoiceId}/cancel")
    public ResponseEntity<Void> cancelInvoice(@PathVariable UUID invoiceId) {
        billingService.cancelInvoice(invoiceId);
        return ResponseEntity.noContent().build();
    }
}
