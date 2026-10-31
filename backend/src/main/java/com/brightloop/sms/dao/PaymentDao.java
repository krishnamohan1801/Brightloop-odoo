package com.brightloop.sms.dao;

import com.brightloop.sms.model.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentDao {

    private final JdbcTemplate jdbcTemplate;

    public PaymentDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Payment> paymentRowMapper = (ResultSet rs, int rowNum) -> {
        Payment p = new Payment();
        p.setId(rs.getObject("id", UUID.class));
        p.setInvoiceId(rs.getObject("invoice_id", UUID.class));
        p.setCustomerId(rs.getObject("customer_id", UUID.class));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setPaymentDate(rs.getObject("payment_date", LocalDate.class));
        p.setReferenceNumber(rs.getString("reference_number"));
        p.setNotes(rs.getString("notes"));
        p.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));

        try { p.setCustomerName(rs.getString("customer_name")); } catch (Exception ignored) {}
        try { p.setInvoiceNumber(rs.getString("invoice_number")); } catch (Exception ignored) {}

        return p;
    };

    private static final String BASE_SELECT =
        "SELECT p.*, u.full_name AS customer_name, inv.invoice_number " +
        "FROM payments p " +
        "JOIN users u ON p.customer_id = u.id " +
        "JOIN invoices inv ON p.invoice_id = inv.id ";

    public List<Payment> findAll(UUID customerId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> params = new ArrayList<>();
        if (customerId != null) {
            sql.append(" WHERE p.customer_id = ? ");
            params.add(customerId);
        }
        sql.append(" ORDER BY p.payment_date DESC, p.created_at DESC");

        List<Payment> list = jdbcTemplate.query(sql.toString(), paymentRowMapper, params.toArray());
        for (Payment p : list) {
            p.setRefundedAmount(getTotalRefundedForPayment(p.getId()));
        }
        return list;
    }

    public List<Payment> findByInvoiceId(UUID invoiceId) {
        String sql = BASE_SELECT + " WHERE p.invoice_id = ? ORDER BY p.created_at ASC";
        List<Payment> list = jdbcTemplate.query(sql, paymentRowMapper, invoiceId);
        for (Payment p : list) {
            p.setRefundedAmount(getTotalRefundedForPayment(p.getId()));
        }
        return list;
    }

    public Optional<Payment> findById(UUID id) {
        String sql = BASE_SELECT + " WHERE p.id = ?";
        List<Payment> list = jdbcTemplate.query(sql, paymentRowMapper, id);
        if (list.isEmpty()) return Optional.empty();
        Payment p = list.get(0);
        p.setRefundedAmount(getTotalRefundedForPayment(p.getId()));
        return Optional.of(p);
    }

    public Payment create(Payment payment) {
        UUID id = payment.getId() != null ? payment.getId() : UUID.randomUUID();
        String sql = "INSERT INTO payments (id, invoice_id, customer_id, amount, payment_method, payment_date, reference_number, notes) " +
                     "VALUES (?, ?, ?, ?, ?::payment_method, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
            sql,
            paymentRowMapper,
            id,
            payment.getInvoiceId(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getPaymentMethod(),
            payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDate.now(),
            payment.getReferenceNumber(),
            payment.getNotes()
        );
    }

    public BigDecimal getTotalRefundedForPayment(UUID paymentId) {
        String sql = "SELECT COALESCE(SUM(amount), 0.00) FROM refunds WHERE payment_id = ?";
        BigDecimal refunded = jdbcTemplate.queryForObject(sql, BigDecimal.class, paymentId);
        return refunded != null ? refunded : BigDecimal.ZERO;
    }
}
