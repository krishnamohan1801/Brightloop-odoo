package com.brightloop.dao;

import com.brightloop.model.Plan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PlanDAO {

    private final JdbcTemplate jdbcTemplate;

    public PlanDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Plan> rowMapper = (rs, rowNum) -> {

        Plan plan = new Plan();

        plan.setId(rs.getObject("id", UUID.class));
        plan.setName(rs.getString("name"));
        plan.setBillingInterval(rs.getString("billing_interval"));
        plan.setPrice(rs.getBigDecimal("price"));
        plan.setMinQuantity(rs.getInt("min_quantity"));
        plan.setStartDate(rs.getDate("start_date").toLocalDate());

        java.sql.Date endDate = rs.getDate("end_date");

        plan.setEndDate(
            endDate != null ? endDate.toLocalDate() : null
        );

        plan.setAutoClose(rs.getBoolean("auto_close"));
        plan.setCanPause(rs.getBoolean("can_pause"));
        plan.setActive(rs.getBoolean("is_active"));

        plan.setCreatedAt(
            rs.getObject("created_at", java.time.OffsetDateTime.class)
        );

        plan.setUpdatedAt(
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );

        return plan;
    };

    // CREATE PLAN
    public UUID create(Plan plan) {

        String sql = """
                INSERT INTO plans
                (
                    name,
                    billing_interval,
                    price,
                    min_quantity,
                    start_date,
                    end_date,
                    auto_close,
                    can_pause
                )
                VALUES (?, CAST(? AS billing_interval), ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                UUID.class,
                plan.getName(),
                plan.getBillingInterval(),
                plan.getPrice(),
                plan.getMinQuantity(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.isAutoClose(),
                plan.isCanPause()
        );
    }

    // GET ALL ACTIVE PLANS
    public List<Plan> findAll() {

        String sql = """
                SELECT *
                FROM plans
                WHERE is_active = TRUE
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, rowMapper);
    }

    // GET PLAN BY ID
    public Plan findById(UUID id) {

        String sql = """
                SELECT *
                FROM plans
                WHERE id = ? AND is_active = TRUE
                """;

        List<Plan> results =
                jdbcTemplate.query(sql, rowMapper, id);

        return results.isEmpty() ? null : results.get(0);
    }

    // UPDATE PLAN
    public boolean update(UUID id, Plan plan) {

        String sql = """
                UPDATE plans
                SET
                    name = ?,
                    billing_interval = CAST(? AS billing_interval),
                    price = ?,
                    min_quantity = ?,
                    start_date = ?,
                    end_date = ?,
                    auto_close = ?,
                    can_pause = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_active = TRUE
                """;

        return jdbcTemplate.update(
                sql,
                plan.getName(),
                plan.getBillingInterval(),
                plan.getPrice(),
                plan.getMinQuantity(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.isAutoClose(),
                plan.isCanPause(),
                id
        ) > 0;
    }

    // ARCHIVE PLAN
    public boolean archive(UUID id) {

        String sql = """
                UPDATE plans
                SET
                    is_active = FALSE,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND is_active = TRUE
                """;

        return jdbcTemplate.update(sql, id) > 0;
    }
}