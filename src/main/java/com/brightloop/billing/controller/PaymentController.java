package com.brightloop.billing.controller;

import com.brightloop.billing.dto.ApiModels.PaymentRequest;
import com.brightloop.billing.dto.ApiModels.PaymentResult;
import com.brightloop.billing.dto.ApiModels.PaymentView;
import com.brightloop.billing.dto.ApiModels.RefundRequest;
import com.brightloop.billing.dto.ApiModels.ResolvePaymentRequest;
import com.brightloop.billing.service.PaymentSimulatorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentSimulatorService paymentSimulatorService;

    public PaymentController(PaymentSimulatorService paymentSimulatorService) {
        this.paymentSimulatorService = paymentSimulatorService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<PaymentResult> simulatePayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentSimulatorService.simulate(request));
    }

    @PostMapping("/{paymentId}/resolve")
    public ResponseEntity<PaymentResult> resolvePendingPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody ResolvePaymentRequest request) {
        return ResponseEntity.ok(paymentSimulatorService.resolvePendingPayment(paymentId, request.outcome()));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResult> simulateRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentSimulatorService.simulateRefund(paymentId, request));
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<PaymentView>> getPaymentHistory(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(paymentSimulatorService.getPaymentHistory(invoiceId));
    }
}
