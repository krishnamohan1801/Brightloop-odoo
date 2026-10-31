package com.brightloop.sms.dao;

import com.brightloop.sms.model.Tax;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TaxDao {

    private final JdbcTemplate jdbcTemplate;

    public TaxDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Tax> taxRowMapper = (ResultSet rs, int rowNum) -> {
        Tax t = new Tax();
        t.setId(rs.getObject("id", UUID.class));
        t.setName(rs.getString("name"));
        t.setRate(rs.getBigDecimal("rate"));
        t.setIsActive(rs.getBoolean("is_active"));
        t.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return t;
    };

    public List<Tax> findAll(Boolean activeOnly) {
        String sql = activeOnly != null && activeOnly
            ? "SELECT * FROM taxes WHERE is_active = true ORDER BY name ASC"
            : "SELECT * FROM taxes ORDER BY name ASC";
        return jdbcTemplate.query(sql, taxRowMapper);
    }

    public Optional<Tax> findById(UUID id) {
        String sql = "SELECT * FROM taxes WHERE id = ?";
        List<Tax> list = jdbcTemplate.query(sql, taxRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Tax create(Tax tax) {
        UUID id = tax.getId() != null ? tax.getId() : UUID.randomUUID();
        String sql = "INSERT INTO taxes (id, name, rate, is_active) VALUES (?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
            sql,
            taxRowMapper,
            id,
            tax.getName(),
            tax.getRate(),
            tax.getIsActive() != null ? tax.getIsActive() : true
        );
    }

    public void update(Tax tax) {
        String sql = "UPDATE taxes SET name = ?, rate = ?, is_active = ? WHERE id = ?";
        jdbcTemplate.update(sql, tax.getName(), tax.getRate(), tax.getIsActive(), tax.getId());
    }

    public void setArchive(UUID id, boolean isActive) {
        String sql = "UPDATE taxes SET is_active = ? WHERE id = ?";
        jdbcTemplate.update(sql, isActive, id);
    }
}
