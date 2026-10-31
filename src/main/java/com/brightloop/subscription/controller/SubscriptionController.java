
package com.brightloop.subscription.controller;

import com.brightloop.subscription.model.Subscription;
import com.brightloop.subscription.model.SubscriptionItem;
import com.brightloop.subscription.service.SubscriptionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  public SubscriptionController(
      SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  // Create subscription with items
  @PostMapping
  public ResponseEntity<Subscription> createSubscription(
      @RequestBody Map<String, Object> request) {

    Subscription subscription = convertSubscription(request);

    List<SubscriptionItem> items = convertItems(request);

    Subscription created = subscriptionService.createSubscription(
        subscription, items);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(created);
  }

  // Get all subscriptions
  @GetMapping
  public ResponseEntity<List<Subscription>> getAllSubscriptions() {

    return ResponseEntity.ok(
        subscriptionService.getAllSubscriptions());
  }

  // Get subscription by ID
  @GetMapping("/{id}")
  public ResponseEntity<Subscription> getSubscriptionById(
      @PathVariable UUID id) {

    return ResponseEntity.ok(
        subscriptionService.getSubscriptionById(id));
  }

  // Get subscriptions by customer
  @GetMapping("/customer/{customerId}")
  public ResponseEntity<List<Subscription>> getByCustomer(
      @PathVariable UUID customerId) {

    return ResponseEntity.ok(
        subscriptionService.getSubscriptionsByCustomer(
            customerId));
  }

  // Get subscription items
  @GetMapping("/{id}/items")
  public ResponseEntity<List<SubscriptionItem>> getItems(
      @PathVariable UUID id) {

    return ResponseEntity.ok(
        subscriptionService.getSubscriptionItems(id));
  }

  // Update subscription status
  @PatchMapping("/{id}/status")
  public ResponseEntity<Subscription> updateStatus(
      @PathVariable UUID id,
      @RequestBody Map<String, String> request) {

    return ResponseEntity.ok(
        subscriptionService.updateStatus(
            id, request.get("status")));
  }

  // Pause subscription
  @PatchMapping("/{id}/pause")
  public ResponseEntity<Subscription> pause(
      @PathVariable UUID id) {

    return ResponseEntity.ok(
        subscriptionService.pauseSubscription(id));
  }

  // Resume subscription
  @PatchMapping("/{id}/resume")
  public ResponseEntity<Subscription> resume(
      @PathVariable UUID id) {

    return ResponseEntity.ok(
        subscriptionService.resumeSubscription(id));
  }

  // Close subscription
  @PatchMapping("/{id}/close")
  public ResponseEntity<Subscription> close(
      @PathVariable UUID id) {

    return ResponseEntity.ok(
        subscriptionService.closeSubscription(id));
  }

  // Convert request map to Subscription
  private Subscription convertSubscription(
      Map<String, Object> request) {

    Subscription subscription = new Subscription();

    subscription.setSubscriptionNumber(
        (String) request.get("subscriptionNumber"));

    subscription.setCustomerId(
        parseUUID(request.get("customerId")));

    subscription.setPlanId(
        parseUUID(request.get("planId")));

    subscription.setDiscountId(
        parseUUID(request.get("discountId")));

    subscription.setStatus(
        (String) request.get("status"));

    subscription.setPaymentTerms(
        (String) request.get("paymentTerms"));

    subscription.setStartDate(
        parseDate(request.get("startDate")));

    subscription.setNextBillingDate(
        parseDate(request.get("nextBillingDate")));

    subscription.setExpiryDate(
        parseDate(request.get("expiryDate")));

    subscription.setNotes(
        (String) request.get("notes"));

    subscription.setCreatedBy(
        parseUUID(request.get("createdBy")));

    return subscription;
  }

  // Convert request items
  @SuppressWarnings("unchecked")
  private List<SubscriptionItem> convertItems(
      Map<String, Object> request) {

    Object itemsObject = request.get("items");

    if (!(itemsObject instanceof List<?> itemsList)) {
      return List.of();
    }

    return itemsList.stream()
        .map(itemObject -> {

          Map<String, Object> itemMap = (Map<String, Object>) itemObject;

          SubscriptionItem item = new SubscriptionItem();

          item.setProductId(
              parseUUID(itemMap.get("productId")));

          item.setVariantId(
              parseUUID(itemMap.get("variantId")));

          item.setQuantity(
              itemMap.get("quantity") == null
                  ? null
                  : Integer.valueOf(
                      itemMap.get("quantity")
                          .toString()));

          item.setUnitPrice(
              itemMap.get("unitPrice") == null
                  ? null
                  : new java.math.BigDecimal(
                      itemMap.get("unitPrice")
                          .toString()));

          item.setTaxId(
              parseUUID(itemMap.get("taxId")));

          item.setSubtotal(
              itemMap.get("subtotal") == null
                  ? null
                  : new java.math.BigDecimal(
                      itemMap.get("subtotal")
                          .toString()));

          return item;
        })
        .toList();
  }

  private UUID parseUUID(Object value) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return UUID.fromString(value.toString());
  }

  private java.time.LocalDate parseDate(Object value) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return java.time.LocalDate.parse(value.toString());
  }
}