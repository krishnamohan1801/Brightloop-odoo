
package com.brightloop.quotation.controller;

import com.brightloop.quotation.model.Quotation;
import com.brightloop.quotation.model.QuotationItem;
import com.brightloop.quotation.service.QuotationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

  private final QuotationService quotationService;

  public QuotationController(QuotationService quotationService) {
    this.quotationService = quotationService;
  }

  // Create a quotation with its items
  @PostMapping
  public ResponseEntity<Quotation> createQuotation(
      @RequestBody Map<String, Object> request) {

    Quotation quotation = new Quotation();

    quotation.setQuotationNumber(
        (String) request.get("quotationNumber"));

    quotation.setCustomerId(
        UUID.fromString((String) request.get("customerId")));

    quotation.setTemplateId(
        request.get("templateId") != null
            ? UUID.fromString((String) request.get("templateId"))
            : null);

    quotation.setPlanId(
        UUID.fromString((String) request.get("planId")));

    quotation.setDiscountId(
        request.get("discountId") != null
            ? UUID.fromString((String) request.get("discountId"))
            : null);

    quotation.setValidUntil(
        java.time.LocalDate.parse(
            (String) request.get("validUntil")));

    quotation.setPaymentTerms(
        (String) request.get("paymentTerms"));

    quotation.setNotes(
        (String) request.get("notes"));

    quotation.setCreatedBy(
        request.get("createdBy") != null
            ? UUID.fromString((String) request.get("createdBy"))
            : null);

        Object rawItems = request.get("items");
        if (!(rawItems instanceof List<?> rawItemList)) {
            throw new IllegalArgumentException("items must be a list");
        }

        List<Map<String, Object>> itemRequests = new java.util.ArrayList<>();
        for (Object rawItem : rawItemList) {
            if (!(rawItem instanceof Map<?, ?> rawMap)) {
                throw new IllegalArgumentException("Each item must be an object");
            }

            Map<String, Object> itemRequest = new java.util.HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("Item keys must be strings");
                }
                itemRequest.put(key, entry.getValue());
            }
            itemRequests.add(itemRequest);
        }

    List<QuotationItem> items = itemRequests.stream()
        .map(this::mapToQuotationItem)
        .toList();

    Quotation createdQuotation = quotationService.createQuotation(quotation, items);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdQuotation);
  }

  private QuotationItem mapToQuotationItem(
      Map<String, Object> itemRequest) {

    QuotationItem item = new QuotationItem();

    item.setProductId(
        UUID.fromString((String) itemRequest.get("productId")));

    item.setVariantId(
        itemRequest.get("variantId") != null
            ? UUID.fromString((String) itemRequest.get("variantId"))
            : null);

    item.setQuantity(
        ((Number) itemRequest.get("quantity")).intValue());

    item.setUnitPrice(
        new java.math.BigDecimal(
            itemRequest.get("unitPrice").toString()));

    item.setTaxId(
        itemRequest.get("taxId") != null
            ? UUID.fromString((String) itemRequest.get("taxId"))
            : null);

    item.setTaxRate(
        itemRequest.get("taxRate") != null
            ? new java.math.BigDecimal(
                itemRequest.get("taxRate").toString())
            : java.math.BigDecimal.ZERO);

    item.setDiscountAmount(
        itemRequest.get("discountAmount") != null
            ? new java.math.BigDecimal(
                itemRequest.get("discountAmount").toString())
            : java.math.BigDecimal.ZERO);

    item.setDescription(
        (String) itemRequest.get("description"));

    return item;
  }

  // Get all quotations
  @GetMapping
  public ResponseEntity<List<Quotation>> getAllQuotations() {

    return ResponseEntity.ok(
        quotationService.getAllQuotations());
  }

  // Get quotation by ID
  @GetMapping("/{id}")
  public ResponseEntity<Quotation> getQuotationById(
      @PathVariable UUID id) {

    return ResponseEntity.ok(
        quotationService.getQuotationById(id));
  }

  // Get quotations by customer
  @GetMapping("/customer/{customerId}")
  public ResponseEntity<List<Quotation>> getQuotationsByCustomer(
      @PathVariable UUID customerId) {

    return ResponseEntity.ok(
        quotationService.getQuotationsByCustomer(customerId));
  }

  // Get quotations by status
  @GetMapping("/status/{status}")
  public ResponseEntity<List<Quotation>> getQuotationsByStatus(
      @PathVariable String status) {

    return ResponseEntity.ok(
        quotationService.getQuotationsByStatus(status));
  }

  // Get all items belonging to a quotation
  @GetMapping("/{id}/items")
  public ResponseEntity<List<QuotationItem>> getQuotationItems(
      @PathVariable UUID id) {

    return ResponseEntity.ok(
        quotationService.getQuotationItems(id));
  }

  // Update quotation status
  @PatchMapping("/{id}/status")
  public ResponseEntity<Map<String, String>> updateQuotationStatus(
      @PathVariable UUID id,
      @RequestBody Map<String, String> request) {

    quotationService.updateQuotationStatus(
        id,
        request.get("status"));

    return ResponseEntity.ok(
        Map.of("message", "Quotation status updated successfully."));
  }
}