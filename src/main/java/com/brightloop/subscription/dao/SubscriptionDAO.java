package com.brightloop.subscription.dao;

import com.brightloop.subscription.model.Subscription;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionDAO {

    private final JdbcTemplate jdbcTemplate;

    public SubscriptionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Subscription> subscriptionRowMapper = (rs, rowNum) -> {
        Subscription subscription = new Subscription();

        subscription.setId(rs.getObject("id", UUID.class));
        subscription.setSubscriptionNumber(rs.getString("subscription_number"));
        subscription.setCustomerId(rs.getObject("customer_id", UUID.class));
        subscription.setPlanId(rs.getObject("plan_id", UUID.class));
        subscription.setDiscountId(rs.getObject("discount_id", UUID.class));
        subscription.setStatus(rs.getString("status"));
        subscription.setPaymentTerms(rs.getString("payment_terms"));
        subscription.setStartDate(rs.getObject("start_date", LocalDate.class));
        subscription.setNextBillingDate(rs.getObject("next_billing_date", LocalDate.class));
        subscription.setExpiryDate(rs.getObject("expiry_date", LocalDate.class));
        subscription.setNotes(rs.getString("notes"));
        subscription.setCreatedBy(rs.getObject("created_by", UUID.class));
        subscription.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        subscription.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

        return subscription;
    };

    public UUID save(Subscription subscription) {
        String sql = """
                INSERT INTO subscriptions (
                    subscription_number,
                    customer_id,
                    plan_id,
                    discount_id,
                    status,
                    payment_terms,
                    start_date,
                    next_billing_date,
                    expiry_date,
                    notes,
                    created_by
                )
                VALUES (?, ?, ?, ?, ?::subscription_status, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        String status = subscription.getStatus() == null ? "draft" : subscription.getStatus().toLowerCase();

        return jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                subscription.getSubscriptionNumber(),
                subscription.getCustomerId(),
                subscription.getPlanId(),
                subscription.getDiscountId(),
                status,
                subscription.getPaymentTerms(),
                subscription.getStartDate(),
                subscription.getNextBillingDate(),
                subscription.getExpiryDate(),
                subscription.getNotes(),
                subscription.getCreatedBy()
        );
    }

    public List<Subscription> findAll() {
        String sql = """
                SELECT *
                FROM subscriptions
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, subscriptionRowMapper);
    }

    public Optional<Subscription> findById(UUID id) {
        String sql = """
                SELECT *
                FROM subscriptions
                WHERE id = ?
                """;

        List<Subscription> results = jdbcTemplate.query(sql, subscriptionRowMapper, id);
        return results.stream().findFirst();
    }

    public List<Subscription> findByCustomerId(UUID customerId) {
        String sql = """
                SELECT *
                FROM subscriptions
                WHERE customer_id = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, subscriptionRowMapper, customerId);
    }

    public int updateStatus(UUID id, String status) {
        String sql = """
                UPDATE subscriptions
                SET status = ?::subscription_status,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        return jdbcTemplate.update(sql, status.toLowerCase(), id);
    }

    public int updateBillingDates(UUID id, LocalDate nextBillingDate, LocalDate expiryDate) {
        String sql = """
                UPDATE subscriptions
                SET next_billing_date = ?,
                    expiry_date = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        return jdbcTemplate.update(sql, nextBillingDate, expiryDate, id);
    }
}
