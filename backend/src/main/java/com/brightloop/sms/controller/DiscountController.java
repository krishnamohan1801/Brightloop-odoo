package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.Discount;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/discounts")
@Tag(name = "Discounts & Promotions", description = "Discount codes, limits, and scope rules")
public class DiscountController {

    private final DiscountService discountService;
    private final UserDao userDao;

    public DiscountController(DiscountService discountService, UserDao userDao) {
        this.discountService = discountService;
        this.userDao = userDao;
    }

    @GetMapping
    @Operation(summary = "List all discounts")
    public ResponseEntity<List<Discount>> getDiscounts(@RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(discountService.getAllDiscounts(activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create discount rule (Admin only)")
    public ResponseEntity<Discount> createDiscount(@RequestBody Discount discount, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(discountService.createDiscount(discount, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update discount rule (Admin only)")
    public ResponseEntity<Discount> updateDiscount(@PathVariable UUID id, @RequestBody Discount discount, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(discountService.updateDiscount(id, discount, userId));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive or restore discount rule (Admin only)")
    public ResponseEntity<Void> archiveDiscount(@PathVariable UUID id, @RequestParam boolean active, Authentication authentication) {
        UUID userId = getUserId(authentication);
        discountService.setDiscountActive(id, active, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserId(Authentication auth) {
        if (auth == null) return null;
        return userDao.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
