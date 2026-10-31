package com.brightloop.sms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class SimulatedPaymentRequest {
    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // bank_transfer, credit_card, cash, upi, other

    private String simulatedOutcome = "successful"; // successful, failed, pending
    private String notes;

    public SimulatedPaymentRequest() {}

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSimulatedOutcome() { return simulatedOutcome; }
    public void setSimulatedOutcome(String simulatedOutcome) { this.simulatedOutcome = simulatedOutcome; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
