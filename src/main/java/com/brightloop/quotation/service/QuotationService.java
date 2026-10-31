
package com.brightloop.quotation.service;

import com.brightloop.quotation.dao.QuotationDAO;
import com.brightloop.quotation.dao.QuotationItemDAO;
import com.brightloop.quotation.model.Quotation;
import com.brightloop.quotation.model.QuotationItem;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class QuotationService {

  private final QuotationDAO quotationDAO;
  private final QuotationItemDAO quotationItemDAO;

  public QuotationService(
      QuotationDAO quotationDAO,
      QuotationItemDAO quotationItemDAO) {
    this.quotationDAO = quotationDAO;
    this.quotationItemDAO = quotationItemDAO;
  }

  @Transactional
  public Quotation createQuotation(
      Quotation quotation,
      List<QuotationItem> items) {

    if (quotation == null) {
      throw new IllegalArgumentException(
          "Quotation details are required.");
    }

    if (quotation.getCustomerId() == null) {
      throw new IllegalArgumentException(
          "Customer ID is required.");
    }

    if (quotation.getPlanId() == null) {
      throw new IllegalArgumentException(
          "Billing plan ID is required.");
    }

    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException(
          "A quotation must contain at least one item.");
    }

    LocalDate issueDate = quotation.getIssueDate() != null
        ? quotation.getIssueDate()
        : LocalDate.now();

    if (quotation.getValidUntil() == null) {
      throw new IllegalArgumentException(
          "Quotation validity date is required.");
    }

    if (quotation.getValidUntil().isBefore(issueDate)) {
      throw new IllegalArgumentException(
          "Validity date cannot be before issue date.");
    }

    quotation.setIssueDate(issueDate);
    quotation.setStatus("draft");

    if (quotation.getQuotationNumber() == null
        || quotation.getQuotationNumber().isBlank()) {

      quotation.setQuotationNumber(generateQuotationNumber());
    }

    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal discountAmount = BigDecimal.ZERO;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal totalAmount = BigDecimal.ZERO;

    for (QuotationItem item : items) {

      validateItem(item);

      BigDecimal lineSubtotal = item.getUnitPrice()
          .multiply(BigDecimal.valueOf(item.getQuantity()))
          .setScale(2, RoundingMode.HALF_UP);

      BigDecimal lineDiscount = item.getDiscountAmount() != null
          ? item.getDiscountAmount()
          : BigDecimal.ZERO;

      BigDecimal taxRate = item.getTaxRate() != null
          ? item.getTaxRate()
          : BigDecimal.ZERO;

      if (lineDiscount.compareTo(lineSubtotal) > 0) {
        throw new IllegalArgumentException(
            "Item discount cannot exceed its subtotal.");
      }

      BigDecimal calculatedTax = lineSubtotal
          .subtract(lineDiscount)
          .multiply(taxRate)
          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

      BigDecimal lineTotal = lineSubtotal
          .subtract(lineDiscount)
          .add(calculatedTax);

      item.setSubtotal(lineSubtotal);
      item.setDiscountAmount(lineDiscount);
      item.setTaxRate(taxRate);
      item.setTaxAmount(calculatedTax);
      item.setTotalAmount(lineTotal);

      subtotal = subtotal.add(lineSubtotal);
      discountAmount = discountAmount.add(lineDiscount);
      taxAmount = taxAmount.add(calculatedTax);
      totalAmount = totalAmount.add(lineTotal);
    }

    quotation.setSubtotal(subtotal);
    quotation.setDiscountAmount(discountAmount);
    quotation.setTaxAmount(taxAmount);
    quotation.setTotalAmount(totalAmount);

    Quotation savedQuotation = quotationDAO.save(quotation);

    for (QuotationItem item : items) {
      item.setQuotationId(savedQuotation.getId());
    }

    quotationItemDAO.saveAll(items);

    return savedQuotation;
  }

  private void validateItem(QuotationItem item) {

    if (item == null) {
      throw new IllegalArgumentException(
          "Quotation item cannot be empty.");
    }

    if (item.getProductId() == null) {
      throw new IllegalArgumentException(
          "Product ID is required for every item.");
    }

    if (item.getQuantity() == null || item.getQuantity() <= 0) {
      throw new IllegalArgumentException(
          "Item quantity must be greater than zero.");
    }

    if (item.getUnitPrice() == null
        || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {

      throw new IllegalArgumentException(
          "Unit price cannot be negative.");
    }

    if (item.getDiscountAmount() != null
        && item.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {

      throw new IllegalArgumentException(
          "Discount amount cannot be negative.");
    }

    if (item.getTaxRate() != null
        && (item.getTaxRate().compareTo(BigDecimal.ZERO) < 0
            || item.getTaxRate().compareTo(BigDecimal.valueOf(100)) > 0)) {

      throw new IllegalArgumentException(
          "Tax rate must be between 0 and 100.");
    }
  }

  private String generateQuotationNumber() {

    String quotationNumber;

    do {
      quotationNumber = "QUO-" + System.currentTimeMillis()
          + "-" + UUID.randomUUID()
              .toString()
              .substring(0, 4)
              .toUpperCase();

    } while (quotationDAO.existsByQuotationNumber(quotationNumber));

    return quotationNumber;
  }

  public List<Quotation> getAllQuotations() {
    return quotationDAO.findAll();
  }

  public Quotation getQuotationById(UUID id) {

    return quotationDAO.findById(id)
        .orElseThrow(() -> new IllegalArgumentException(
            "Quotation not found."));
  }

  public List<Quotation> getQuotationsByCustomer(UUID customerId) {

    return quotationDAO.findByCustomerId(customerId);
  }

  public List<Quotation> getQuotationsByStatus(String status) {

    validateStatus(status);

    return quotationDAO.findByStatus(status);
  }

  public List<QuotationItem> getQuotationItems(UUID quotationId) {

    getQuotationById(quotationId);

    return quotationItemDAO.findByQuotationId(quotationId);
  }

  public void updateQuotationStatus(UUID id, String status) {

    getQuotationById(id);
    validateStatus(status);

    quotationDAO.updateStatus(id, status);
  }

  private void validateStatus(String status) {

    List<String> allowedStatuses = List.of(
        "draft",
        "sent",
        "accepted",
        "rejected",
        "expired");

    if (status == null
        || !allowedStatuses.contains(status.toLowerCase())) {

      throw new IllegalArgumentException(
          "Invalid quotation status.");
    }
  }
}