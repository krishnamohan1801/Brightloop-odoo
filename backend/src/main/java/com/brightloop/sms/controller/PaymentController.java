package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.dto.RefundRequest;
import com.brightloop.sms.dto.SimulatedPaymentRequest;
import com.brightloop.sms.dto.SimulatedPaymentResponse;
import com.brightloop.sms.model.Payment;
import com.brightloop.sms.model.Refund;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.PaymentSimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Simulator", description = "Simulated payments (UPI, Card, Bank, Cash) and simulated refunds")
public class PaymentController {

    private final PaymentSimulatorService paymentSimulatorService;
    private final UserDao userDao;

    public PaymentController(PaymentSimulatorService paymentSimulatorService, UserDao userDao) {
        this.paymentSimulatorService = paymentSimulatorService;
        this.userDao = userDao;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simulate payment transaction (Credit Card, UPI, Bank Transfer, Cash)")
    public ResponseEntity<SimulatedPaymentResponse> simulatePayment(
            @Valid @RequestBody SimulatedPaymentRequest request,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(paymentSimulatorService.processSimulatedPayment(request, userId));
    }

    @GetMapping
    @Operation(summary = "List payment transaction history")
    public ResponseEntity<List<Payment>> getPayments(
            @RequestParam(required = false) UUID customerId,
            Authentication authentication) {
        User user = getUser(authentication);
        UUID filterCustomerId = customerId;
        if (user != null && "customer".equalsIgnoreCase(user.getRole())) {
            filterCustomerId = user.getId();
        }
        return ResponseEntity.ok(paymentSimulatorService.getAllPayments(filterCustomerId));
    }

    @GetMapping("/invoice/{invoiceId}")
    @Operation(summary = "Get all payments for a specific invoice")
    public ResponseEntity<List<Payment>> getInvoicePayments(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(paymentSimulatorService.getPaymentsForInvoice(invoiceId));
    }

    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Process simulated refund")
    public ResponseEntity<Refund> processRefund(
            @Valid @RequestBody RefundRequest request,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(paymentSimulatorService.processRefund(request, userId));
    }

    @GetMapping("/refunds")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List simulated refunds")
    public ResponseEntity<List<Refund>> getRefunds() {
        return ResponseEntity.ok(paymentSimulatorService.getAllRefunds());
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userDao.findByEmail(auth.getName()).orElse(null);
    }

    private UUID getUserId(Authentication auth) {
        User u = getUser(auth);
        return u != null ? u.getId() : null;
    }
}
