package com.brightloop.sms.dao;

import com.brightloop.sms.model.Refund;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefundDao {

    private final JdbcTemplate jdbcTemplate;

    public RefundDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Refund> refundRowMapper = (ResultSet rs, int rowNum) -> {
        Refund r = new Refund();
        r.setId(rs.getObject("id", UUID.class));
        r.setPaymentId(rs.getObject("payment_id", UUID.class));
        r.setAmount(rs.getBigDecimal("amount"));
        r.setReason(rs.getString("reason"));
        r.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));

        try { r.setReferenceNumber(rs.getString("reference_number")); } catch (Exception ignored) {}
        try { r.setInvoiceNumber(rs.getString("invoice_number")); } catch (Exception ignored) {}

        return r;
    };

    private static final String BASE_SELECT =
        "SELECT r.*, p.reference_number, inv.invoice_number " +
        "FROM refunds r " +
        "JOIN payments p ON r.payment_id = p.id " +
        "JOIN invoices inv ON p.invoice_id = inv.id ";

    public List<Refund> findAll() {
        String sql = BASE_SELECT + " ORDER BY r.created_at DESC";
        return jdbcTemplate.query(sql, refundRowMapper);
    }

    public List<Refund> findByPaymentId(UUID paymentId) {
        String sql = BASE_SELECT + " WHERE r.payment_id = ? ORDER BY r.created_at DESC";
        return jdbcTemplate.query(sql, refundRowMapper, paymentId);
    }

    public Optional<Refund> findById(UUID id) {
        String sql = BASE_SELECT + " WHERE r.id = ?";
        List<Refund> list = jdbcTemplate.query(sql, refundRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Refund create(Refund refund) {
        UUID id = refund.getId() != null ? refund.getId() : UUID.randomUUID();
        String sql = "INSERT INTO refunds (id, payment_id, amount, reason) VALUES (?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> {
                Refund r = new Refund();
                r.setId(rs.getObject("id", UUID.class));
                r.setPaymentId(rs.getObject("payment_id", UUID.class));
                r.setAmount(rs.getBigDecimal("amount"));
                r.setReason(rs.getString("reason"));
                r.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                return r;
            },
            id,
            refund.getPaymentId(),
            refund.getAmount(),
            refund.getReason()
        );
    }
}
