package com.brightloop.quotation.dao;

import com.brightloop.quotation.model.Quotation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class QuotationDAO {

    private final JdbcTemplate jdbcTemplate;

    public QuotationDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Quotation> quotationRowMapper = (rs, rowNum) -> {
        Quotation quotation = new Quotation();

        quotation.setId(rs.getObject("id", UUID.class));
        quotation.setQuotationNumber(rs.getString("quotation_number"));
        quotation.setCustomerId(rs.getObject("customer_id", UUID.class));
        quotation.setTemplateId(rs.getObject("template_id", UUID.class));
        quotation.setPlanId(rs.getObject("plan_id", UUID.class));
        quotation.setDiscountId(rs.getObject("discount_id", UUID.class));
        quotation.setStatus(rs.getString("status"));

        Date issueDate = rs.getDate("issue_date");
        quotation.setIssueDate(issueDate != null ? issueDate.toLocalDate() : null);

        Date validUntil = rs.getDate("valid_until");
        quotation.setValidUntil(validUntil != null ? validUntil.toLocalDate() : null);

        quotation.setSubtotal(rs.getBigDecimal("subtotal"));
        quotation.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        quotation.setTaxAmount(rs.getBigDecimal("tax_amount"));
        quotation.setTotalAmount(rs.getBigDecimal("total_amount"));
        quotation.setPaymentTerms(rs.getString("payment_terms"));
        quotation.setNotes(rs.getString("notes"));
        quotation.setCreatedBy(rs.getObject("created_by", UUID.class));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            quotation.setCreatedAt(createdAt.toInstant().atOffset(java.time.ZoneOffset.UTC));
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            quotation.setUpdatedAt(updatedAt.toInstant().atOffset(java.time.ZoneOffset.UTC));
        }

        return quotation;
    };

    public Quotation save(Quotation quotation) {
        String sql = """
                INSERT INTO quotations (
                    quotation_number,
                    customer_id,
                    template_id,
                    plan_id,
                    discount_id,
                    status,
                    issue_date,
                    valid_until,
                    subtotal,
                    discount_amount,
                    tax_amount,
                    total_amount,
                    payment_terms,
                    notes,
                    created_by
                )
                VALUES (?, ?, ?, ?, ?, ?::quotation_status, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        UUID id = jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                quotation.getQuotationNumber(),
                quotation.getCustomerId(),
                quotation.getTemplateId(),
                quotation.getPlanId(),
                quotation.getDiscountId(),
                quotation.getStatus(),
                quotation.getIssueDate(),
                quotation.getValidUntil(),
                quotation.getSubtotal(),
                quotation.getDiscountAmount(),
                quotation.getTaxAmount(),
                quotation.getTotalAmount(),
                quotation.getPaymentTerms(),
                quotation.getNotes(),
                quotation.getCreatedBy()
        );

        quotation.setId(id);

        return findById(id)
                .orElseThrow(() -> new IllegalStateException("Quotation could not be retrieved after saving."));
    }

    public List<Quotation> findAll() {
        String sql = """
                SELECT *
                FROM quotations
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, quotationRowMapper);
    }

    public Optional<Quotation> findById(UUID id) {
        String sql = """
                SELECT *
                FROM quotations
                WHERE id = ?
                """;

        List<Quotation> results = jdbcTemplate.query(sql, quotationRowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<Quotation> findByQuotationNumber(String quotationNumber) {
        String sql = """
                SELECT *
                FROM quotations
                WHERE quotation_number = ?
                """;

        List<Quotation> results = jdbcTemplate.query(sql, quotationRowMapper, quotationNumber);
        return results.stream().findFirst();
    }

    public List<Quotation> findByCustomerId(UUID customerId) {
        String sql = """
                SELECT *
                FROM quotations
                WHERE customer_id = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, quotationRowMapper, customerId);
    }

    public List<Quotation> findByStatus(String status) {
        String sql = """
                SELECT *
                FROM quotations
                WHERE status = ?::quotation_status
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, quotationRowMapper, status);
    }

    public boolean existsByQuotationNumber(String quotationNumber) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM quotations
                    WHERE quotation_number = ?
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, quotationNumber);
        return Boolean.TRUE.equals(exists);
    }

    public void updateStatus(UUID id, String status) {
        String sql = """
                UPDATE quotations
                SET status = ?::quotation_status,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        jdbcTemplate.update(sql, status, id);
    }

    public void delete(UUID id) {
        String sql = """
                DELETE FROM quotations
                WHERE id = ?
                """;

        jdbcTemplate.update(sql, id);
    }
}
