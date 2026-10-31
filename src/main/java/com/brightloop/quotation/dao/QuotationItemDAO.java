package com.brightloop.quotation.dao;

import com.brightloop.quotation.model.QuotationItem;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class QuotationItemDAO {

    private final JdbcTemplate jdbcTemplate;

    public QuotationItemDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<QuotationItem> quotationItemRowMapper = (rs, rowNum) -> {
        QuotationItem item = new QuotationItem();

        item.setId(rs.getObject("id", UUID.class));
        item.setQuotationId(rs.getObject("quotation_id", UUID.class));
        item.setProductId(rs.getObject("product_id", UUID.class));
        item.setVariantId(rs.getObject("variant_id", UUID.class));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setTaxId(rs.getObject("tax_id", UUID.class));
        item.setTaxRate(rs.getBigDecimal("tax_rate"));
        item.setTaxAmount(rs.getBigDecimal("tax_amount"));
        item.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        item.setSubtotal(rs.getBigDecimal("subtotal"));
        item.setTotalAmount(rs.getBigDecimal("total_amount"));
        item.setDescription(rs.getString("description"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            item.setCreatedAt(createdAt.toInstant().atOffset(ZoneOffset.UTC));
        }

        return item;
    };

    public QuotationItem save(QuotationItem item) {
        String sql = """
                INSERT INTO quotation_items (
                    quotation_id,
                    product_id,
                    variant_id,
                    quantity,
                    unit_price,
                    tax_id,
                    tax_rate,
                    tax_amount,
                    discount_amount,
                    subtotal,
                    total_amount,
                    description
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        UUID id = jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                item.getQuotationId(),
                item.getProductId(),
                item.getVariantId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTaxId(),
                item.getTaxRate(),
                item.getTaxAmount(),
                item.getDiscountAmount(),
                item.getSubtotal(),
                item.getTotalAmount(),
                item.getDescription()
        );

        item.setId(id);

        return findById(id)
                .orElseThrow(() -> new IllegalStateException("Quotation item could not be retrieved after saving."));
    }

    public List<QuotationItem> saveAll(List<QuotationItem> items) {
        return items.stream().map(this::save).toList();
    }

    public List<QuotationItem> findByQuotationId(UUID quotationId) {
        String sql = """
                SELECT *
                FROM quotation_items
                WHERE quotation_id = ?
                ORDER BY created_at ASC
                """;

        return jdbcTemplate.query(sql, quotationItemRowMapper, quotationId);
    }

    public Optional<QuotationItem> findById(UUID id) {
        String sql = """
                SELECT *
                FROM quotation_items
                WHERE id = ?
                """;

        List<QuotationItem> results = jdbcTemplate.query(sql, quotationItemRowMapper, id);
        return results.stream().findFirst();
    }

    public void update(QuotationItem item) {
        String sql = """
                UPDATE quotation_items
                SET product_id = ?,
                    variant_id = ?,
                    quantity = ?,
                    unit_price = ?,
                    tax_id = ?,
                    tax_rate = ?,
                    tax_amount = ?,
                    discount_amount = ?,
                    subtotal = ?,
                    total_amount = ?,
                    description = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(
                sql,
                item.getProductId(),
                item.getVariantId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTaxId(),
                item.getTaxRate(),
                item.getTaxAmount(),
                item.getDiscountAmount(),
                item.getSubtotal(),
                item.getTotalAmount(),
                item.getDescription(),
                item.getId()
        );
    }

    public void deleteById(UUID id) {
        String sql = """
                DELETE FROM quotation_items
                WHERE id = ?
                """;

        jdbcTemplate.update(sql, id);
    }

    public void deleteByQuotationId(UUID quotationId) {
        String sql = """
                DELETE FROM quotation_items
                WHERE quotation_id = ?
                """;

        jdbcTemplate.update(sql, quotationId);
    }
}
