package com.brightloop.controller;

import com.brightloop.dto.ProductVariantRequest;
import com.brightloop.model.ProductVariant;
import com.brightloop.service.ProductVariantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/variants")
public class ProductVariantController {

    private final ProductVariantService variantService;

    public ProductVariantController(ProductVariantService variantService) {
        this.variantService = variantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductVariantRequest request) {

        return variantService.create(productId, request);
    }

    @GetMapping
    public List<ProductVariant> getAll(
            @PathVariable UUID productId) {

        return variantService.getAll(productId);
    }

    @GetMapping("/{variantId}")
    public ProductVariant getById(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {

        return variantService.getById(productId, variantId);
    }

    @PutMapping("/{variantId}")
    public String update(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody ProductVariantRequest request) {

        variantService.update(productId, variantId, request);

        return "Product variant updated successfully";
    }

    @DeleteMapping("/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {

        variantService.delete(productId, variantId);
    }
}