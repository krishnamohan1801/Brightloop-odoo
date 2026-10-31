package com.brightloop.sms.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class SimulatedPaymentResponse {
    private UUID paymentId;
    private UUID invoiceId;
    private BigDecimal amount;
    private String paymentMethod;
    private String outcome;
    private String referenceNumber;
    private String invoiceStatus;
    private BigDecimal remainingBalance;
    private String message;
    private Boolean isSimulated = true;

    public SimulatedPaymentResponse() {}

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }

    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsSimulated() { return isSimulated; }
    public void setIsSimulated(Boolean simulated) { isSimulated = simulated; }
}
