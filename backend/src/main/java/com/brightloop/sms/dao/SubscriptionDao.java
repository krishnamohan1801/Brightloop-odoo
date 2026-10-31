package com.brightloop.sms.dao;

import com.brightloop.sms.model.Subscription;
import com.brightloop.sms.model.SubscriptionItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionDao {

    private final JdbcTemplate jdbcTemplate;

    public SubscriptionDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Subscription> subscriptionRowMapper = (ResultSet rs, int rowNum) -> {
        Subscription s = new Subscription();
        s.setId(rs.getObject("id", UUID.class));
        s.setSubscriptionNumber(rs.getString("subscription_number"));
        s.setCustomerId(rs.getObject("customer_id", UUID.class));
        s.setPlanId(rs.getObject("plan_id", UUID.class));
        s.setDiscountId(rs.getObject("discount_id", UUID.class));
        s.setStatus(rs.getString("status"));
        s.setPaymentTerms(rs.getString("payment_terms"));
        s.setStartDate(rs.getObject("start_date", LocalDate.class));
        s.setNextBillingDate(rs.getObject("next_billing_date", LocalDate.class));
        s.setExpiryDate(rs.getObject("expiry_date", LocalDate.class));
        s.setNotes(rs.getString("notes"));
        s.setCreatedBy(rs.getObject("created_by", UUID.class));
        s.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        s.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

        // Enriched columns if joined
        try { s.setCustomerName(rs.getString("customer_name")); } catch (Exception ignored) {}
        try { s.setCustomerEmail(rs.getString("customer_email")); } catch (Exception ignored) {}
        try { s.setPlanName(rs.getString("plan_name")); } catch (Exception ignored) {}
        try { s.setBillingInterval(rs.getString("billing_interval")); } catch (Exception ignored) {}
        try { s.setDiscountCode(rs.getString("discount_code")); } catch (Exception ignored) {}

        return s;
    };

    private final RowMapper<SubscriptionItem> itemRowMapper = (ResultSet rs, int rowNum) -> {
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

        try { item.setProductName(rs.getString("product_name")); } catch (Exception ignored) {}
        try { item.setVariantName(rs.getString("variant_name")); } catch (Exception ignored) {}
        try { item.setTaxName(rs.getString("tax_name")); } catch (Exception ignored) {}
        try { item.setTaxRate(rs.getBigDecimal("tax_rate")); } catch (Exception ignored) {}

        return item;
    };

    private static final String BASE_SELECT =
        "SELECT s.*, u.full_name AS customer_name, u.email AS customer_email, " +
        "p.name AS plan_name, p.billing_interval, d.code AS discount_code " +
        "FROM subscriptions s " +
        "JOIN users u ON s.customer_id = u.id " +
        "JOIN plans p ON s.plan_id = p.id " +
        "LEFT JOIN discounts d ON s.discount_id = d.id ";

    public List<Subscription> findAll(String status, UUID customerId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND s.status = ?::subscription_status ");
            params.add(status);
        }
        if (customerId != null) {
            sql.append(" AND s.customer_id = ? ");
            params.add(customerId);
        }
        sql.append(" ORDER BY s.created_at DESC");

        List<Subscription> list = jdbcTemplate.query(sql.toString(), subscriptionRowMapper, params.toArray());
        for (Subscription s : list) {
            s.setItems(findItemsBySubscriptionId(s.getId()));
        }
        return list;
    }

    public Optional<Subscription> findById(UUID id) {
        String sql = BASE_SELECT + " WHERE s.id = ?";
        List<Subscription> list = jdbcTemplate.query(sql, subscriptionRowMapper, id);
        if (list.isEmpty()) return Optional.empty();
        Subscription s = list.get(0);
        s.setItems(findItemsBySubscriptionId(s.getId()));
        return Optional.of(s);
    }

    public List<Subscription> findDueForBilling(LocalDate asOfDate) {
        String sql = BASE_SELECT + " WHERE s.status = 'active' AND s.next_billing_date <= ? ORDER BY s.next_billing_date ASC";
        List<Subscription> list = jdbcTemplate.query(sql, subscriptionRowMapper, asOfDate);
        for (Subscription s : list) {
            s.setItems(findItemsBySubscriptionId(s.getId()));
        }
        return list;
    }

    public List<SubscriptionItem> findItemsBySubscriptionId(UUID subscriptionId) {
        String sql = "SELECT si.*, pr.name AS product_name, pv.variant_name, t.name AS tax_name, t.rate AS tax_rate " +
                     "FROM subscription_items si " +
                     "JOIN products pr ON si.product_id = pr.id " +
                     "LEFT JOIN product_variants pv ON si.variant_id = pv.id " +
                     "LEFT JOIN taxes t ON si.tax_id = t.id " +
                     "WHERE si.subscription_id = ? ORDER BY si.created_at ASC";
        return jdbcTemplate.query(sql, itemRowMapper, subscriptionId);
    }

    @Transactional
    public Subscription create(Subscription sub, List<SubscriptionItem> items) {
        UUID id = sub.getId() != null ? sub.getId() : UUID.randomUUID();
        sub.setId(id);

        String sql = "INSERT INTO subscriptions (id, subscription_number, customer_id, plan_id, discount_id, " +
                     "status, payment_terms, start_date, next_billing_date, expiry_date, notes, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?::subscription_status, ?, ?, ?, ?, ?, ?) RETURNING *";

        jdbcTemplate.queryForObject(
            sql,
            subscriptionRowMapper,
            id,
            sub.getSubscriptionNumber(),
            sub.getCustomerId(),
            sub.getPlanId(),
            sub.getDiscountId(),
            sub.getStatus() != null ? sub.getStatus() : "draft",
            sub.getPaymentTerms(),
            sub.getStartDate(),
            sub.getNextBillingDate(),
            sub.getExpiryDate(),
            sub.getNotes(),
            sub.getCreatedBy()
        );

        if (items != null) {
            for (SubscriptionItem item : items) {
                UUID itemId = UUID.randomUUID();
                item.setId(itemId);
                item.setSubscriptionId(id);
                String itemSql = "INSERT INTO subscription_items (id, subscription_id, product_id, variant_id, quantity, unit_price, tax_id, subtotal) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(
                    itemSql,
                    itemId,
                    id,
                    item.getProductId(),
                    item.getVariantId(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTaxId(),
                    item.getSubtotal()
                );
            }
        }

        return findById(id).orElse(sub);
    }

    public void updateStatus(UUID id, String status) {
        String sql = "UPDATE subscriptions SET status = ?::subscription_status WHERE id = ?";
        jdbcTemplate.update(sql, status, id);
    }

    public void updateNextBillingDate(UUID id, LocalDate nextBillingDate) {
        String sql = "UPDATE subscriptions SET next_billing_date = ? WHERE id = ?";
        jdbcTemplate.update(sql, nextBillingDate, id);
    }

    public void update(Subscription sub) {
        String sql = "UPDATE subscriptions SET plan_id = ?, discount_id = ?, status = ?::subscription_status, " +
                     "payment_terms = ?, start_date = ?, next_billing_date = ?, expiry_date = ?, notes = ? " +
                     "WHERE id = ?";
        jdbcTemplate.update(
            sql,
            sub.getPlanId(),
            sub.getDiscountId(),
            sub.getStatus(),
            sub.getPaymentTerms(),
            sub.getStartDate(),
            sub.getNextBillingDate(),
            sub.getExpiryDate(),
            sub.getNotes(),
            sub.getId()
        );
    }
}
