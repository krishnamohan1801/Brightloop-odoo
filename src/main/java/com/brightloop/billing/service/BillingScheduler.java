package com.brightloop.billing.service;

import com.brightloop.billing.dto.ApiModels.BillingRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "brightloop.billing.enabled", havingValue = "true", matchIfMissing = true)
public class BillingScheduler {
    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);
    private final BillingService billingService;

    public BillingScheduler(BillingService billingService) {
        this.billingService = billingService;
    }

    @Scheduled(cron = "${brightloop.billing.cron:0 0 1 * * *}")
    public void generateDueInvoices() {
        BillingRunResult result = billingService.runDueBilling();
        log.info("Scheduled billing complete. Due subscriptions: {}, invoices generated: {}",
                result.dueSubscriptions(), result.invoicesGenerated());
    }
}
