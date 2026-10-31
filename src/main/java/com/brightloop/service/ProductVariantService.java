package com.brightloop.service;

import com.brightloop.dao.ProductDAO;
import com.brightloop.dao.ProductVariantDAO;
import com.brightloop.dto.ProductVariantRequest;
import com.brightloop.model.ProductVariant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductVariantService {

    private final ProductVariantDAO variantDAO;
    private final ProductDAO productDAO;

    public ProductVariantService(
            ProductVariantDAO variantDAO,
            ProductDAO productDAO) {
        this.variantDAO = variantDAO;
        this.productDAO = productDAO;
    }

    public UUID create(UUID productId, ProductVariantRequest request) {

        if (productDAO.findById(productId) == null) {
            throw new RuntimeException("Product not found");
        }

        ProductVariant variant = new ProductVariant();
        variant.setVariantName(request.getVariantName());
        variant.setExtraPrice(request.getExtraPrice());
        variant.setSku(request.getSku());

        return variantDAO.create(productId, variant);
    }

    public List<ProductVariant> getAll(UUID productId) {

        if (productDAO.findById(productId) == null) {
            throw new RuntimeException("Product not found");
        }

        return variantDAO.findAllByProductId(productId);
    }

    public ProductVariant getById(UUID productId, UUID variantId) {

        ProductVariant variant =
                variantDAO.findById(variantId, productId);

        if (variant == null) {
            throw new RuntimeException("Product variant not found");
        }

        return variant;
    }

    public void update(UUID productId, UUID variantId,
                       ProductVariantRequest request) {

        ProductVariant variant = getById(productId, variantId);

        variant.setVariantName(request.getVariantName());
        variant.setExtraPrice(request.getExtraPrice());
        variant.setSku(request.getSku());

        variantDAO.update(variantId, productId, variant);
    }

    public void delete(UUID productId, UUID variantId) {

        if (!variantDAO.delete(variantId, productId)) {
            throw new RuntimeException("Product variant not found");
        }
    }
}