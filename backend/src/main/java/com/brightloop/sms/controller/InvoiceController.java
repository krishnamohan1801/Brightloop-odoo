package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.Invoice;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Recurring invoice management, balances, and overdue tracking")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserDao userDao;

    public InvoiceController(InvoiceService invoiceService, UserDao userDao) {
        this.invoiceService = invoiceService;
        this.userDao = userDao;
    }

    @GetMapping
    @Operation(summary = "List invoices")
    public ResponseEntity<List<Invoice>> getInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) Boolean overdueOnly,
            Authentication authentication) {
        User user = getUser(authentication);
        UUID filterCustomerId = customerId;
        if (user != null && "customer".equalsIgnoreCase(user.getRole())) {
            filterCustomerId = user.getId();
        }
        return ResponseEntity.ok(invoiceService.getAllInvoices(status, filterCustomerId, overdueOnly));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single invoice with items and payments")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable UUID id, Authentication authentication) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        User user = getUser(authentication);
        if (user != null && "customer".equalsIgnoreCase(user.getRole())) {
            if (!invoice.getCustomerId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(invoice);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Confirm draft invoice")
    public ResponseEntity<Invoice> confirmInvoice(@PathVariable UUID id, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(invoiceService.confirmInvoice(id, userId));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Cancel invoice")
    public ResponseEntity<Invoice> cancelInvoice(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "Cancelled by staff") String reason,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(invoiceService.cancelInvoice(id, reason, userId));
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
