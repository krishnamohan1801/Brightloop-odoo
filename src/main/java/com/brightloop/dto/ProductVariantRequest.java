package com.brightloop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductVariantRequest {

    @NotBlank(message = "Variant name is required")
    @Size(max = 100, message = "Variant name cannot exceed 100 characters")
    private String variantName;

    @NotNull(message = "Extra price is required")
    @DecimalMin(value = "0.00", message = "Extra price cannot be negative")
    private BigDecimal extraPrice;

    @Size(max = 80, message = "SKU cannot exceed 80 characters")
    private String sku;

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public BigDecimal getExtraPrice() {
        return extraPrice;
    }

    public void setExtraPrice(BigDecimal extraPrice) {
        this.extraPrice = extraPrice;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}