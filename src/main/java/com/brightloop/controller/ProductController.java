package com.brightloop.controller;

import com.brightloop.dto.ProductRequest;
import com.brightloop.model.Product;
import com.brightloop.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<UUID> createProduct(
        @Valid @RequestBody ProductRequest request
    ) {

        UUID productId = productService.createProduct(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(productId);
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {

        return ResponseEntity.ok(
            productService.getAllProducts()
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
        @PathVariable UUID id
    ) {

        Optional<Product> product =
            productService.getProductById(id);

        return product
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(
        @PathVariable UUID id,
        @Valid @RequestBody ProductRequest request
    ) {

        boolean updated =
            productService.updateProduct(id, request);

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
            "Product updated successfully"
        );
    }

    // ARCHIVE
    @PatchMapping("/{id}/archive")
    public ResponseEntity<String> archiveProduct(
        @PathVariable UUID id
    ) {

        boolean archived =
            productService.archiveProduct(id);

        if (!archived) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
            "Product archived successfully"
        );
    }
}