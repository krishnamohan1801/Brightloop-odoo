package com.brightloop.sms.dao;

import com.brightloop.sms.model.QuotationTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class QuotationTemplateDao {

    private final JdbcTemplate jdbcTemplate;

    public QuotationTemplateDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<QuotationTemplate> templateRowMapper = (ResultSet rs, int rowNum) -> {
        QuotationTemplate qt = new QuotationTemplate();
        qt.setId(rs.getObject("id", UUID.class));
        qt.setName(rs.getString("name"));
        qt.setDefaultPlanId(rs.getObject("default_plan_id", UUID.class));
        qt.setValidityDays(rs.getInt("validity_days"));
        qt.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return qt;
    };

    public List<QuotationTemplate> findAll() {
        String sql = "SELECT * FROM quotation_templates ORDER BY name ASC";
        return jdbcTemplate.query(sql, templateRowMapper);
    }

    public Optional<QuotationTemplate> findById(UUID id) {
        String sql = "SELECT * FROM quotation_templates WHERE id = ?";
        List<QuotationTemplate> list = jdbcTemplate.query(sql, templateRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public QuotationTemplate create(QuotationTemplate template) {
        UUID id = template.getId() != null ? template.getId() : UUID.randomUUID();
        String sql = "INSERT INTO quotation_templates (id, name, default_plan_id, validity_days) VALUES (?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
            sql,
            templateRowMapper,
            id,
            template.getName(),
            template.getDefaultPlanId(),
            template.getValidityDays() != null ? template.getValidityDays() : 30
        );
    }

    public void update(QuotationTemplate template) {
        String sql = "UPDATE quotation_templates SET name = ?, default_plan_id = ?, validity_days = ? WHERE id = ?";
        jdbcTemplate.update(sql, template.getName(), template.getDefaultPlanId(), template.getValidityDays(), template.getId());
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM quotation_templates WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
