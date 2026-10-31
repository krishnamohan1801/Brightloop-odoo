package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.Tax;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/taxes")
@Tag(name = "Tax Rules", description = "Tax rates (GST, VAT, etc.) configuration")
public class TaxController {

    private final TaxService taxService;
    private final UserDao userDao;

    public TaxController(TaxService taxService, UserDao userDao) {
        this.taxService = taxService;
        this.userDao = userDao;
    }

    @GetMapping
    @Operation(summary = "List all tax rules")
    public ResponseEntity<List<Tax>> getTaxes(@RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(taxService.getAllTaxes(activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tax rule (Admin only)")
    public ResponseEntity<Tax> createTax(@RequestBody Tax tax, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(taxService.createTax(tax, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tax rule (Admin only)")
    public ResponseEntity<Tax> updateTax(@PathVariable UUID id, @RequestBody Tax tax, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(taxService.updateTax(id, tax, userId));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Archive or restore tax rule (Admin only)")
    public ResponseEntity<Void> archiveTax(@PathVariable UUID id, @RequestParam boolean active, Authentication authentication) {
        UUID userId = getUserId(authentication);
        taxService.setTaxActive(id, active, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserId(Authentication auth) {
        if (auth == null) return null;
        return userDao.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
