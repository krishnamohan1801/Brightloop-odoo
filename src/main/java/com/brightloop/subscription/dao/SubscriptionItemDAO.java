package com.brightloop.subscription.dao;

import com.brightloop.subscription.model.SubscriptionItem;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class SubscriptionItemDAO {

    private final JdbcTemplate jdbcTemplate;

    public SubscriptionItemDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SubscriptionItem> rowMapper = (rs, rowNum) -> {
        SubscriptionItem item = new SubscriptionItem();

        item.setId(rs.getObject("id", UUID.class));
        item.setSubscriptionId(rs.getObject("subscription_id", UUID.class));
        item.setProductId(rs.getObject("product_id", UUID.class));
        item.setVariantId(rs.getObject("variant_id", UUID.class));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setTaxId(rs.getObject("tax_id", UUID.class));
        item.setSubtotal(rs.getBigDecimal("subtotal"));
        item.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));

        return item;
    };

    public UUID save(SubscriptionItem item) {
        String sql = """
                INSERT INTO subscription_items (
                    subscription_id,
                    product_id,
                    variant_id,
                    quantity,
                    unit_price,
                    tax_id,
                    subtotal
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                item.getSubscriptionId(),
                item.getProductId(),
                item.getVariantId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTaxId(),
                item.getSubtotal()
        );
    }

    public List<SubscriptionItem> findBySubscriptionId(UUID subscriptionId) {
        String sql = """
                SELECT *
                FROM subscription_items
                WHERE subscription_id = ?
                ORDER BY created_at ASC
                """;

        return jdbcTemplate.query(sql, rowMapper, subscriptionId);
    }

    public SubscriptionItem findById(UUID id) {
        String sql = """
                SELECT *
                FROM subscription_items
                WHERE id = ?
                """;

        List<SubscriptionItem> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public int deleteBySubscriptionId(UUID subscriptionId) {
        String sql = """
                DELETE FROM subscription_items
                WHERE subscription_id = ?
                """;

        return jdbcTemplate.update(sql, subscriptionId);
    }
}
