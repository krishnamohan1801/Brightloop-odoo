package com.brightloop.dao;

import com.brightloop.model.Product;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductDAO {

    private final JdbcTemplate jdbcTemplate;

    public ProductDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Convert database rows into Product objects
    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> {

        Product product = new Product();

        product.setProductId(rs.getObject("id", UUID.class));
        product.setName(rs.getString("name"));
        product.setProductType(rs.getString("product_type"));
        product.setSalesPrice(rs.getBigDecimal("sales_price"));
        product.setCostPrice(rs.getBigDecimal("cost_price"));
        product.setNotes(rs.getString("notes"));

        // Active in DB means not archived
        product.setArchived(!rs.getBoolean("is_active"));

        product.setCreatedAt(
            rs.getObject("created_at",
                java.time.OffsetDateTime.class)
        );

        product.setUpdatedAt(
            rs.getObject("updated_at",
                java.time.OffsetDateTime.class)
        );

        return product;
    };

    // CREATE PRODUCT
    public UUID create(Product product) {

        String sql = """
            INSERT INTO products
            (name, product_type, sales_price, cost_price, notes)
            VALUES (?, CAST(? AS product_type), ?, ?, ?)
            RETURNING id
            """;

        return jdbcTemplate.queryForObject(
            sql,
            UUID.class,
            product.getName(),
            product.getProductType(),
            product.getSalesPrice(),
            product.getCostPrice(),
            product.getNotes()
        );
    }

    // GET ALL ACTIVE PRODUCTS
    public List<Product> findAll() {

        String sql = """
            SELECT * FROM products
            WHERE is_active = TRUE
            ORDER BY created_at DESC
            """;

        return jdbcTemplate.query(sql, productRowMapper);
    }

    // GET PRODUCT BY ID
    public Optional<Product> findById(UUID productId) {

        String sql = """
            SELECT * FROM products
            WHERE id = ? AND is_active = TRUE
            """;

        List<Product> products = jdbcTemplate.query(
            sql,
            productRowMapper,
            productId
        );

        return products.stream().findFirst();
    }

    // UPDATE PRODUCT
    public int update(UUID productId, Product product) {

        String sql = """
            UPDATE products
            SET name = ?,
                product_type = CAST(? AS product_type),
                sales_price = ?,
                cost_price = ?,
                notes = ?
            WHERE id = ? AND is_active = TRUE
            """;

        return jdbcTemplate.update(
            sql,
            product.getName(),
            product.getProductType(),
            product.getSalesPrice(),
            product.getCostPrice(),
            product.getNotes(),
            productId
        );
    }

    // ARCHIVE PRODUCT
    public int archive(UUID productId) {

        String sql = """
            UPDATE products
            SET is_active = FALSE
            WHERE id = ? AND is_active = TRUE
            """;

        return jdbcTemplate.update(sql, productId);
    }
}