package com.brightloop.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "billing.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);

    private final BillingService billingService;

    public BillingScheduler(BillingService billingService) {
        this.billingService = billingService;
    }

    @Scheduled(cron = "${billing.scheduler.cron:0 0 0 * * *}")
    public void runDailyBilling() {
        log.info("Scheduled task triggered: Running daily billing check...");
        try {
            billingService.runBillingEngine();
        } catch (Exception e) {
            log.error("Scheduled billing execution failed: {}", e.getMessage(), e);
        }
    }
}
