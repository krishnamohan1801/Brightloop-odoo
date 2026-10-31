package com.brightloop.service;

import com.brightloop.dao.ProductDAO;
import com.brightloop.dto.ProductRequest;
import com.brightloop.model.Product;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductDAO productDAO;

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    public UUID createProduct(ProductRequest request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setProductType(request.getProductType());
        product.setSalesPrice(request.getSalesPrice());
        product.setCostPrice(request.getCostPrice());
        product.setNotes(request.getNotes());

        return productDAO.create(product);
    }

    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    public Optional<Product> getProductById(UUID productId) {
        return productDAO.findById(productId);
    }

    public boolean updateProduct(
        UUID productId,
        ProductRequest request
    ) {

        Product product = new Product();

        product.setName(request.getName());
        product.setProductType(request.getProductType());
        product.setSalesPrice(request.getSalesPrice());
        product.setCostPrice(request.getCostPrice());
        product.setNotes(request.getNotes());

        return productDAO.update(productId, product) > 0;
    }

    public boolean archiveProduct(UUID productId) {
        return productDAO.archive(productId) > 0;
    }
}