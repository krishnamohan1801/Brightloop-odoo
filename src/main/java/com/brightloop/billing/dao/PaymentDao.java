package com.brightloop.billing.dao;

import com.brightloop.billing.dto.ApiModels.PaymentView;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentDao {
    private final JdbcTemplate jdbc;

    public PaymentDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BigDecimal calculateNetCollectedAmount(UUID invoiceId) {
        BigDecimal result = jdbc.queryForObject(
                "SELECT COALESCE(SUM(CASE " +
                "WHEN COALESCE(reference_number, '') LIKE 'SIM-REFUND-%' THEN -amount " +
                "WHEN COALESCE(reference_number, '') LIKE 'SIM-FAILED-%' " +
                "OR COALESCE(reference_number, '') LIKE 'SIM-PENDING-%' THEN 0 " +
                "ELSE amount END), 0) FROM payments WHERE invoice_id = ?",
                BigDecimal.class, invoiceId);
        return result == null ? BigDecimal.ZERO : result;
    }

    public BigDecimal sumRefundsForOriginalPayment(UUID originalPaymentId) {
        BigDecimal result = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payments " +
                "WHERE reference_number LIKE 'SIM-REFUND-%' AND notes LIKE ?",
                BigDecimal.class, "SIMULATED_REFUND; REFUND_OF=" + originalPaymentId + ";%");
        return result == null ? BigDecimal.ZERO : result;
    }

    public UUID insertPayment(UUID invoiceId, UUID customerId, BigDecimal amount,
                              String method, String reference, String notes) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO payments (id, invoice_id, customer_id, amount, payment_method, payment_date, reference_number, notes) " +
                "VALUES (?, ?, ?, ?, CAST(? AS payment_method), ?, ?, ?)",
                id, invoiceId, customerId, amount, method, Date.valueOf(LocalDate.now()), reference, notes);
        return id;
    }

    public List<PaymentView> findPayments(UUID invoiceId) {
        return jdbc.query(
                "SELECT id, invoice_id, customer_id, amount, payment_method::text AS payment_method, " +
                "payment_date, reference_number, notes FROM payments WHERE invoice_id = ? ORDER BY created_at DESC",
                (rs, rowNum) -> mapPayment(
                        rs.getObject("id", UUID.class), rs.getObject("invoice_id", UUID.class),
                        rs.getObject("customer_id", UUID.class), rs.getBigDecimal("amount"),
                        rs.getString("payment_method"), rs.getDate("payment_date").toLocalDate(),
                        rs.getString("reference_number"), rs.getString("notes")), invoiceId);
    }

    public Optional<PaymentView> findPayment(UUID paymentId) {
        List<PaymentView> rows = jdbc.query(
                "SELECT id, invoice_id, customer_id, amount, payment_method::text AS payment_method, " +
                "payment_date, reference_number, notes FROM payments WHERE id = ?",
                (rs, rowNum) -> mapPayment(
                        rs.getObject("id", UUID.class), rs.getObject("invoice_id", UUID.class),
                        rs.getObject("customer_id", UUID.class), rs.getBigDecimal("amount"),
                        rs.getString("payment_method"), rs.getDate("payment_date").toLocalDate(),
                        rs.getString("reference_number"), rs.getString("notes")), paymentId);
        return rows.stream().findFirst();
    }

    public int resolvePending(UUID paymentId, String reference, String notes) {
        return jdbc.update(
                "UPDATE payments SET reference_number = ?, notes = ? " +
                "WHERE id = ? AND reference_number LIKE 'SIM-PENDING-%'",
                reference, notes, paymentId);
    }

    private PaymentView mapPayment(UUID id, UUID invoiceId, UUID customerId, BigDecimal amount,
                                   String method, LocalDate date, String reference, String notes) {
        String outcome = "LEGACY_OR_SUCCESS";
        if (reference != null && reference.startsWith("SIM-FAILED-")) outcome = "FAILED";
        else if (reference != null && reference.startsWith("SIM-PENDING-")) outcome = "PENDING";
        else if (reference != null && reference.startsWith("SIM-REFUND-")) outcome = "REFUND";
        else if (reference != null && reference.startsWith("SIM-SUCCESS-")) outcome = "SUCCESS";
        return new PaymentView(id, invoiceId, customerId, amount, method, date, reference, outcome, notes);
    }
}
