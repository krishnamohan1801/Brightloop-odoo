package com.brightloop.sms.controller;

import com.brightloop.sms.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing Scheduler", description = "Automated recurring billing engine triggers")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/run-job")
    @Operation(summary = "Manually trigger the daily recurring billing job")
    public ResponseEntity<Map<String, Object>> runBillingJob() {
        return ResponseEntity.ok(billingService.runBillingEngine());
    }
}
