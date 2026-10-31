package com.brightloop.sms.dao;

import com.brightloop.sms.model.Product;
import com.brightloop.sms.model.ProductVariant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductDao {

    private final JdbcTemplate jdbcTemplate;

    public ProductDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Product> productRowMapper = (ResultSet rs, int rowNum) -> {
        Product p = new Product();
        p.setId(rs.getObject("id", UUID.class));
        p.setName(rs.getString("name"));
        p.setProductType(rs.getString("product_type"));
        p.setSalesPrice(rs.getBigDecimal("sales_price"));
        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setNotes(rs.getString("notes"));
        p.setIsActive(rs.getBoolean("is_active"));
        p.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        p.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return p;
    };

    private final RowMapper<ProductVariant> variantRowMapper = (ResultSet rs, int rowNum) -> {
        ProductVariant v = new ProductVariant();
        v.setId(rs.getObject("id", UUID.class));
        v.setProductId(rs.getObject("product_id", UUID.class));
        v.setVariantName(rs.getString("variant_name"));
        v.setExtraPrice(rs.getBigDecimal("extra_price"));
        v.setSku(rs.getString("sku"));
        v.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return v;
    };

    public List<Product> findAll(Boolean activeOnly) {
        String sql = activeOnly != null && activeOnly
            ? "SELECT * FROM products WHERE is_active = true ORDER BY name ASC"
            : "SELECT * FROM products ORDER BY created_at DESC";
        List<Product> products = jdbcTemplate.query(sql, productRowMapper);
        for (Product p : products) {
            p.setVariants(findVariantsByProductId(p.getId()));
        }
        return products;
    }

    public Optional<Product> findById(UUID id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        List<Product> list = jdbcTemplate.query(sql, productRowMapper, id);
        if (list.isEmpty()) return Optional.empty();
        Product p = list.get(0);
        p.setVariants(findVariantsByProductId(p.getId()));
        return Optional.of(p);
    }

    public Product create(Product product) {
        UUID id = product.getId() != null ? product.getId() : UUID.randomUUID();
        String sql = "INSERT INTO products (id, name, product_type, sales_price, cost_price, notes, is_active) " +
                     "VALUES (?, ?, ?::product_type, ?, ?, ?, ?) RETURNING *";
        Product created = jdbcTemplate.queryForObject(
            sql,
            productRowMapper,
            id,
            product.getName(),
            product.getProductType(),
            product.getSalesPrice(),
            product.getCostPrice(),
            product.getNotes(),
            product.getIsActive() != null ? product.getIsActive() : true
        );
        return created;
    }

    public void update(Product product) {
        String sql = "UPDATE products SET name = ?, product_type = ?::product_type, sales_price = ?, " +
                     "cost_price = ?, notes = ?, is_active = ? WHERE id = ?";
        jdbcTemplate.update(
            sql,
            product.getName(),
            product.getProductType(),
            product.getSalesPrice(),
            product.getCostPrice(),
            product.getNotes(),
            product.getIsActive(),
            product.getId()
        );
    }

    public void setArchive(UUID id, boolean isActive) {
        String sql = "UPDATE products SET is_active = ? WHERE id = ?";
        jdbcTemplate.update(sql, isActive, id);
    }

    public List<ProductVariant> findVariantsByProductId(UUID productId) {
        String sql = "SELECT * FROM product_variants WHERE product_id = ? ORDER BY variant_name ASC";
        return jdbcTemplate.query(sql, variantRowMapper, productId);
    }

    public Optional<ProductVariant> findVariantById(UUID variantId) {
        String sql = "SELECT * FROM product_variants WHERE id = ?";
        List<ProductVariant> list = jdbcTemplate.query(sql, variantRowMapper, variantId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public ProductVariant createVariant(ProductVariant variant) {
        UUID id = variant.getId() != null ? variant.getId() : UUID.randomUUID();
        String sql = "INSERT INTO product_variants (id, product_id, variant_name, extra_price, sku) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
            sql,
            variantRowMapper,
            id,
            variant.getProductId(),
            variant.getVariantName(),
            variant.getExtraPrice(),
            variant.getSku()
        );
    }

    public void updateVariant(ProductVariant variant) {
        String sql = "UPDATE product_variants SET variant_name = ?, extra_price = ?, sku = ? WHERE id = ?";
        jdbcTemplate.update(sql, variant.getVariantName(), variant.getExtraPrice(), variant.getSku(), variant.getId());
    }

    public void deleteVariant(UUID variantId) {
        String sql = "DELETE FROM product_variants WHERE id = ?";
        jdbcTemplate.update(sql, variantId);
    }
}
