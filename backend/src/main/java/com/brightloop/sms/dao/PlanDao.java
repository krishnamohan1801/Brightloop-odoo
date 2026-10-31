package com.brightloop.sms.dao;

import com.brightloop.sms.model.Plan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlanDao {

    private final JdbcTemplate jdbcTemplate;

    public PlanDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Plan> planRowMapper = (ResultSet rs, int rowNum) -> {
        Plan p = new Plan();
        p.setId(rs.getObject("id", UUID.class));
        p.setName(rs.getString("name"));
        p.setBillingInterval(rs.getString("billing_interval"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setMinQuantity(rs.getInt("min_quantity"));
        p.setStartDate(rs.getObject("start_date", LocalDate.class));
        p.setEndDate(rs.getObject("end_date", LocalDate.class));
        p.setAutoClose(rs.getBoolean("auto_close"));
        p.setCanPause(rs.getBoolean("can_pause"));
        p.setIsActive(rs.getBoolean("is_active"));
        p.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        p.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        return p;
    };

    public List<Plan> findAll(Boolean activeOnly) {
        String sql = activeOnly != null && activeOnly
            ? "SELECT * FROM plans WHERE is_active = true ORDER BY name ASC"
            : "SELECT * FROM plans ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, planRowMapper);
    }

    public Optional<Plan> findById(UUID id) {
        String sql = "SELECT * FROM plans WHERE id = ?";
        List<Plan> list = jdbcTemplate.query(sql, planRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Plan create(Plan plan) {
        UUID id = plan.getId() != null ? plan.getId() : UUID.randomUUID();
        String sql = "INSERT INTO plans (id, name, billing_interval, price, min_quantity, start_date, end_date, auto_close, can_pause, is_active) " +
                     "VALUES (?, ?, ?::billing_interval, ?, ?, ?, ?, ?, ?, ?) RETURNING *";
        return jdbcTemplate.queryForObject(
            sql,
            planRowMapper,
            id,
            plan.getName(),
            plan.getBillingInterval(),
            plan.getPrice(),
            plan.getMinQuantity() != null ? plan.getMinQuantity() : 1,
            plan.getStartDate(),
            plan.getEndDate(),
            plan.getAutoClose() != null ? plan.getAutoClose() : false,
            plan.getCanPause() != null ? plan.getCanPause() : true,
            plan.getIsActive() != null ? plan.getIsActive() : true
        );
    }

    public void update(Plan plan) {
        String sql = "UPDATE plans SET name = ?, billing_interval = ?::billing_interval, price = ?, " +
                     "min_quantity = ?, start_date = ?, end_date = ?, auto_close = ?, can_pause = ?, is_active = ? " +
                     "WHERE id = ?";
        jdbcTemplate.update(
            sql,
            plan.getName(),
            plan.getBillingInterval(),
            plan.getPrice(),
            plan.getMinQuantity(),
            plan.getStartDate(),
            plan.getEndDate(),
            plan.getAutoClose(),
            plan.getCanPause(),
            plan.getIsActive(),
            plan.getId()
        );
    }

    public void setArchive(UUID id, boolean isActive) {
        String sql = "UPDATE plans SET is_active = ? WHERE id = ?";
        jdbcTemplate.update(sql, isActive, id);
    }
}
