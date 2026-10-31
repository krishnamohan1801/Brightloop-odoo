
package com.brightloop.discount.service.controller;

import com.brightloop.discount.model.Discount;
import com.brightloop.discount.service.DiscountService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

  private final DiscountService discountService;

  public DiscountController(DiscountService discountService) {
    this.discountService = discountService;
  }

  // Create discount
  @PostMapping
  public ResponseEntity<Discount> createDiscount(
      @RequestBody Discount discount) {

    Discount created = discountService.createDiscount(discount);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(created);
  }

  // Get all discounts
  @GetMapping
  public ResponseEntity<List<Discount>> getAllDiscounts() {

    return ResponseEntity.ok(
        discountService.getAllDiscounts());
  }

  // Get discount by ID
  @GetMapping("/{id}")
  public ResponseEntity<Discount> getDiscountById(
      @PathVariable UUID id) {

    return ResponseEntity.ok(
        discountService.getDiscountById(id));
  }

  // Get discount by code
  @GetMapping("/code/{code}")
  public ResponseEntity<Discount> getDiscountByCode(
      @PathVariable String code) {

    return ResponseEntity.ok(
        discountService.getDiscountByCode(code));
  }

  // Update discount
  @PutMapping("/{id}")
  public ResponseEntity<Discount> updateDiscount(
      @PathVariable UUID id,
      @RequestBody Discount discount) {

    return ResponseEntity.ok(
        discountService.updateDiscount(id, discount));
  }

  // Activate or deactivate discount
  @PatchMapping("/{id}/status")
  public ResponseEntity<Discount> updateStatus(
      @PathVariable UUID id,
      @RequestBody Map<String, Boolean> request) {

    Boolean active = request.get("active");

    if (active == null) {
      return ResponseEntity.badRequest().build();
    }

    return ResponseEntity.ok(
        discountService.updateActiveStatus(id, active));
  }

  // Validate discount code
  @GetMapping("/validate/{code}")
  public ResponseEntity<Discount> validateDiscount(
      @PathVariable String code) {

    return ResponseEntity.ok(
        discountService.validateDiscountCode(code));
  }
}