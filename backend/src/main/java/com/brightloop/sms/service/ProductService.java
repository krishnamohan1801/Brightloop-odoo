package com.brightloop.sms.service;

import com.brightloop.sms.dao.AuditLogDao;
import com.brightloop.sms.dao.ProductDao;
import com.brightloop.sms.model.Product;
import com.brightloop.sms.model.ProductVariant;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductDao productDao;
    private final AuditLogDao auditLogDao;

    public ProductService(ProductDao productDao, AuditLogDao auditLogDao) {
        this.productDao = productDao;
        this.auditLogDao = auditLogDao;
    }

    public List<Product> getAllProducts(Boolean activeOnly) {
        return productDao.findAll(activeOnly);
    }

    public Product getProductById(UUID id) {
        return productDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    public Product createProduct(Product product, UUID userId) {
        if (product.getSalesPrice() == null || product.getSalesPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sales price must be greater than or equal to 0");
        }
        Product created = productDao.create(product);
        auditLogDao.log(userId, "PRODUCT_CREATE", "products", created.getId().toString(), "Created product: " + created.getName());
        return created;
    }

    public Product updateProduct(UUID id, Product product, UUID userId) {
        product.setId(id);
        productDao.update(product);
        auditLogDao.log(userId, "PRODUCT_UPDATE", "products", id.toString(), "Updated product: " + product.getName());
        return getProductById(id);
    }

    public void setProductActive(UUID id, boolean isActive, UUID userId) {
        productDao.setArchive(id, isActive);
        auditLogDao.log(userId, isActive ? "PRODUCT_RESTORE" : "PRODUCT_ARCHIVE", "products", id.toString(), "Changed product active status to: " + isActive);
    }

    public ProductVariant addVariant(UUID productId, ProductVariant variant, UUID userId) {
        variant.setProductId(productId);
        if (variant.getExtraPrice() == null) {
            variant.setExtraPrice(BigDecimal.ZERO);
        }
        ProductVariant created = productDao.createVariant(variant);
        auditLogDao.log(userId, "VARIANT_CREATE", "product_variants", created.getId().toString(), "Added variant: " + created.getVariantName() + " (+$" + created.getExtraPrice() + ")");
        return created;
    }

    public ProductVariant updateVariant(UUID variantId, ProductVariant variant, UUID userId) {
        variant.setId(variantId);
        productDao.updateVariant(variant);
        auditLogDao.log(userId, "VARIANT_UPDATE", "product_variants", variantId.toString(), "Updated variant: " + variant.getVariantName());
        return productDao.findVariantById(variantId).orElse(variant);
    }

    public void deleteVariant(UUID variantId, UUID userId) {
        productDao.deleteVariant(variantId);
        auditLogDao.log(userId, "VARIANT_DELETE", "product_variants", variantId.toString(), "Deleted variant");
    }

    public BigDecimal calculateEffectivePrice(UUID productId, UUID variantId) {
        Product product = getProductById(productId);
        BigDecimal price = product.getSalesPrice();
        if (variantId != null) {
            ProductVariant variant = productDao.findVariantById(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + variantId));
            price = price.add(variant.getExtraPrice());
        }
        return price;
    }
}
