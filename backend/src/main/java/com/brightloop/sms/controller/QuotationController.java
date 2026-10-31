package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.QuotationTemplate;
import com.brightloop.sms.model.Subscription;
import com.brightloop.sms.model.SubscriptionItem;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quotations")
@Tag(name = "Quotations", description = "Quotation templates and sales quotation offers")
public class QuotationController {

    private final QuotationService quotationService;
    private final UserDao userDao;

    public QuotationController(QuotationService quotationService, UserDao userDao) {
        this.quotationService = quotationService;
        this.userDao = userDao;
    }

    @GetMapping("/templates")
    @Operation(summary = "List all quotation templates")
    public ResponseEntity<List<QuotationTemplate>> getTemplates() {
        return ResponseEntity.ok(quotationService.getAllTemplates());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create quotation template")
    public ResponseEntity<QuotationTemplate> createTemplate(@RequestBody QuotationTemplate template, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(quotationService.createTemplate(template, userId));
    }

    @GetMapping
    @Operation(summary = "List quotations (filtered by customer if customer role)")
    public ResponseEntity<List<Subscription>> getQuotations(
            @RequestParam(required = false) UUID customerId,
            Authentication authentication) {
        User user = getUser(authentication);
        UUID filterCustomerId = customerId;
        if (user != null && "customer".equalsIgnoreCase(user.getRole())) {
            filterCustomerId = user.getId();
        }
        return ResponseEntity.ok(quotationService.getAllQuotations(filterCustomerId));
    }

    public static class CreateQuoteRequest {
        private Subscription quotation;
        private List<SubscriptionItem> items;
        public Subscription getQuotation() { return quotation; }
        public void setQuotation(Subscription quotation) { this.quotation = quotation; }
        public List<SubscriptionItem> getItems() { return items; }
        public void setItems(List<SubscriptionItem> items) { this.items = items; }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Prepare quotation offer")
    public ResponseEntity<Subscription> createQuotation(
            @RequestBody CreateQuoteRequest request,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(quotationService.createQuotation(request.getQuotation(), request.getItems(), userId));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept quotation and convert to active subscription")
    public ResponseEntity<Subscription> acceptQuotation(@PathVariable UUID id, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(quotationService.acceptQuotation(id, userId));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject quotation")
    public ResponseEntity<Subscription> rejectQuotation(@PathVariable UUID id, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(quotationService.rejectQuotation(id, userId));
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
