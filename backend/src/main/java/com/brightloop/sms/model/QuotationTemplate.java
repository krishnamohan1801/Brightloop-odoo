package com.brightloop.sms.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class QuotationTemplate {
    private UUID id;
    private String name;
    private UUID defaultPlanId;
    private Integer validityDays;
    private OffsetDateTime createdAt;

    public QuotationTemplate() {}

    public QuotationTemplate(UUID id, String name, UUID defaultPlanId, Integer validityDays) {
        this.id = id;
        this.name = name;
        this.defaultPlanId = defaultPlanId;
        this.validityDays = validityDays;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getDefaultPlanId() { return defaultPlanId; }
    public void setDefaultPlanId(UUID defaultPlanId) { this.defaultPlanId = defaultPlanId; }

    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
