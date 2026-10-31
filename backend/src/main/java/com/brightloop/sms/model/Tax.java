package com.brightloop.sms.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class Tax {
    private UUID id;
    private String name;
    private BigDecimal rate; // percentage e.g. 18.00
    private Boolean isActive;
    private OffsetDateTime createdAt;

    public Tax() {}

    public Tax(UUID id, String name, BigDecimal rate, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.rate = rate;
        this.isActive = isActive;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
