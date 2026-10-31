package com.brightloop.sms.controller;

import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.Product;
import com.brightloop.sms.model.ProductVariant;
import com.brightloop.sms.model.User;
import com.brightloop.sms.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Catalog", description = "Product and product variant catalog management")
public class ProductController {

    private final ProductService productService;
    private final UserDao userDao;

    public ProductController(ProductService productService, UserDao userDao) {
        this.productService = productService;
        this.userDao = userDao;
    }

    @GetMapping
    @Operation(summary = "List all products with variants")
    public ResponseEntity<List<Product>> getProducts(@RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(productService.getAllProducts(activeOnly));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single product with variants by id")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create product")
    public ResponseEntity<Product> createProduct(@RequestBody Product product, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(productService.createProduct(product, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update product")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product product, Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(productService.updateProduct(id, product, userId));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Archive or restore product")
    public ResponseEntity<Void> archiveProduct(@PathVariable UUID id, @RequestParam boolean active, Authentication authentication) {
        UUID userId = getUserId(authentication);
        productService.setProductActive(id, active, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Add variant to product")
    public ResponseEntity<ProductVariant> addVariant(
            @PathVariable UUID productId,
            @RequestBody ProductVariant variant,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(productService.addVariant(productId, variant, userId));
    }

    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update product variant")
    public ResponseEntity<ProductVariant> updateVariant(
            @PathVariable UUID variantId,
            @RequestBody ProductVariant variant,
            Authentication authentication) {
        UUID userId = getUserId(authentication);
        return ResponseEntity.ok(productService.updateVariant(variantId, variant, userId));
    }

    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Delete product variant")
    public ResponseEntity<Void> deleteVariant(@PathVariable UUID variantId, Authentication authentication) {
        UUID userId = getUserId(authentication);
        productService.deleteVariant(variantId, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserId(Authentication auth) {
        if (auth == null) return null;
        return userDao.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}
