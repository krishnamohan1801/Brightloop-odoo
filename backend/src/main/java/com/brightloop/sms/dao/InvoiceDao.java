package com.brightloop.sms.dao;

import com.brightloop.sms.model.Invoice;
import com.brightloop.sms.model.InvoiceItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InvoiceDao {

    private final JdbcTemplate jdbcTemplate;

    public InvoiceDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Invoice> invoiceRowMapper = (ResultSet rs, int rowNum) -> {
        Invoice inv = new Invoice();
        inv.setId(rs.getObject("id", UUID.class));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        inv.setSubscriptionId(rs.getObject("subscription_id", UUID.class));
        inv.setCustomerId(rs.getObject("customer_id", UUID.class));
        inv.setBillingPeriodStart(rs.getObject("billing_period_start", LocalDate.class));
        inv.setBillingPeriodEnd(rs.getObject("billing_period_end", LocalDate.class));
        inv.setIssueDate(rs.getObject("issue_date", LocalDate.class));
        inv.setDueDate(rs.getObject("due_date", LocalDate.class));
        inv.setSubtotal(rs.getBigDecimal("subtotal"));
        inv.setTaxAmount(rs.getBigDecimal("tax_amount"));
        inv.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        inv.setTotalAmount(rs.getBigDecimal("total_amount"));
        inv.setStatus(rs.getString("status"));
        inv.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        inv.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

        try { inv.setCustomerName(rs.getString("customer_name")); } catch (Exception ignored) {}
        try { inv.setCustomerEmail(rs.getString("customer_email")); } catch (Exception ignored) {}
        try { inv.setSubscriptionNumber(rs.getString("subscription_number")); } catch (Exception ignored) {}

        return inv;
    };

    private final RowMapper<InvoiceItem> itemRowMapper = (ResultSet rs, int rowNum) -> {
        InvoiceItem item = new InvoiceItem();
        item.setId(rs.getObject("id", UUID.class));
        item.setInvoiceId(rs.getObject("invoice_id", UUID.class));
        item.setProductId(rs.getObject("product_id", UUID.class));
        item.setVariantId(rs.getObject("variant_id", UUID.class));
        item.setDescription(rs.getString("description"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setTaxRate(rs.getBigDecimal("tax_rate"));
        item.setTaxAmount(rs.getBigDecimal("tax_amount"));
        item.setSubtotal(rs.getBigDecimal("subtotal"));

        try { item.setProductName(rs.getString("product_name")); } catch (Exception ignored) {}
        try { item.setVariantName(rs.getString("variant_name")); } catch (Exception ignored) {}

        return item;
    };

    private static final String BASE_SELECT =
        "SELECT inv.*, u.full_name AS customer_name, u.email AS customer_email, s.subscription_number " +
        "FROM invoices inv " +
        "JOIN users u ON inv.customer_id = u.id " +
        "JOIN subscriptions s ON inv.subscription_id = s.id ";

    public List<Invoice> findAll(String status, UUID customerId, Boolean overdueOnly) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND inv.status = ?::invoice_status ");
            params.add(status);
        }
        if (customerId != null) {
            sql.append(" AND inv.customer_id = ? ");
            params.add(customerId);
        }
        if (overdueOnly != null && overdueOnly) {
            sql.append(" AND inv.status != 'paid' AND inv.status != 'cancelled' AND inv.due_date < CURRENT_DATE ");
        }
        sql.append(" ORDER BY inv.issue_date DESC, inv.created_at DESC");

        List<Invoice> list = jdbcTemplate.query(sql.toString(), invoiceRowMapper, params.toArray());
        for (Invoice inv : list) {
            enrichInvoice(inv);
        }
        return list;
    }

    public Optional<Invoice> findById(UUID id) {
        String sql = BASE_SELECT + " WHERE inv.id = ?";
        List<Invoice> list = jdbcTemplate.query(sql, invoiceRowMapper, id);
        if (list.isEmpty()) return Optional.empty();
        Invoice inv = list.get(0);
        enrichInvoice(inv);
        return Optional.of(inv);
    }

    public boolean existsByBillingCycle(UUID subscriptionId, LocalDate start, LocalDate end) {
        String sql = "SELECT COUNT(*) FROM invoices WHERE subscription_id = ? AND billing_period_start = ? AND billing_period_end = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, subscriptionId, start, end);
        return count != null && count > 0;
    }

    @Transactional
    public Invoice create(Invoice invoice, List<InvoiceItem> items) {
        UUID id = invoice.getId() != null ? invoice.getId() : UUID.randomUUID();
        invoice.setId(id);

        String sql = "INSERT INTO invoices (id, invoice_number, subscription_id, customer_id, " +
                     "billing_period_start, billing_period_end, issue_date, due_date, " +
                     "subtotal, tax_amount, discount_amount, total_amount, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::invoice_status) RETURNING *";

        jdbcTemplate.queryForObject(
            sql,
            invoiceRowMapper,
            id,
            invoice.getInvoiceNumber(),
            invoice.getSubscriptionId(),
            invoice.getCustomerId(),
            invoice.getBillingPeriodStart(),
            invoice.getBillingPeriodEnd(),
            invoice.getIssueDate() != null ? invoice.getIssueDate() : LocalDate.now(),
            invoice.getDueDate(),
            invoice.getSubtotal(),
            invoice.getTaxAmount(),
            invoice.getDiscountAmount(),
            invoice.getTotalAmount(),
            invoice.getStatus() != null ? invoice.getStatus() : "draft"
        );

        if (items != null) {
            for (InvoiceItem item : items) {
                UUID itemId = UUID.randomUUID();
                item.setId(itemId);
                item.setInvoiceId(id);
                String itemSql = "INSERT INTO invoice_items (id, invoice_id, product_id, variant_id, description, " +
                                 "quantity, unit_price, tax_rate, tax_amount, subtotal) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(
                    itemSql,
                    itemId,
                    id,
                    item.getProductId(),
                    item.getVariantId(),
                    item.getDescription(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTaxRate(),
                    item.getTaxAmount(),
                    item.getSubtotal()
                );
            }
        }

        return findById(id).orElse(invoice);
    }

    public void updateStatus(UUID id, String status) {
        String sql = "UPDATE invoices SET status = ?::invoice_status WHERE id = ?";
        jdbcTemplate.update(sql, status, id);
    }

    public List<InvoiceItem> findItemsByInvoiceId(UUID invoiceId) {
        String sql = "SELECT ii.*, pr.name AS product_name, pv.variant_name " +
                     "FROM invoice_items ii " +
                     "JOIN products pr ON ii.product_id = pr.id " +
                     "LEFT JOIN product_variants pv ON ii.variant_id = pv.id " +
                     "WHERE ii.invoice_id = ?";
        return jdbcTemplate.query(sql, itemRowMapper, invoiceId);
    }

    public BigDecimal getAmountPaid(UUID invoiceId) {
        String sql = "SELECT COALESCE(SUM(amount), 0.00) FROM payments WHERE invoice_id = ?";
        BigDecimal paid = jdbcTemplate.queryForObject(sql, BigDecimal.class, invoiceId);
        return paid != null ? paid : BigDecimal.ZERO;
    }

    private void enrichInvoice(Invoice inv) {
        inv.setItems(findItemsByInvoiceId(inv.getId()));
        BigDecimal paid = getAmountPaid(inv.getId());
        inv.setAmountPaid(paid);
        BigDecimal balance = inv.getTotalAmount().subtract(paid);
        inv.setBalanceDue(balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balance);
        inv.setIsOverdue(
            !"paid".equalsIgnoreCase(inv.getStatus()) &&
            !"cancelled".equalsIgnoreCase(inv.getStatus()) &&
            inv.getDueDate() != null &&
            inv.getDueDate().isBefore(LocalDate.now())
        );
    }
}
