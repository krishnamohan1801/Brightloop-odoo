package com.brightloop.billing.dao;

import com.brightloop.billing.exception.NotFoundException;
import com.brightloop.billing.model.BillingModels.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BillingDao {
    private final JdbcTemplate jdbc;

    public BillingDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<UUID> findDueSubscriptionIds(LocalDate today) {
        return jdbc.query(
                "SELECT s.id FROM subscriptions s " +
                "JOIN plans p ON p.id = s.plan_id " +
                "WHERE s.status = 'active' AND s.next_billing_date <= ? " +
                "AND p.is_active = TRUE " +
                "AND (s.expiry_date IS NULL OR s.next_billing_date <= s.expiry_date) " +
                "ORDER BY s.next_billing_date",
                (rs, rowNum) -> rs.getObject("id", UUID.class), Date.valueOf(today));
    }

    public Optional<SubscriptionInfo> findSubscription(UUID subscriptionId) {
        List<SubscriptionInfo> rows = jdbc.query(
                "SELECT s.id, s.customer_id, s.discount_id, s.status::text AS status, " +
                "s.payment_terms, s.next_billing_date, s.expiry_date, p.billing_interval::text AS billing_interval " +
                "FROM subscriptions s JOIN plans p ON p.id = s.plan_id WHERE s.id = ?",
                (rs, rowNum) -> new SubscriptionInfo(
                        rs.getObject("id", UUID.class),
                        rs.getObject("customer_id", UUID.class),
                        rs.getObject("discount_id", UUID.class),
                        rs.getString("status"),
                        rs.getString("payment_terms"),
                        rs.getDate("next_billing_date").toLocalDate(),
                        rs.getDate("expiry_date") == null ? null : rs.getDate("expiry_date").toLocalDate(),
                        rs.getString("billing_interval")),
                subscriptionId);
        return rows.stream().findFirst();
    }

    public List<SubscriptionItemInfo> findSubscriptionItems(UUID subscriptionId) {
        return jdbc.query(
                "SELECT si.product_id, si.variant_id, p.name AS product_name, pv.variant_name, " +
                "si.quantity, si.unit_price, COALESCE(CASE WHEN t.is_active THEN t.rate ELSE 0.00 END, 0.00) AS tax_rate " +
                "FROM subscription_items si " +
                "JOIN products p ON p.id = si.product_id " +
                "LEFT JOIN product_variants pv ON pv.id = si.variant_id " +
                "LEFT JOIN taxes t ON t.id = si.tax_id " +
                "WHERE si.subscription_id = ? ORDER BY si.created_at, si.id",
                (rs, rowNum) -> new SubscriptionItemInfo(
                        rs.getObject("product_id", UUID.class),
                        rs.getObject("variant_id", UUID.class),
                        rs.getString("product_name"),
                        rs.getString("variant_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getBigDecimal("tax_rate")),
                subscriptionId);
    }

    public Optional<DiscountInfo> findDiscount(UUID discountId) {
        if (discountId == null) {
            return Optional.empty();
        }
        List<DiscountInfo> rows = jdbc.query(
                "SELECT id, code, discount_type::text AS discount_type, value, min_purchase, min_quantity, " +
                "start_date, end_date, max_uses, used_count, target_scope::text AS target_scope, " +
                "target_product_id, is_active FROM discounts WHERE id = ?",
                (rs, rowNum) -> new DiscountInfo(
                        rs.getObject("id", UUID.class),
                        rs.getString("code"),
                        rs.getString("discount_type"),
                        rs.getBigDecimal("value"),
                        rs.getBigDecimal("min_purchase"),
                        rs.getInt("min_quantity"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        (Integer) rs.getObject("max_uses"),
                        rs.getInt("used_count"),
                        rs.getString("target_scope"),
                        rs.getObject("target_product_id", UUID.class),
                        rs.getBoolean("is_active")),
                discountId);
        return rows.stream().findFirst();
    }

    public boolean invoiceExists(UUID subscriptionId, LocalDate periodStart, LocalDate periodEnd) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM invoices WHERE subscription_id = ? " +
                "AND billing_period_start = ? AND billing_period_end = ?",
                Integer.class, subscriptionId, Date.valueOf(periodStart), Date.valueOf(periodEnd));
        return count != null && count > 0;
    }

    public UUID insertInvoice(UUID invoiceId, String invoiceNumber, UUID subscriptionId,
                              UUID customerId, LocalDate periodStart, LocalDate periodEnd,
                              LocalDate issueDate, LocalDate dueDate, BigDecimal subtotal,
                              BigDecimal taxAmount, BigDecimal discountAmount, BigDecimal totalAmount) {
        jdbc.update(
                "INSERT INTO invoices (id, invoice_number, subscription_id, customer_id, " +
                "billing_period_start, billing_period_end, issue_date, due_date, subtotal, " +
                "tax_amount, discount_amount, total_amount, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS invoice_status))",
                invoiceId, invoiceNumber, subscriptionId, customerId,
                Date.valueOf(periodStart), Date.valueOf(periodEnd), Date.valueOf(issueDate),
                Date.valueOf(dueDate), subtotal, taxAmount, discountAmount, totalAmount, "confirmed");
        return invoiceId;
    }

    public void insertInvoiceItem(UUID invoiceId, SubscriptionItemInfo item,
                                   BigDecimal taxAmount, BigDecimal lineSubtotal) {
        String description = item.productName();
        if (item.variantName() != null && !item.variantName().isBlank()) {
            description += " - " + item.variantName();
        }
        jdbc.update(
                "INSERT INTO invoice_items (id, invoice_id, product_id, variant_id, description, " +
                "quantity, unit_price, tax_rate, tax_amount, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), invoiceId, item.productId(), item.variantId(), description,
                item.quantity(), item.unitPrice(), item.taxRate(), taxAmount, lineSubtotal);
    }

    public void updateNextBillingDate(UUID subscriptionId, LocalDate nextDate) {
        jdbc.update("UPDATE subscriptions SET next_billing_date = ? WHERE id = ? AND status = 'active'",
                Date.valueOf(nextDate), subscriptionId);
    }

    public void incrementDiscountUsage(UUID discountId) {
        jdbc.update("UPDATE discounts SET used_count = used_count + 1 WHERE id = ?", discountId);
    }

    public Optional<UUID> findInvoiceIdForPeriod(UUID subscriptionId, LocalDate start, LocalDate end) {
        List<UUID> ids = jdbc.query(
                "SELECT id FROM invoices WHERE subscription_id = ? AND billing_period_start = ? AND billing_period_end = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                subscriptionId, Date.valueOf(start), Date.valueOf(end));
        return ids.stream().findFirst();
    }

    public Optional<InvoiceHeader> findInvoiceForUpdate(UUID invoiceId) {
        List<InvoiceHeader> rows = jdbc.query(
                "SELECT id, customer_id, total_amount, status::text AS status, due_date " +
                "FROM invoices WHERE id = ? FOR UPDATE",
                (rs, rowNum) -> new InvoiceHeader(
                        rs.getObject("id", UUID.class),
                        rs.getObject("customer_id", UUID.class),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"),
                        rs.getDate("due_date").toLocalDate()),
                invoiceId);
        return rows.stream().findFirst();
    }

    public void updateInvoiceStatus(UUID invoiceId, String status) {
        jdbc.update("UPDATE invoices SET status = CAST(? AS invoice_status) WHERE id = ?", status, invoiceId);
    }

    public List<com.brightloop.billing.dto.ApiModels.InvoiceSummary> findInvoices(UUID customerId) {
        String sql = invoiceSelect() + " WHERE (?::uuid IS NULL OR i.customer_id = ?) ORDER BY i.issue_date DESC, i.created_at DESC";
        return jdbc.query(sql, invoiceRowMapper(), customerId, customerId);
    }

    public List<com.brightloop.billing.dto.ApiModels.InvoiceSummary> findOverdueInvoices() {
        String sql = invoiceSelect() + " WHERE i.status <> 'cancelled' AND i.status <> 'draft' AND i.status <> 'paid' AND i.due_date < CURRENT_DATE " +
                "AND i.total_amount > COALESCE((SELECT SUM(CASE " +
                "WHEN COALESCE(p.reference_number, '') LIKE 'SIM-REFUND-%' THEN -p.amount " +
                "WHEN COALESCE(p.reference_number, '') LIKE 'SIM-FAILED-%' " +
                "OR COALESCE(p.reference_number, '') LIKE 'SIM-PENDING-%' THEN 0 " +
                "ELSE p.amount END) FROM payments p WHERE p.invoice_id = i.id), 0) " +
                "ORDER BY i.due_date ASC";
        return jdbc.query(sql, invoiceRowMapper());
    }

    public Optional<com.brightloop.billing.dto.ApiModels.InvoiceSummary> findInvoice(UUID invoiceId) {
        List<com.brightloop.billing.dto.ApiModels.InvoiceSummary> rows = jdbc.query(
                invoiceSelect() + " WHERE i.id = ?", invoiceRowMapper(), invoiceId);
        return rows.stream().findFirst();
    }

    public Optional<UUID> findAnyCustomerId() {
        List<UUID> rows = jdbc.query("SELECT id FROM users LIMIT 1", (rs, rowNum) -> rs.getObject("id", UUID.class));
        return rows.stream().findFirst();
    }

    public Optional<UUID> findAnySubscriptionId() {
        List<UUID> rows = jdbc.query("SELECT id FROM subscriptions LIMIT 1", (rs, rowNum) -> rs.getObject("id", UUID.class));
        return rows.stream().findFirst();
    }

    public List<com.brightloop.billing.dto.ApiModels.InvoiceItemView> findInvoiceItems(UUID invoiceId) {
        return jdbc.query(
                "SELECT id, product_id, variant_id, description, quantity, unit_price, tax_rate, tax_amount, subtotal " +
                "FROM invoice_items WHERE invoice_id = ? ORDER BY id",
                (rs, rowNum) -> new com.brightloop.billing.dto.ApiModels.InvoiceItemView(
                        rs.getObject("id", UUID.class),
                        rs.getObject("product_id", UUID.class),
                        rs.getObject("variant_id", UUID.class),
                        rs.getString("description"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getBigDecimal("tax_rate"),
                        rs.getBigDecimal("tax_amount"),
                        rs.getBigDecimal("subtotal")),
                invoiceId);
    }

    public void cancelInvoice(UUID invoiceId) {
        int updated = jdbc.update("UPDATE invoices SET status = 'cancelled' WHERE id = ? AND status <> 'paid'", invoiceId);
        if (updated == 0) {
            throw new NotFoundException("Invoice was not found or cannot be cancelled.");
        }
    }

    private String invoiceSelect() {
        return "SELECT i.id, i.invoice_number, i.subscription_id, i.customer_id, " +
                "i.billing_period_start, i.billing_period_end, i.issue_date, i.due_date, " +
                "i.subtotal, i.tax_amount, i.discount_amount, i.total_amount, i.status::text AS database_status, " +
                "COALESCE((SELECT SUM(CASE " +
                "WHEN COALESCE(p.reference_number, '') LIKE 'SIM-REFUND-%' THEN -p.amount " +
                "WHEN COALESCE(p.reference_number, '') LIKE 'SIM-FAILED-%' " +
                "OR COALESCE(p.reference_number, '') LIKE 'SIM-PENDING-%' THEN 0 " +
                "ELSE p.amount END) FROM payments p WHERE p.invoice_id = i.id), 0) AS paid_amount " +
                "FROM invoices i";
    }

    private org.springframework.jdbc.core.RowMapper<com.brightloop.billing.dto.ApiModels.InvoiceSummary> invoiceRowMapper() {
        return (rs, rowNum) -> {
            java.math.BigDecimal total = rs.getBigDecimal("total_amount");
            java.math.BigDecimal paid = rs.getBigDecimal("paid_amount");
            java.math.BigDecimal balance = total.subtract(paid).max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);
            String dbStatus = rs.getString("database_status");
            java.time.LocalDate due = rs.getDate("due_date").toLocalDate();
            String paymentStatus;
            if ("cancelled".equals(dbStatus)) paymentStatus = "CANCELLED";
            else if (balance.compareTo(BigDecimal.ZERO) == 0) paymentStatus = "PAID";
            else if (due.isBefore(LocalDate.now())) paymentStatus = "OVERDUE";
            else if (paid.compareTo(BigDecimal.ZERO) > 0) paymentStatus = "PARTIALLY_PAID";
            else if ("draft".equals(dbStatus)) paymentStatus = "DRAFT";
            else paymentStatus = "CONFIRMED";

            return new com.brightloop.billing.dto.ApiModels.InvoiceSummary(
                    rs.getObject("id", UUID.class), rs.getString("invoice_number"),
                    rs.getObject("subscription_id", UUID.class), rs.getObject("customer_id", UUID.class),
                    rs.getDate("billing_period_start").toLocalDate(), rs.getDate("billing_period_end").toLocalDate(),
                    rs.getDate("issue_date").toLocalDate(), due, rs.getBigDecimal("subtotal"),
                    rs.getBigDecimal("tax_amount"), rs.getBigDecimal("discount_amount"), total,
                    dbStatus, paid, balance, paymentStatus);
        };
    }
}
