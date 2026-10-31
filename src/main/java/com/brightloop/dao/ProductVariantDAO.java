package com.brightloop.dao;

import com.brightloop.model.ProductVariant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ProductVariantDAO {

    private final JdbcTemplate jdbcTemplate;

    public ProductVariantDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ProductVariant> rowMapper = (rs, rowNum) -> {
        ProductVariant variant = new ProductVariant();

        variant.setId(rs.getObject("id", UUID.class));
        variant.setProductId(rs.getObject("product_id", UUID.class));
        variant.setVariantName(rs.getString("variant_name"));
        variant.setExtraPrice(rs.getBigDecimal("extra_price"));
        variant.setSku(rs.getString("sku"));
        variant.setCreatedAt(
                rs.getObject("created_at", java.time.OffsetDateTime.class)
        );

        return variant;
    };

    public UUID create(UUID productId, ProductVariant variant) {

        String sql = """
                INSERT INTO product_variants
                (product_id, variant_name, extra_price, sku)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                productId,
                variant.getVariantName(),
                variant.getExtraPrice(),
                variant.getSku()
        );
    }

    public List<ProductVariant> findAllByProductId(UUID productId) {

        String sql = """
                SELECT *
                FROM product_variants
                WHERE product_id = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, rowMapper, productId);
    }

    public ProductVariant findById(UUID variantId, UUID productId) {

        String sql = """
                SELECT *
                FROM product_variants
                WHERE id = ? AND product_id = ?
                """;

        List<ProductVariant> results =
                jdbcTemplate.query(sql, rowMapper, variantId, productId);

        return results.isEmpty() ? null : results.get(0);
    }

    public boolean update(UUID variantId, UUID productId,
                          ProductVariant variant) {

        String sql = """
                UPDATE product_variants
                SET variant_name = ?, extra_price = ?, sku = ?
                WHERE id = ? AND product_id = ?
                """;

        return jdbcTemplate.update(
                sql,
                variant.getVariantName(),
                variant.getExtraPrice(),
                variant.getSku(),
                variantId,
                productId
        ) > 0;
    }

    public boolean delete(UUID variantId, UUID productId) {

        String sql = """
                DELETE FROM product_variants
                WHERE id = ? AND product_id = ?
                """;

        return jdbcTemplate.update(sql, variantId, productId) > 0;
    }
}